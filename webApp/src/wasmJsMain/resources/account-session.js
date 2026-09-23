(() => {
  const decodeJwt = token => {
    try {
      const body = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
      return JSON.parse(decodeURIComponent(Array.from(atob(body), character =>
        '%' + character.charCodeAt(0).toString(16).padStart(2, '0')
      ).join('')));
    } catch (_) { return {}; }
  };

  const createSessionManager = () => {
    let refreshPromise = null;

    const read = (storage, sessionKey) => {
      try { return JSON.parse(storage.getItem(sessionKey) || 'null'); }
      catch (_) { return null; }
    };

    const save = (storage, sessionKey, accessToken, refreshToken) => {
      const claims = decodeJwt(accessToken);
      const session = {
        accessToken,
        refreshToken,
        userId: claims.sub || '',
        email: claims.email || '',
        expiresAt: Number(claims.exp || 0) * 1000,
      };
      storage.setItem(sessionKey, JSON.stringify(session));
      return session;
    };

    const authenticated = async ({ storage, sessionKey, config, timedFetch, policy, now = Date.now() }) => {
      const session = read(storage, sessionKey);
      if (!session) throw new Error('Please sign in again.');
      if (!policy.sessionNeedsRefresh(session.expiresAt, now)) return session;
      if (refreshPromise) return refreshPromise;

      refreshPromise = (async () => {
        const response = await timedFetch(`${config.url}/auth/v1/token?grant_type=refresh_token`, {
          method: 'POST',
          headers: { 'apikey': config.publishableKey, 'Content-Type': 'application/json' },
          body: JSON.stringify({ refresh_token: session.refreshToken }),
        });
        if (!response.ok) throw new Error('Your session expired. Please sign in again.');
        const refreshed = await response.json();
        if (!refreshed.access_token || !refreshed.refresh_token) {
          throw new Error('The session server returned an incomplete token response.');
        }
        return save(storage, sessionKey, refreshed.access_token, refreshed.refresh_token);
      })();
      try {
        return await refreshPromise;
      } finally {
        refreshPromise = null;
      }
    };

    return Object.freeze({ decodeJwt, read, save, authenticated });
  };

  const manager = createSessionManager();
  globalThis.VeShinantamAccountSession = manager;
  if (typeof module !== 'undefined' && module.exports) {
    module.exports = { createSessionManager, decodeJwt };
  }
})();
