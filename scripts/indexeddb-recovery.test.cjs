const test = require('node:test');
const assert = require('node:assert/strict');
const {
  STATE_KEY,
  FALLBACK_PENDING_KEY,
  nextTurn,
  storageHarness,
} = require('./support/indexeddb-harness.cjs');

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
