const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');

const source = fs.readFileSync(
  'webApp/src/wasmJsMain/resources/service-worker.js',
  'utf8'
).replace('/*__PRECACHE_FILES__*/', '');

function workerHarness({ failPrecache = false, cacheKeys = [] } = {}) {
  const events = new Map();
  const opened = [];
  const deleted = [];
  const precached = [];
  let skippedWaiting = false;
  let claimedClients = false;
  const cache = {
    async addAll(entries) {
      precached.push(...entries);
      if (failPrecache) throw new Error('simulated interrupted download');
    },
    async match() { return null; },
    async put() {},
  };
  const context = {
    URL,
    fetch: async () => { throw new Error('not used'); },
    caches: {
      async open(name) { opened.push(name); return cache; },
      async keys() { return cacheKeys; },
      async delete(name) { deleted.push(name); return true; },
      async match() { return null; },
    },
    self: {
      location: { origin: 'https://example.test' },
      addEventListener(type, listener) { events.set(type, listener); },
      async skipWaiting() { skippedWaiting = true; },
      clients: { async claim() { claimedClients = true; }, async matchAll() { return []; }, async openWindow() {} },
      registration: { async showNotification() {} },
    },
  };
  vm.runInNewContext(source, context, { filename: 'service-worker.js' });
  const dispatch = type => {
    let work;
    events.get(type)({ waitUntil(promise) { work = promise; } });
    return work;
  };
  return {
    dispatch,
    opened,
    deleted,
    precached,
    skippedWaiting: () => skippedWaiting,
    claimedClients: () => claimedClients,
  };
}

test('new worker activates only after every offline asset is cached', async () => {
  const worker = workerHarness();
  await worker.dispatch('install');
  assert.equal(worker.skippedWaiting(), true);
  assert.equal(worker.precached.includes('./entity-sync-policy.js'), true);
  assert.equal(worker.precached.includes('./account-session.js'), true);
  assert.equal(worker.precached.includes('./entity-sync.js'), true);
  assert.equal(worker.precached.includes('./veshinantam.js'), true);
});

test('interrupted precache rejects installation and leaves the prior cache untouched', async () => {
  const worker = workerHarness({ failPrecache: true, cacheKeys: ['veshinantam-web-v39'] });
  await assert.rejects(worker.dispatch('install'), /interrupted download/);
  assert.equal(worker.skippedWaiting(), false);
  assert.deepEqual(worker.deleted, []);
});

test('activation removes only superseded app caches', async () => {
  const worker = workerHarness({
    cacheKeys: ['veshinantam-web-v38', 'veshinantam-web-v39', 'veshinantam-web-v40', 'another-app-v1'],
  });
  await worker.dispatch('activate');
  assert.deepEqual(worker.deleted.sort(), ['veshinantam-web-v38', 'veshinantam-web-v39']);
  assert.equal(worker.claimedClients(), true);
});
