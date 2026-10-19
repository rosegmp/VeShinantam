const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const source = fs.readFileSync(path.join(__dirname, '../webApp/src/wasmJsMain/resources/push-reminders.js'), 'utf8');
const tick = () => new Promise(resolve => setTimeout(resolve, 0));

function browser({ signedIn = true, permission = 'granted' } = {}) {
  const calls = [];
  let subscription = null;
  let unsubscribed = false;
  const storage = new Map(signedIn ? [['veshinantam.supabase.session.v1', '{}']] : []);
  const localStorage = {
    getItem: key => storage.get(key) || null,
    setItem: (key, value) => storage.set(key, value),
  };
  const account = { userId: 'test-user', accessToken: 'test-token' };
  const registration = {
    pushManager: {
      getSubscription: async () => subscription,
      subscribe: async options => {
        assert.equal(options.userVisibleOnly, true);
        subscription = {
          endpoint: 'https://push.example/subscription',
          toJSON: () => ({ endpoint: 'https://push.example/subscription', keys: { p256dh: 'public-key', auth: 'auth-key' } }),
          unsubscribe: async () => { unsubscribed = true; subscription = null; },
        };
        return subscription;
      },
    },
  };
  const window = {
    PushManager: function PushManager() {},
    Notification: { permission, requestPermission: async () => 'granted' },
    VESHINANTAM_SUPABASE: { url: 'https://project.example', publishableKey: 'public', webPushPublicKey: 'AQID' },
    VeShinantamEntitySyncPolicy: {},
    VeShinantamAccountSession: {
      read: () => signedIn ? account : null,
      authenticated: async () => account,
    },
  };
  const context = {
    window, localStorage, navigator: { serviceWorker: { ready: Promise.resolve(registration) } },
    Notification: window.Notification,
    Intl: { DateTimeFormat: () => ({ resolvedOptions: () => ({ timeZone: 'America/New_York' }) }) },
    Uint8Array, atob, AbortController, setTimeout, clearTimeout, console,
    fetch: async (url, options) => { calls.push({ url, options }); return { ok: true }; },
  };
  vm.runInNewContext(source, context);
  return { window, calls, get unsubscribed() { return unsubscribed; } };
}

test('signed-in browser registers a push subscription and removes it when disabled', async () => {
  const app = browser();
  app.window.veshinantamPush.update(true);
  await tick();
  assert.equal(app.calls.length, 1);
  assert.equal(app.calls[0].options.method, 'POST');
  const payload = JSON.parse(app.calls[0].options.body);
  assert.equal(payload.user_id, 'test-user');
  assert.equal(payload.time_zone, 'America/New_York');
  assert.equal(payload.endpoint, 'https://push.example/subscription');

  app.window.veshinantamPush.update(false);
  await tick();
  assert.equal(app.calls[1].options.method, 'DELETE');
  assert.equal(app.unsubscribed, true);
});

test('a browser without an account does not subscribe to remote reminders', async () => {
  const app = browser({ signedIn: false });
  app.window.veshinantamPush.update(true);
  await tick();
  assert.equal(app.calls.length, 0);
});

test('sign-out removes the remote subscription before the account is cleared', async () => {
  const app = browser();
  app.window.veshinantamPush.update(true);
  await tick();
  await app.window.veshinantamPush.signOut();
  assert.equal(app.calls.at(-1).options.method, 'DELETE');
  assert.equal(app.unsubscribed, true);
});
