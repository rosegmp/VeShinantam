const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const { test } = require('node:test');

function localScansHarness() {
  const records = new Map();
  const metadata = new Map();
  const alerts = [];
  const views = [];
  let input;
  const database = {
    createObjectStore() {},
    transaction() {
      const transaction = {
        objectStore() {
          return {
            put(file, key) {
              records.set(key, file);
              queueMicrotask(() => transaction.oncomplete());
            },
            get(key) {
              const request = {};
              queueMicrotask(() => { request.result = records.get(key); request.onsuccess(); });
              return request;
            },
          };
        },
      };
      return transaction;
    },
  };
  const indexedDB = {
    open() {
      const request = { result: database };
      queueMicrotask(() => { request.onupgradeneeded(); request.onsuccess(); });
      return request;
    },
  };
  const window = {
    open() {
      const view = { location: { replace(value) { view.url = value; } }, close() { view.closed = true; } };
      views.push(view);
      return view;
    },
  };
  vm.runInNewContext(fs.readFileSync('webApp/src/wasmJsMain/resources/local-scans.js', 'utf8'), {
    window, indexedDB, TextDecoder, URL, alert: value => alerts.push(value),
    localStorage: { getItem: key => metadata.get(key) ?? null, setItem: (key, value) => metadata.set(key, value) },
    document: { createElement: () => (input = { click() {} }) },
    queueMicrotask,
  });
  return { scans: window.veshinantamLocalScans, records, alerts, views, selectedInput: () => input };
}

test('local Mishnah Berurah scans stay in device storage and retain page alignment', async () => {
  const harness = localScansHarness();
  const key = 'mishnah-berurah-1';
  harness.scans.choose(key, 3);
  const pdf = new Blob(['%PDF-1.4\ncontent'], { type: 'application/pdf' });
  pdf.name = 'chelek-1.pdf';
  harness.selectedInput().files = [pdf];
  await harness.selectedInput().onchange();

  assert.equal(harness.records.get(key), pdf);
  assert.equal(JSON.parse(harness.scans.readMetadata(key)).anchorOrdinal, 3);
  harness.scans.align(key, 3, 12);
  assert.equal(JSON.parse(harness.scans.readMetadata(key)).anchorPdfPage, 12);
  assert.equal(JSON.parse(harness.scans.readMetadata(key)).calibrated, true);

  await harness.scans.open(key, 12);
  assert.match(harness.views[0].url, /#page=13$/);
  assert.deepEqual(harness.alerts, []);
});

test('non-PDF content is rejected before it enters device storage', async () => {
  const harness = localScansHarness();
  harness.scans.choose('mishnah-berurah-2', 0);
  const invalid = new Blob(['not-a-pdf'], { type: 'application/pdf' });
  invalid.name = 'invalid.pdf';
  harness.selectedInput().files = [invalid];
  await harness.selectedInput().onchange();

  assert.equal(harness.records.size, 0);
  assert.equal(harness.scans.readMetadata('mishnah-berurah-2'), null);
  assert.match(harness.alerts[0], /not a PDF/);
});
