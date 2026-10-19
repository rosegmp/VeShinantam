(() => {
  const SESSION_KEY = 'veshinantam.supabase.session.v1';
  const config = window.VESHINANTAM_SUPABASE || {};
  let desiredEnabled = false;
  let inFlight = Promise.resolve();
  const timedFetch = async (url, options) => {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), 10000);
    try { return await fetch(url, { ...options, signal: controller.signal }); }
    finally { clearTimeout(timer); }
  };

  const supported = () => Boolean(config.webPushPublicKey && config.url && config.publishableKey &&
    'serviceWorker' in navigator && 'PushManager' in window && 'Notification' in window);
  const keyBytes = value => Uint8Array.from(atob(value.replace(/-/g, '+').replace(/_/g, '/')), character => character.charCodeAt(0));
  const session = async () => {
    if (!window.VeShinantamAccountSession.read(localStorage, SESSION_KEY)) return null;
    return window.VeShinantamAccountSession.authenticated({
      storage: localStorage,
      sessionKey: SESSION_KEY,
      config,
      timedFetch,
      policy: window.VeShinantamEntitySyncPolicy,
    });
  };
  const api = async (path, options) => {
    const account = await session();
    if (!account) return null;
    const response = await timedFetch(`${config.url}/rest/v1/${path}`, {
      ...options,
      headers: {
        apikey: config.publishableKey,
        Authorization: `Bearer ${account.accessToken}`,
        'Content-Type': 'application/json',
        ...options?.headers,
      },
    });
    if (!response.ok) throw new Error(`Push subscription could not be saved (${response.status}).`);
    return account;
  };
  const remove = async () => {
    if (!supported()) return;
    const registration = await navigator.serviceWorker.ready;
    const subscription = await registration.pushManager.getSubscription();
    if (!subscription) return;
    await subscription.unsubscribe();
    await api(`web_push_subscriptions?endpoint=eq.${encodeURIComponent(subscription.endpoint)}`, { method: 'DELETE' });
  };
  const register = async () => {
    if (!supported() || !desiredEnabled || Notification.permission !== 'granted') return;
    const account = await session();
    if (!account) return;
    const registration = await navigator.serviceWorker.ready;
    const subscription = await registration.pushManager.getSubscription() || await registration.pushManager.subscribe({
      userVisibleOnly: true,
      applicationServerKey: keyBytes(config.webPushPublicKey),
    });
    const serialized = subscription.toJSON();
    await api('web_push_subscriptions?on_conflict=user_id,endpoint', {
      method: 'POST',
      headers: { Prefer: 'resolution=merge-duplicates' },
      body: JSON.stringify({
        user_id: account.userId,
        endpoint: serialized.endpoint,
        p256dh: serialized.keys?.p256dh,
        auth_secret: serialized.keys?.auth,
        time_zone: Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC',
      }),
    });
  };
  const queue = work => {
    inFlight = inFlight.catch(() => {}).then(work);
    inFlight.catch(error => console.warn('Web Push setup failed:', error));
    return inFlight;
  };

  window.veshinantamPush = {
    update(enabled) {
      desiredEnabled = Boolean(enabled);
      queue(() => desiredEnabled ? register() : remove());
    },
    requestPermission() {
      if (!('Notification' in window) || Notification.permission !== 'default') return;
      Promise.resolve(Notification.requestPermission())
        .then(permission => { if (permission === 'granted') queue(register); })
        .catch(error => console.warn('Notification permission was not granted:', error));
    },
    signOut() { return queue(remove); },
  };
})();
