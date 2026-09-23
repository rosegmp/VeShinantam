const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');

const source = fs.readFileSync(
  'webApp/src/wasmJsMain/resources/entity-sync.js',
  'utf8'
);

const STATE_KEY = 'veshinantam.web.v1';
const FALLBACK_PENDING_KEY = 'veshinantam.web.v1.pending';

const nextTurn = () => new Promise(resolve => setImmediate(resolve));

function storageHarness({
  localState = null,
  indexedState = null,
  fallbackPending = false,
  openMode = 'success',
  failWrites = false,
  failAppStateWrites = false,
} = {}) {
  const localValues = new Map();
  if (localState !== null) localValues.set(STATE_KEY, localState);
  if (fallbackPending) localValues.set(FALLBACK_PENDING_KEY, '1');

  const stores = new Map();
  if (indexedState !== null) stores.set('app_state', new Map([['state', indexedState]]));

  const config = { failWrites, failAppStateWrites };
  const request = (result, error = null) => {
    const value = { result, error, onsuccess: null, onerror: null };
    queueMicrotask(() => error ? value.onerror?.() : value.onsuccess?.());
    return value;
  };

  const database = {
    closed: false,
    onclose: null,
    onversionchange: null,
    objectStoreNames: { contains: name => stores.has(name) },
    createObjectStore(name) {
      if (!stores.has(name)) stores.set(name, new Map());
      return stores.get(name);
    },
    close() {
      if (this.closed) return;
      this.closed = true;
      this.onclose?.();
    },
    transaction(name, mode) {
      if (this.closed) throw new Error('database connection is closed');
      const writeFails = mode === 'readwrite' &&
        (config.failWrites || (config.failAppStateWrites && name === 'app_state'));
      const transaction = {
        error: writeFails ? new Error('simulated write failure') : null,
        oncomplete: null,
        onerror: null,
        onabort: null,
        objectStore(storeName) {
          if (!stores.has(storeName)) throw new Error(`missing object store: ${storeName}`);
          const store = stores.get(storeName);
          return {
            get(key) { return request(store.get(key)); },
            getAll() { return request(Array.from(store.values())); },
            put(value, key) {
              if (!writeFails) store.set(key, value);
              return request(key);
            },
            delete(key) {
              if (!writeFails) store.delete(key);
              return request(undefined);
            },
          };
        },
      };
      queueMicrotask(() => {
        if (transaction.error) transaction.onabort?.();
        else transaction.oncomplete?.();
      });
      return transaction;
    },
  };

  const indexedDB = {
    open(_name, _version) {
      const openRequest = {
        result: database,
        error: openMode === 'error' ? new Error('simulated open failure') : null,
        onupgradeneeded: null,
        onsuccess: null,
        onerror: null,
        onblocked: null,
      };
      setImmediate(() => {
        if (openMode === 'error') openRequest.onerror?.();
        else if (openMode === 'blocked') openRequest.onblocked?.();
        else {
          openRequest.onupgradeneeded?.();
          openRequest.onsuccess?.();
        }
      });
      return openRequest;
    },
  };

  const localStorage = {
    getItem(key) { return localValues.has(key) ? localValues.get(key) : null; },
    setItem(key, value) { localValues.set(key, String(value)); },
    removeItem(key) { localValues.delete(key); },
  };
  const window = { indexedDB };
  const context = {
    window,
    indexedDB,
    localStorage,
    setImmediate,
    queueMicrotask,
    VeShinantamEntitySyncPolicy: {
      settleMutation: () => ({ matched: false, nextMutation: null }),
      receiveRemote: () => ({ apply: true, nextMutation: null }),
    },
  };
  vm.runInNewContext(source, context, { filename: 'entity-sync.js' });
  return { window, database, stores, localValues, config };
}

test('fresh IndexedDB creates every version-2 object store', async () => {
  const harness = storageHarness();
  await harness.window.veshinantamStorageReady;
  assert.deepEqual(
    Array.from(harness.stores.keys()).sort(),
    ['app_state', 'sync_metadata', 'sync_outbox']
  );
});

test('legacy local-storage state migrates atomically into IndexedDB', async () => {
  const state = JSON.stringify({ language: 'en', schedules: [], tasks: [] });
  const harness = storageHarness({ localState: state });
  await harness.window.veshinantamStorageReady;
  assert.equal(harness.stores.get('app_state').get('state'), state);
  assert.equal(harness.localValues.has(STATE_KEY), false);
  assert.equal(harness.window.veshinantamReadState(), state);
});

test('an existing IndexedDB state wins over a stale startup mirror', async () => {
  const indexedState = JSON.stringify({ language: 'he', schedules: [], tasks: [] });
  const harness = storageHarness({ indexedState, localState: '{"stale":true}' });
  await harness.window.veshinantamStorageReady;
  assert.equal(harness.window.veshinantamReadState(), indexedState);
  assert.equal(harness.localValues.has(STATE_KEY), false);
});

test('failed database open and blocked upgrade both release startup to the durable fallback', async () => {
  for (const openMode of ['error', 'blocked']) {
    const state = JSON.stringify({ mode: openMode });
    const harness = storageHarness({ localState: state, openMode });
    await harness.window.veshinantamStorageReady;
    assert.equal(harness.window.veshinantamReadState(), state);
    const changed = JSON.stringify({ recovered: openMode });
    harness.window.veshinantamPersistState(changed);
    await nextTurn();
    assert.equal(harness.localValues.get(STATE_KEY), changed);
  }
});

test('interrupted legacy migration preserves the local state', async () => {
  const state = JSON.stringify({ schedules: [{ id: 'safe' }], tasks: [] });
  const harness = storageHarness({ localState: state, failWrites: true });
  await harness.window.veshinantamStorageReady;
  assert.equal(harness.localValues.get(STATE_KEY), state);
  assert.equal(harness.window.veshinantamReadState(), state);
});

test('a pending fallback replaces an older IndexedDB snapshot after restart', async () => {
  const older = JSON.stringify({ revision: 1 });
  const recovered = JSON.stringify({ revision: 2 });
  const harness = storageHarness({
    indexedState: older,
    localState: recovered,
    fallbackPending: true,
  });
  await harness.window.veshinantamStorageReady;
  assert.equal(harness.window.veshinantamReadState(), recovered);
  assert.equal(harness.stores.get('app_state').get('state'), recovered);
  assert.equal(harness.localValues.has(STATE_KEY), false);
  assert.equal(harness.localValues.has(FALLBACK_PENDING_KEY), false);
});

test('failed and closed IndexedDB writes return the newest state to local storage', async () => {
  const harness = storageHarness();
  await harness.window.veshinantamStorageReady;

  harness.config.failWrites = true;
  const failedWrite = JSON.stringify({ revision: 1 });
  harness.window.veshinantamPersistState(failedWrite);
  await nextTurn();
  assert.equal(harness.localValues.get(STATE_KEY), failedWrite);
  assert.equal(harness.localValues.get(FALLBACK_PENDING_KEY), '1');

  harness.config.failWrites = false;
  harness.database.close();
  const afterClose = JSON.stringify({ revision: 2 });
  harness.window.veshinantamPersistState(afterClose);
  await nextTurn();
  assert.equal(harness.localValues.get(STATE_KEY), afterClose);
});

test('failed remote-state storage is recoverable and does not suppress later local diffs', async () => {
  const harness = storageHarness();
  await harness.window.veshinantamStorageReady;
  harness.config.failAppStateWrites = true;
  await assert.rejects(harness.window.veshinantamApplyEntityRecords([]), /simulated write failure/);
  assert.equal(harness.localValues.get(FALLBACK_PENDING_KEY), '1');

  harness.config.failAppStateWrites = false;
  harness.window.veshinantamPersistState(JSON.stringify({ language: 'he', schedules: [], tasks: [] }));
  const outbox = await harness.window.veshinantamReadOutbox();
  assert.equal(outbox.some(mutation => mutation.entityType === 'PREFERENCES'), true);
});
