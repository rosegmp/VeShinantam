(() => {
  const STATE_KEY = 'veshinantam.web.v1';
  const DATABASE_NAME = 'veshinantam';
  const APP_STATE = 'app_state';
  const OUTBOX = 'sync_outbox';
  const METADATA = 'sync_metadata';
  let database = null;
  let cachedState = null;
  let suppressDiff = false;
  let pendingDiff = Promise.resolve();

  const requestResult = request => new Promise((resolve, reject) => {
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error || new Error('IndexedDB request failed'));
  });

  const transactionDone = transaction => new Promise((resolve, reject) => {
    transaction.oncomplete = () => resolve();
    transaction.onerror = () => reject(transaction.error || new Error('IndexedDB transaction failed'));
    transaction.onabort = () => reject(transaction.error || new Error('IndexedDB transaction aborted'));
  });

  const newMutationId = () => globalThis.crypto?.randomUUID?.() ||
    `web-${Date.now()}-${Math.random().toString(16).slice(2)}`;

  const materialType = value => {
    switch (String(value || '').toLowerCase()) {
      case 'gemara': case 'daf': return 'DAF';
      case 'amud': return 'AMUD';
      case 'mishnah': case 'mishnayos': return 'MISHNAH';
      case 'perek': case 'perakim': return 'PEREK';
      case 'page': return 'PAGE';
      case 'seif': return 'SEIF';
      case 'siman': case 'kitzur': return 'SIMAN';
      default: return 'CUSTOM_UNIT';
    }
  };

  const schedulePayload = (schedule, state, now) => {
    const tasks = (state.tasks || []).filter(task => task.scheduleId === schedule.id);
    const learningDates = tasks.filter(task => task.type === 'LEARNING').map(task => task.dueDate).sort();
    const allDates = tasks.map(task => task.dueDate).sort();
    return {
      id: schedule.id,
      nameEnglish: schedule.name,
      nameHebrew: schedule.nameHebrew || '',
      kind: schedule.presetId ? 'PRESET' : 'CUSTOM',
      sourceType: schedule.sourceType || schedule.material || 'CUSTOM',
      materialType: materialType(schedule.materialType || schedule.material),
      presetId: schedule.presetId || undefined,
      startDate: schedule.startDate || allDates[0] || now.slice(0, 10),
      targetDate: schedule.targetDate || learningDates.at(-1) || undefined,
      dailyQuantity: Number(schedule.pace ?? 1),
      selectedWeekdays: Array.from(schedule.weekdays || [0, 1, 2, 3, 4, 5, 6]),
      chazarahDayOffsets: Array.from(schedule.chazarahOffsets || []),
      repeatsAnnually: Boolean(schedule.repeatsAnnually),
      officialOraysaChazarah: Boolean(schedule.officialOraysaChazarah),
      missedWorkBehavior: schedule.missedWorkBehavior || 'KEEP_FIXED_OVERDUE',
      state: schedule.archived ? 'ARCHIVED' : schedule.active === false ? 'PAUSED' : 'ACTIVE',
      generationRevision: Number(schedule.generationRevision || 1),
      createdAt: schedule.createdAt || now,
      updatedAt: now,
      revision: Number(schedule.revision || 0),
      deleted: false
    };
  };

  const taskPayload = (task, now) => ({
    id: task.id,
    stableKey: task.stableKey || task.id,
    scheduleId: task.scheduleId,
    type: task.type === 'CHAZARAH' ? 'CHAZARAH' : 'LEARNING',
    labelEnglish: task.referenceEnglish || '',
    labelHebrew: task.referenceHebrew || '',
    materialType: materialType(task.materialType),
    quantity: Number(task.quantity || 1),
    plannedDate: task.dueDate,
    originalLearningDate: task.originalLearningDate || task.dueDate,
    reviewIdentity: task.reviewIdentity || undefined,
    generationRevision: Number(task.generationRevision || 1),
    completedAt: task.completedAt || undefined,
    completionLocalDate: task.completionLocalDate || undefined,
    completionZoneId: task.completionZoneId || undefined,
    updatedAt: now,
    revision: Number(task.revision || 0),
    deleted: false
  });

  const exclusionPayload = (exclusion, now) => ({
    scheduleId: exclusion.scheduleId,
    date: exclusion.date,
    updatedAt: now,
    revision: Number(exclusion.revision || 0),
    deleted: false
  });

  const goalPayload = (goal, now) => ({
    id: goal.id || goal.kind,
    kind: goal.kind,
    target: Number(goal.target),
    completed: false,
    updatedAt: now,
    revision: Number(goal.revision || 0),
    deleted: false
  });

  const preferencesPayload = (state, now) => ({
    appLanguage: state.language === 'he' ? 'he' : 'en',
    sefarimLanguage: state.sefarimLanguage || 'BOTH',
    primaryCalendar: state.primaryCalendar || 'GREGORIAN',
    defaultChazarahOffsets: state.defaultChazarahOffsets || [1, 7, 30, 90],
    reminderEnabled: false,
    reminderHour: 20,
    reminderMinute: 0,
    todaySortOrder: state.todaySortOrder || 'SCHEDULED_FIRST',
    automaticPresetUpdates: false,
    updatedAt: now,
    revision: Number(state.preferencesRevision || 0)
  });

  const comparable = value => {
    if (!value) return null;
    const copy = { ...value };
    delete copy.revision;
    delete copy.updatedAt;
    return JSON.stringify(copy);
  };

  const queueMutation = async mutation => {
    if (!database) return;
    const transaction = database.transaction(OUTBOX, 'readwrite');
    transaction.objectStore(OUTBOX).put(mutation, `${mutation.entityType}:${mutation.entityId}`);
    await transactionDone(transaction);
  };

  const queueStateDiff = async (previous, next) => {
    if (!database || suppressDiff) return;
    const now = new Date().toISOString();
    const compareCollection = async (type, before, after, convert) => {
      const beforeMap = new Map((before || []).map(value => [value.id, value]));
      const afterMap = new Map((after || []).map(value => [value.id, value]));
      for (const [id, value] of afterMap) {
        const old = beforeMap.get(id);
        const payload = convert(value, next, now);
        const oldPayload = old ? convert(old, previous, now) : null;
        if (!old || comparable(payload) !== comparable(oldPayload)) {
          await queueMutation({
            mutationId: newMutationId(), entityType: type, entityId: id,
            baseRevision: Number(value.revision || 0), payload, deleted: false, createdAt: now
          });
        }
      }
      for (const [id, value] of beforeMap) {
        if (!afterMap.has(id)) await queueMutation({
          mutationId: newMutationId(), entityType: type, entityId: id,
          baseRevision: Number(value.revision || 0), payload: null, deleted: true, createdAt: now
        });
      }
    };
    await compareCollection('SCHEDULE', previous?.schedules, next?.schedules, schedulePayload);
    await compareCollection('TASK', previous?.tasks, next?.tasks, (task, _state, timestamp) => taskPayload(task, timestamp));
    await compareCollection('EXCLUSION', previous?.exclusions, next?.exclusions, (exclusion, _state, timestamp) => exclusionPayload(exclusion, timestamp));
    await compareCollection('GOAL', previous?.goals, next?.goals, (goal, _state, timestamp) => goalPayload(goal, timestamp));
    if (!previous || previous.language !== next.language) {
      const payload = preferencesPayload(next, now);
      await queueMutation({
        mutationId: newMutationId(), entityType: 'PREFERENCES', entityId: 'preferences',
        baseRevision: Number(next.preferencesRevision || 0), payload, deleted: false, createdAt: now
      });
    }
  };

  const writeIndexedState = value => {
    if (!database) return Promise.resolve();
    const transaction = database.transaction(APP_STATE, 'readwrite');
    transaction.objectStore(APP_STATE).put(value, 'state');
    return transactionDone(transaction);
  };

  const readBrowserState = () => cachedState ?? localStorage.getItem(STATE_KEY);
  window.veshinantamReadState = readBrowserState;

  window.veshinantamPersistState = value => {
    const previousRaw = readBrowserState();
    cachedState = value;
    if (database) {
      writeIndexedState(value).then(() => localStorage.removeItem(STATE_KEY)).catch(() => {});
    } else {
      localStorage.setItem(STATE_KEY, value);
    }
    try {
      pendingDiff = pendingDiff.then(() => queueStateDiff(previousRaw ? JSON.parse(previousRaw) : null, JSON.parse(value))).catch(() => {});
    } catch (_) { /* Kotlin validates the state before it reaches persistence. */ }
  };

  window.veshinantamReadOutbox = async () => {
    await window.veshinantamStorageReady;
    await pendingDiff;
    if (!database) return [];
    return requestResult(database.transaction(OUTBOX, 'readonly').objectStore(OUTBOX).getAll());
  };

  window.veshinantamRemoveOutbox = async (entityType, entityId) => {
    if (!database) return;
    const transaction = database.transaction(OUTBOX, 'readwrite');
    transaction.objectStore(OUTBOX).delete(`${entityType}:${entityId}`);
    await transactionDone(transaction);
  };

  window.veshinantamGetSyncCursor = async () => {
    await window.veshinantamStorageReady;
    await pendingDiff;
    if (!database) return 0;
    return Number(await requestResult(database.transaction(METADATA, 'readonly').objectStore(METADATA).get('cursor')) || 0);
  };

  window.veshinantamSetSyncCursor = async cursor => {
    if (!database) return;
    const transaction = database.transaction(METADATA, 'readwrite');
    transaction.objectStore(METADATA).put(Number(cursor), 'cursor');
    await transactionDone(transaction);
  };

  window.veshinantamEnsureEntitySeed = async state => {
    await window.veshinantamStorageReady;
    await pendingDiff;
    if (!database) return;
    const transaction = database.transaction(METADATA, 'readonly');
    const seeded = await requestResult(transaction.objectStore(METADATA).get('seeded'));
    if (seeded) return;
    await queueStateDiff(null, state);
    const write = database.transaction(METADATA, 'readwrite');
    write.objectStore(METADATA).put(true, 'seeded');
    await transactionDone(write);
  };

  const scheduleFromCanonical = value => ({
    id: value.id, name: value.nameEnglish || value.nameHebrew, material: value.sourceType,
    pace: value.dailyQuantity, weekdays: value.selectedWeekdays, chazarahOffsets: value.chazarahDayOffsets,
    active: value.state === 'ACTIVE', archived: value.state === 'ARCHIVED', nameHebrew: value.nameHebrew || '',
    presetId: value.presetId || null, startDate: value.startDate, targetDate: value.targetDate || null,
    missedWorkBehavior: value.missedWorkBehavior, repeatsAnnually: value.repeatsAnnually,
    officialOraysaChazarah: value.officialOraysaChazarah, generationRevision: value.generationRevision,
    createdAt: value.createdAt, updatedAt: value.updatedAt, revision: value.revision,
    sourceType: value.sourceType, materialType: value.materialType
  });

  const taskFromCanonical = value => ({
    id: value.id, scheduleId: value.scheduleId, referenceEnglish: value.labelEnglish,
    referenceHebrew: value.labelHebrew, dueDate: value.plannedDate, type: value.type,
    completed: Boolean(value.completedAt), stableKey: value.stableKey, materialType: value.materialType,
    quantity: value.quantity, originalLearningDate: value.originalLearningDate,
    reviewIdentity: value.reviewIdentity || null, generationRevision: value.generationRevision,
    completedAt: value.completedAt || null, completionLocalDate: value.completionLocalDate || null,
    completionZoneId: value.completionZoneId || null, updatedAt: value.updatedAt, revision: value.revision
  });

  const exclusionFromCanonical = value => ({
    id: `${value.scheduleId}:${value.date}`, scheduleId: value.scheduleId, date: value.date,
    updatedAt: value.updatedAt, revision: value.revision
  });

  const goalFromCanonical = value => ({
    id: value.id || value.kind, kind: value.kind, target: Number(value.target),
    updatedAt: value.updatedAt, revision: value.revision
  });

  window.veshinantamApplyEntityRecords = async records => {
    if (!database) throw new Error('IndexedDB is unavailable; cloud data cannot be stored safely in this browser.');
    const state = JSON.parse(readBrowserState() || '{"schedules":[],"tasks":[],"language":"en"}');
    const schedules = new Map((state.schedules || []).map(value => [value.id, value]));
    const tasks = new Map((state.tasks || []).map(value => [value.id, value]));
    const exclusions = new Map((state.exclusions || []).map(value => [value.id, value]));
    const goals = new Map((state.goals || []).map(value => [value.id, value]));
    const deletedScheduleIds = new Set();
    for (const record of records) {
      if (record.entity_type === 'SCHEDULE') {
        if (record.deleted) {
          schedules.delete(record.entity_id);
          deletedScheduleIds.add(record.entity_id);
        } else if (record.payload) {
          schedules.set(record.entity_id, scheduleFromCanonical({ ...record.payload, revision: record.revision }));
        }
      } else if (record.entity_type === 'TASK') {
        if (record.deleted) tasks.delete(record.entity_id);
        else if (record.payload) tasks.set(record.entity_id, taskFromCanonical({ ...record.payload, revision: record.revision }));
      } else if (record.entity_type === 'EXCLUSION') {
        if (record.deleted) exclusions.delete(record.entity_id);
        else if (record.payload) exclusions.set(record.entity_id, exclusionFromCanonical({ ...record.payload, revision: record.revision }));
      } else if (record.entity_type === 'GOAL') {
        if (record.deleted) goals.delete(record.entity_id);
        else if (record.payload) goals.set(record.entity_id, goalFromCanonical({ ...record.payload, revision: record.revision }));
      } else if (record.entity_type === 'PREFERENCES' && !record.deleted && record.payload) {
        state.language = record.payload.appLanguage === 'he' ? 'he' : 'en';
        state.sefarimLanguage = record.payload.sefarimLanguage || 'BOTH';
        state.primaryCalendar = record.payload.primaryCalendar || 'GREGORIAN';
        state.defaultChazarahOffsets = record.payload.defaultChazarahOffsets || [1, 7, 30, 90];
        state.todaySortOrder = record.payload.todaySortOrder || 'SCHEDULED_FIRST';
        state.preferencesRevision = record.revision;
      }
    }
    state.schedules = Array.from(schedules.values());
    state.tasks = Array.from(tasks.values()).filter(task => !deletedScheduleIds.has(task.scheduleId));
    state.exclusions = Array.from(exclusions.values()).filter(exclusion => !deletedScheduleIds.has(exclusion.scheduleId));
    state.goals = Array.from(goals.values());
    const serialized = JSON.stringify(state);
    suppressDiff = true;
    cachedState = serialized;
    await writeIndexedState(serialized);
    localStorage.removeItem(STATE_KEY);
    suppressDiff = false;
    return state;
  };

  window.veshinantamStorageReady = new Promise(resolve => {
    if (!('indexedDB' in window)) { resolve(); return; }
    const request = indexedDB.open(DATABASE_NAME, 2);
    request.onupgradeneeded = () => {
      if (!request.result.objectStoreNames.contains(APP_STATE)) request.result.createObjectStore(APP_STATE);
      if (!request.result.objectStoreNames.contains(OUTBOX)) request.result.createObjectStore(OUTBOX);
      if (!request.result.objectStoreNames.contains(METADATA)) request.result.createObjectStore(METADATA);
    };
    request.onerror = () => {
      cachedState = localStorage.getItem(STATE_KEY);
      resolve();
    };
    request.onsuccess = async () => {
      database = request.result;
      database.onversionchange = () => database.close();
      try {
        const indexedState = await requestResult(database.transaction(APP_STATE, 'readonly').objectStore(APP_STATE).get('state'));
        const legacyState = localStorage.getItem(STATE_KEY);
        if (typeof indexedState === 'string') {
          cachedState = indexedState;
          localStorage.removeItem(STATE_KEY);
        } else if (legacyState) {
          cachedState = legacyState;
          await writeIndexedState(legacyState);
          localStorage.removeItem(STATE_KEY);
        }
      } catch (_) { cachedState = localStorage.getItem(STATE_KEY); }
      resolve();
    };
  });
})();
