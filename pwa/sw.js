/* Service worker: app shell cache-first, API network-first dengan fallback cache. */
const VERSION = 'v1';
const SHELL_CACHE = `shell-${VERSION}`;
const API_CACHE = `api-${VERSION}`;
const SHELL = [
  './', './index.html', './style.css', './app.js', './manifest.webmanifest',
  './icons/icon-192.png', './icons/icon-512.png', './icons/maskable-512.png',
];
const API_MAX_ENTRIES = 100;

self.addEventListener('install', (event) => {
  event.waitUntil(caches.open(SHELL_CACHE).then((c) => c.addAll(SHELL)));
  self.skipWaiting();
});

self.addEventListener('activate', (event) => {
  event.waitUntil((async () => {
    const keep = new Set([SHELL_CACHE, API_CACHE]);
    for (const key of await caches.keys()) if (!keep.has(key)) await caches.delete(key);
    await self.clients.claim();
  })());
});

self.addEventListener('message', (event) => {
  if (event.data === 'GET_VERSION') event.source.postMessage({ type: 'VERSION', version: VERSION });
});

async function trimCache(cache) {
  const keys = await cache.keys();
  for (let i = 0; i < keys.length - API_MAX_ENTRIES; i++) await cache.delete(keys[i]);
}

async function networkFirstApi(request) {
  const cache = await caches.open(API_CACHE);
  try {
    const response = await fetch(request);
    if (response.ok) {
      await cache.put(request, response.clone());
      trimCache(cache);
    }
    return response;
  } catch (err) {
    const cached = await cache.match(request);
    if (cached) return cached;
    throw err;
  }
}

self.addEventListener('fetch', (event) => {
  const { request } = event;
  if (request.method !== 'GET') return;
  const url = new URL(request.url);

  if (url.origin === self.location.origin) {
    if (request.mode === 'navigate') {
      event.respondWith(fetch(request).catch(() => caches.match('./index.html')));
      return;
    }
    event.respondWith((async () => {
      const cached = await caches.match(request, { ignoreSearch: true });
      const fresh = fetch(request).then(async (res) => {
        if (res.ok) (await caches.open(SHELL_CACHE)).put(request, res.clone());
        return res;
      }).catch(() => null);
      return cached || (await fresh) || Response.error();
    })());
    return;
  }

  if (url.pathname.includes('/api/v1/')) {
    event.respondWith(networkFirstApi(request));
  }
});
