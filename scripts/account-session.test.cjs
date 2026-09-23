const test = require('node:test');
const assert = require('node:assert/strict');
const policy = require('../webApp/src/wasmJsMain/resources/entity-sync-policy.js');
const { createSessionManager, decodeJwt } = require('../webApp/src/wasmJsMain/resources/account-session.js');

const SESSION_KEY = 'session';
const token = claims => {
  const encode = value => Buffer.from(JSON.stringify(value)).toString('base64url');
  return `${encode({ alg: 'none', typ: 'JWT' })}.${encode(claims)}.`;
};

const storage = () => {
  const values = new Map();
  return {
    getItem: key => values.has(key) ? values.get(key) : null,
    setItem: (key, value) => values.set(key, String(value)),
    removeItem: key => values.delete(key),
    values,
  };
};

const options = (store, timedFetch, now = 1_000_000) => ({
  storage: store,
  sessionKey: SESSION_KEY,
  config: { url: 'https://sync.example.test', publishableKey: 'publishable-key' },
  timedFetch,
  policy,
  now,
});

test('JWT decoding and session persistence retain account identity and expiry', () => {
  const manager = createSessionManager();
  const store = storage();
  const accessToken = token({ sub: 'account-a', email: 'person@example.test', exp: 1234 });
  assert.deepEqual(decodeJwt(accessToken), {
    sub: 'account-a', email: 'person@example.test', exp: 1234,
  });
  assert.deepEqual(manager.save(store, SESSION_KEY, accessToken, 'refresh-a'), {
    accessToken,
    refreshToken: 'refresh-a',
    userId: 'account-a',
    email: 'person@example.test',
    expiresAt: 1_234_000,
  });
  assert.equal(manager.read(store, SESSION_KEY).userId, 'account-a');
});

test('a healthy session is returned without contacting the token endpoint', async () => {
  const manager = createSessionManager();
  const store = storage();
  manager.save(store, SESSION_KEY, token({ sub: 'account-a', exp: 2000 }), 'refresh-a');
  const session = await manager.authenticated(options(store, async () => {
    throw new Error('refresh should not be called');
  }));
  assert.equal(session.refreshToken, 'refresh-a');
});

test('concurrent callers share one rotating refresh-token request', async () => {
  const manager = createSessionManager();
  const store = storage();
  manager.save(store, SESSION_KEY, token({ sub: 'account-a', exp: 1 }), 'refresh-old');
  const refreshedAccess = token({ sub: 'account-a', email: 'new@example.test', exp: 3000 });
  let calls = 0;
  const timedFetch = async (url, request) => {
    calls += 1;
    assert.equal(url, 'https://sync.example.test/auth/v1/token?grant_type=refresh_token');
    assert.equal(request.headers.apikey, 'publishable-key');
    assert.deepEqual(JSON.parse(request.body), { refresh_token: 'refresh-old' });
    await new Promise(resolve => setImmediate(resolve));
    return {
      ok: true,
      async json() { return { access_token: refreshedAccess, refresh_token: 'refresh-new' }; },
    };
  };
  const [first, second] = await Promise.all([
    manager.authenticated(options(store, timedFetch)),
    manager.authenticated(options(store, timedFetch)),
  ]);
  assert.equal(calls, 1);
  assert.equal(first.refreshToken, 'refresh-new');
  assert.deepEqual(second, first);
  assert.equal(manager.read(store, SESSION_KEY).email, 'new@example.test');
});

test('a failed refresh releases the lock so the next attempt can recover', async () => {
  const manager = createSessionManager();
  const store = storage();
  manager.save(store, SESSION_KEY, token({ sub: 'account-a', exp: 1 }), 'refresh-old');
  const refreshedAccess = token({ sub: 'account-a', exp: 3000 });
  let calls = 0;
  const timedFetch = async () => {
    calls += 1;
    if (calls === 1) return { ok: false };
    return {
      ok: true,
      async json() { return { access_token: refreshedAccess, refresh_token: 'refresh-new' }; },
    };
  };
  await assert.rejects(
    manager.authenticated(options(store, timedFetch)),
    /session expired/
  );
  const recovered = await manager.authenticated(options(store, timedFetch));
  assert.equal(calls, 2);
  assert.equal(recovered.refreshToken, 'refresh-new');
});

test('an incomplete refresh response cannot overwrite the last recoverable session', async () => {
  const manager = createSessionManager();
  const store = storage();
  manager.save(store, SESSION_KEY, token({ sub: 'account-a', exp: 1 }), 'refresh-safe');
  await assert.rejects(
    manager.authenticated(options(store, async () => ({
      ok: true,
      async json() { return { access_token: token({ sub: 'account-a', exp: 3000 }) }; },
    }))),
    /incomplete token response/
  );
  assert.equal(manager.read(store, SESSION_KEY).refreshToken, 'refresh-safe');
});
