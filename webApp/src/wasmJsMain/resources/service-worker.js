const CACHE_PREFIX = 'veshinantam-web-';
const CACHE = 'veshinantam-web-v42';
const PRECACHE = [
  './',
  './index.html',
  './veshinantam.js',
  './entity-sync-policy.js',
  './account-session.js',
  './entity-sync.js',
  './preset-updates.js',
  './supabase-config.js',
  './manifest.webmanifest',
  './icon.svg',
  /*__PRECACHE_FILES__*/
];

const cacheResponse = async (request, response) => {
  if (response && response.ok && response.type === 'basic') {
    const cache = await caches.open(CACHE);
    await cache.put(request, response.clone());
  }
  return response;
};

self.addEventListener('install', event => {
  event.waitUntil(
    caches.open(CACHE)
      .then(cache => cache.addAll(PRECACHE))
      .then(() => self.skipWaiting())
  );
});

self.addEventListener('activate', event => {
  event.waitUntil(
    caches.keys()
      .then(keys => Promise.all(keys.filter(key => key.startsWith(CACHE_PREFIX) && key !== CACHE).map(key => caches.delete(key))))
      .then(() => self.clients.claim())
  );
});

self.addEventListener('fetch', event => {
  const request = event.request;
  if (request.method !== 'GET' || new URL(request.url).origin !== self.location.origin) return;
  // The signed catalog must always be fetched from the network; verified copies live in app storage.
  if (new URL(request.url).pathname.endsWith('/preset-catalog.json')) return;

  if (request.mode === 'navigate') {
    event.respondWith(
      fetch(request)
        .then(response => cacheResponse('./index.html', response))
        .catch(() => caches.match('./index.html'))
    );
    return;
  }

  event.respondWith(
    caches.match(request).then(cached => cached || fetch(request).then(response => cacheResponse(request, response)))
  );
});

self.addEventListener('notificationclick', event => {
  event.notification.close();
  const target = new URL(event.notification.data?.url || './?view=today', self.registration.scope).href;
  event.waitUntil(
    self.clients.matchAll({ type: 'window', includeUncontrolled: true }).then(clients => {
      const existing = clients.find(client => new URL(client.url).origin === self.location.origin);
      if (existing) return existing.navigate(target).then(client => client?.focus());
      return self.clients.openWindow(target);
    })
  );
});
