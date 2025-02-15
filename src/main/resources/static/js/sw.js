const cacheName = "mango-reader-v1";
const contentToCache = [
  "/",
  "/css/css-reset.css",
  "/css/styles.css",
  "/js/app.js",
  "/js/no-image-context-popups.js",
  "/manifest.json",
  "/icon.png",
];

// Install the Service Worker
self.addEventListener("install", (e) => {
  e.waitUntil(
    (async () => {
      const cache = await caches.open(cacheName);
      await cache.addAll(contentToCache);
    })()
  );
});

// Fetch content using the Service Worker
self.addEventListener("fetch", (e) => {
  e.respondWith(
    (async () => {
      const r = await caches.match(e.request);
      if (r) return r;
      const response = await fetch(e.request);
      const cache = await caches.open(cacheName);
      cache.put(e.request, response.clone());
      return response;
    })()
  );
});
