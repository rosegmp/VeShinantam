const test = require('node:test');
const assert = require('node:assert/strict');
const { storageHarness } = require('./support/indexeddb-harness.cjs');

const clone = value => value == null ? value : JSON.parse(JSON.stringify(value));

class SyntheticEntityServer {
  constructor() {
    this.entities = new Map();
    this.receipts = new Map();
    this.revision = 0;
  }

  apply(mutations) {
    return mutations.map(mutation => {
      const receipt = this.receipts.get(mutation.mutationId);
      if (receipt) return {
        mutation_id: mutation.mutationId,
        revision: receipt,
        conflict: false,
        remote_payload: null,
        remote_deleted: Boolean(mutation.deleted),
      };

      const key = `${mutation.entityType}:${mutation.entityId}`;
      const current = this.entities.get(key);
      if ((current && current.revision !== Number(mutation.baseRevision || 0)) ||
          (!current && Number(mutation.baseRevision || 0) !== 0)) {
        return {
          mutation_id: mutation.mutationId,
          revision: current?.revision || 0,
          conflict: true,
          remote_payload: clone(current?.payload || null),
          remote_deleted: Boolean(current?.deleted),
        };
      }

      const revision = ++this.revision;
      this.entities.set(key, {
        entity_type: mutation.entityType,
        entity_id: mutation.entityId,
        payload: mutation.deleted ? null : clone(mutation.payload),
        deleted: Boolean(mutation.deleted),
        revision,
        updated_at: new Date(revision * 1000).toISOString(),
      });
      this.receipts.set(mutation.mutationId, revision);
      return {
        mutation_id: mutation.mutationId,
        revision,
        conflict: false,
        remote_payload: null,
        remote_deleted: Boolean(mutation.deleted),
      };
    });
  }

  pull(cursor) {
    return Array.from(this.entities.values())
      .filter(record => record.revision > cursor)
      .sort((left, right) => left.revision - right.revision)
      .map(clone);
  }
}

const baseState = () => ({
  language: 'en',
  schedules: [{
    id: 'schedule-1',
    name: 'Mishnah Yomis',
    nameHebrew: 'משנה יומית',
    presetId: 'mishnah-yomis',
    material: 'MISHNAH',
    materialType: 'MISHNAH',
    pace: 2,
    weekdays: [0, 1, 2, 3, 4, 5, 6],
    chazarahOffsets: [],
    startDate: '2026-09-23',
    active: true,
    revision: 0,
  }],
  tasks: [{
    id: 'task-1',
    stableKey: 'task-1',
    scheduleId: 'schedule-1',
    referenceEnglish: 'Berachos 1:1',
    referenceHebrew: 'ברכות א:א',
    materialType: 'MISHNAH',
    quantity: 1,
    dueDate: '2026-09-23',
    originalLearningDate: '2026-09-23',
    type: 'LEARNING',
    completed: false,
    completedAt: null,
    revision: 0,
  }],
  exclusions: [],
  goals: [],
});

const blankState = () => ({ language: 'en', schedules: [], tasks: [], exclusions: [], goals: [] });
const readState = client => JSON.parse(client.window.veshinantamReadState());
const writeState = (client, state) => client.window.veshinantamPersistState(JSON.stringify(state));

async function synchronize(client, server, { afterServerApply } = {}) {
  const state = readState(client);
  await client.window.veshinantamEnsureEntitySeed(state);
  const mutations = await client.window.veshinantamReadOutbox();
  const results = server.apply(mutations);
  if (afterServerApply) await afterServerApply({ mutations, results });

  const conflicts = [];
  for (const mutation of mutations) {
    const result = results.find(value => value.mutation_id === mutation.mutationId);
    assert.ok(result, `missing result for ${mutation.mutationId}`);
    const matched = await client.window.veshinantamSettleOutbox(
      mutation.entityType,
      mutation.entityId,
      mutation.mutationId,
      result.revision
    );
    if (result.conflict && matched) conflicts.push({
      entity_type: mutation.entityType,
      entity_id: mutation.entityId,
      payload: result.remote_payload,
      deleted: result.remote_deleted,
      revision: result.revision,
      updated_at: new Date().toISOString(),
    });
  }
  if (conflicts.length) await client.window.veshinantamApplyEntityRecords(conflicts);

  const cursor = await client.window.veshinantamGetSyncCursor();
  const records = server.pull(cursor);
  if (records.length) {
    await client.window.veshinantamApplyEntityRecords(records);
    await client.window.veshinantamSetSyncCursor(
      Math.max(...records.map(record => record.revision))
    );
  }
  return readState(client);
}

async function readyClient(state) {
  const client = storageHarness({ localState: JSON.stringify(state) });
  await client.window.veshinantamStorageReady;
  return client;
}

test('two offline devices converge through edit conflicts and tombstones without silent loss', async () => {
  const server = new SyntheticEntityServer();
  const first = await readyClient(baseState());
  const second = await readyClient(blankState());

  await synchronize(first, server);
  await synchronize(second, server);
  assert.equal(readState(second).tasks[0].referenceEnglish, 'Berachos 1:1');

  const secondOffline = readState(second);
  Object.assign(secondOffline.tasks[0], {
    completed: true,
    completedAt: '2026-09-23T14:00:00.000Z',
    completionLocalDate: '2026-09-23',
    completionZoneId: 'America/New_York',
  });
  writeState(second, secondOffline);

  const firstOffline = readState(first);
  firstOffline.tasks[0].referenceEnglish = 'Offline label that should lose';
  writeState(first, firstOffline);

  await synchronize(second, server);
  await synchronize(first, server);
  await synchronize(second, server);
  assert.equal(readState(first).tasks[0].completed, true);
  assert.equal(readState(first).tasks[0].referenceEnglish, 'Berachos 1:1');
  assert.equal(readState(second).tasks[0].completed, true);

  const editedSecond = readState(second);
  editedSecond.tasks[0].referenceEnglish = 'Cloud winner';
  writeState(second, editedSecond);
  const deletedFirst = readState(first);
  deletedFirst.tasks = [];
  writeState(first, deletedFirst);

  await synchronize(second, server);
  await synchronize(first, server);
  assert.equal(readState(first).tasks[0].referenceEnglish, 'Cloud winner');

  const finalDelete = readState(first);
  finalDelete.tasks = [];
  writeState(first, finalDelete);
  await synchronize(first, server);
  await synchronize(second, server);
  assert.deepEqual(readState(first).tasks, []);
  assert.deepEqual(readState(second).tasks, []);
});

test('an interrupted acknowledgement is idempotent and a newer local replacement survives a late acknowledgement', async () => {
  const server = new SyntheticEntityServer();
  const client = await readyClient(baseState());
  await synchronize(client, server);

  const completed = readState(client);
  completed.tasks[0].completedAt = '2026-09-23T15:00:00.000Z';
  completed.tasks[0].completed = true;
  writeState(client, completed);
  const unacknowledged = await client.window.veshinantamReadOutbox();
  server.apply(unacknowledged);
  const revisionAfterInterruptedUpload = server.revision;

  await synchronize(client, server);
  assert.equal(server.revision, revisionAfterInterruptedUpload);
  assert.equal((await client.window.veshinantamReadOutbox()).length, 0);

  const firstEdit = readState(client);
  firstEdit.tasks[0].referenceEnglish = 'First edit';
  writeState(client, firstEdit);
  await synchronize(client, server, {
    afterServerApply: async () => {
      const replacement = readState(client);
      replacement.tasks[0].referenceEnglish = 'Replacement edit';
      writeState(client, replacement);
      await client.window.veshinantamReadOutbox();
    },
  });

  const pending = await client.window.veshinantamReadOutbox();
  assert.equal(pending.length, 1);
  assert.equal(pending[0].payload.labelEnglish, 'Replacement edit');
  assert.equal(pending[0].baseRevision, server.revision);

  await synchronize(client, server);
  assert.equal(server.entities.get('TASK:task-1').payload.labelEnglish, 'Replacement edit');
  assert.equal(readState(client).tasks[0].referenceEnglish, 'Replacement edit');
  assert.equal((await client.window.veshinantamReadOutbox()).length, 0);
});
