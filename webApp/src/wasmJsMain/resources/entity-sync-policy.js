(() => {
  const accountBinding = (boundAccountId, currentAccountId) => {
    if (!currentAccountId) return { allowed: false, bind: null, reason: 'missing-account' };
    if (!boundAccountId) return { allowed: true, bind: currentAccountId, reason: 'first-account' };
    if (boundAccountId === currentAccountId) return { allowed: true, bind: null, reason: 'same-account' };
    return { allowed: false, bind: null, reason: 'different-account' };
  };

  const settleMutation = (currentMutation, acknowledgedMutationId, serverRevision) => {
    if (!currentMutation) return { matched: false, nextMutation: null };
    if (currentMutation.mutationId === acknowledgedMutationId) {
      return { matched: true, nextMutation: null };
    }
    return {
      matched: false,
      nextMutation: { ...currentMutation, baseRevision: Number(serverRevision) },
    };
  };

  const receiveRemote = (currentMutation, serverRevision) => {
    if (!currentMutation) return { apply: true, nextMutation: null };
    return {
      apply: false,
      nextMutation: {
        ...currentMutation,
        baseRevision: Math.max(Number(currentMutation.baseRevision || 0), Number(serverRevision)),
      },
    };
  };

  const isRetryableStatus = status => [408, 502, 503, 504].includes(Number(status));
  const isRetryableNetworkError = error =>
    error?.name === 'TypeError' || String(error?.message || '').includes('did not respond within');

  const sessionNeedsRefresh = (expiresAt, now = Date.now(), refreshSkewMs = 60000) =>
    !Number.isFinite(Number(expiresAt)) || Number(expiresAt) <= Number(now) + Number(refreshSkewMs);

  const policy = Object.freeze({
    accountBinding,
    settleMutation,
    receiveRemote,
    isRetryableStatus,
    isRetryableNetworkError,
    sessionNeedsRefresh,
  });
  globalThis.VeShinantamEntitySyncPolicy = policy;
  if (typeof module !== 'undefined' && module.exports) module.exports = policy;
})();
