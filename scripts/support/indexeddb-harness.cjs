const fs = require('node:fs');
const vm = require('node:vm');
const policy = require('../../webApp/src/wasmJsMain/resources/entity-sync-policy.js');

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
      setImmediate(() => {
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
    VeShinantamEntitySyncPolicy: policy,
  };
  vm.runInNewContext(source, context, { filename: 'entity-sync.js' });
  return { window, database, stores, localValues, config };
}

module.exports = { STATE_KEY, FALLBACK_PENDING_KEY, nextTurn, storageHarness };
