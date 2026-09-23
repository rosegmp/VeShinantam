const test = require('node:test');
const assert = require('node:assert/strict');
const policy = require('../webApp/src/wasmJsMain/resources/entity-sync-policy.js');

test('first account binds and the same account can reconnect', () => {
  assert.deepEqual(policy.accountBinding(null, 'account-a'), {
    allowed: true, bind: 'account-a', reason: 'first-account'
  });
  assert.deepEqual(policy.accountBinding('account-a', 'account-a'), {
    allowed: true, bind: null, reason: 'same-account'
  });
});

test('account switching and missing token identity are rejected', () => {
  assert.equal(policy.accountBinding('account-a', 'account-b').allowed, false);
  assert.equal(policy.accountBinding(null, '').reason, 'missing-account');
});

test('matching upload acknowledgement removes exactly that mutation', () => {
  const current = { mutationId: 'm1', entityId: 'task-1', baseRevision: 4, deleted: false };
  assert.deepEqual(policy.settleMutation(current, 'm1', 5), {
    matched: true, nextMutation: null
  });
});

test('late acknowledgement preserves a newer offline edit and rebases it', () => {
  const newer = { mutationId: 'm2', entityId: 'task-1', baseRevision: 4, payload: { completed: true } };
  assert.deepEqual(policy.settleMutation(newer, 'm1', 8), {
    matched: false,
    nextMutation: { ...newer, baseRevision: 8 }
  });
});

test('duplicate acknowledgement cannot remove a completed or replacement mutation', () => {
  assert.deepEqual(policy.settleMutation(null, 'm1', 8), {
    matched: false, nextMutation: null
  });
  const replacementDelete = { mutationId: 'm3', entityId: 'task-1', baseRevision: 8, deleted: true };
  assert.equal(policy.settleMutation(replacementDelete, 'm1', 9).nextMutation.deleted, true);
});

test('remote pull never overwrites a pending local edit or delete', () => {
  const pendingEdit = { mutationId: 'm4', baseRevision: 5, deleted: false };
  assert.deepEqual(policy.receiveRemote(pendingEdit, 9), {
    apply: false,
    nextMutation: { ...pendingEdit, baseRevision: 9 }
  });
  const pendingDelete = { mutationId: 'm5', baseRevision: 10, deleted: true };
  assert.deepEqual(policy.receiveRemote(pendingDelete, 9), {
    apply: false,
    nextMutation: pendingDelete
  });
  assert.deepEqual(policy.receiveRemote(null, 9), { apply: true, nextMutation: null });
});

test('retry policy covers transient responses and network timeouts only', () => {
  for (const status of [408, 502, 503, 504]) assert.equal(policy.isRetryableStatus(status), true);
  for (const status of [400, 401, 403, 409, 500]) assert.equal(policy.isRetryableStatus(status), false);
  assert.equal(policy.isRetryableNetworkError({ name: 'TypeError' }), true);
  assert.equal(policy.isRetryableNetworkError({ message: 'The sync server did not respond within 30 seconds.' }), true);
  assert.equal(policy.isRetryableNetworkError({ message: 'permission denied' }), false);
});

test('token refresh policy refreshes expired and near-expiry sessions', () => {
  const now = 1_000_000;
  assert.equal(policy.sessionNeedsRefresh(now + 120_000, now), false);
  assert.equal(policy.sessionNeedsRefresh(now + 60_001, now), false);
  assert.equal(policy.sessionNeedsRefresh(now + 60_000, now), true);
  assert.equal(policy.sessionNeedsRefresh(now - 1, now), true);
  assert.equal(policy.sessionNeedsRefresh(undefined, now), true);
});
