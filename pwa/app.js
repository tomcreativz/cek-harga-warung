'use strict';

const DEFAULT_API = 'https://api-products.alpha-projects.cloud/api/v1';
const PAGE_SIZE = 50;
const HISTORY_MAX = 8;
const TIMEOUT_MS = 15000;

const $ = (id) => document.getElementById(id);
const els = {
  form: $('search-form'), q: $('q'), status: $('status'), results: $('results'), more: $('more'),
  scanBtn: $('scan-btn'), install: $('install'), offline: $('offline'),
  historyWrap: $('history-wrap'), history: $('history'), clearHistory: $('clear-history'),
  detail: $('detail'), dName: $('d-name'), dImg: $('d-img'), dBarcode: $('d-barcode'),
  dPrice: $('d-price'), dUom: $('d-uom'), dCopy: $('d-copy'),
  scanner: $('scanner'), video: $('video'), scanMsg: $('scan-msg'),
  settings: $('settings'), settingsBtn: $('settings-btn'), apiUrl: $('api-url'),
  apiSave: $('api-save'), apiReset: $('api-reset'), version: $('version'),
};

/* ---------- storage ---------- */
const store = {
  get(key, fallback) {
    try { const v = localStorage.getItem(key); return v === null ? fallback : JSON.parse(v); } catch { return fallback; }
  },
  set(key, value) { try { localStorage.setItem(key, JSON.stringify(value)); } catch { /* storage penuh/diblokir */ } },
  remove(key) { try { localStorage.removeItem(key); } catch { /* ignore */ } },
};
const apiBase = () => String(store.get('apiBase', DEFAULT_API)).replace(/\/+$/, '');

/* ---------- util ---------- */
const rupiah = new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', maximumFractionDigits: 0 });
const formatPrice = (p) => (p === null || p === undefined ? '-' : rupiah.format(Number(p)));

function setStatus(text, isError = false) {
  els.status.textContent = text;
  els.status.classList.toggle('error', isError);
}

class NotFoundError extends Error {}

async function apiGet(path, params) {
  const url = new URL(apiBase() + path);
  Object.entries(params).forEach(([k, v]) => url.searchParams.set(k, v));
  const ctrl = new AbortController();
  const timer = setTimeout(() => ctrl.abort(), TIMEOUT_MS);
  try {
    const res = await fetch(url, { signal: ctrl.signal, headers: { Accept: 'application/json' } });
    if (res.status === 404) throw new NotFoundError();
    if (res.status === 429) {
      throw new Error('Terlalu banyak permintaan. Coba lagi sebentar lagi.');
    }
    if (res.status === 400) {
      let msg = 'Input tidak valid.';
      try { msg = (await res.json()).message || msg; } catch { /* ignore */ }
      throw new Error(msg);
    }
    if (!res.ok) throw new Error(`Server membalas ${res.status}.`);
    const text = await res.text();
    return { body: text ? JSON.parse(text) : null, headers: res.headers };
  } catch (e) {
    if (e.name === 'AbortError') throw new Error('Permintaan terlalu lama, coba lagi.');
    if (e instanceof TypeError) {
      throw new Error(navigator.onLine ? 'Tidak dapat menghubungi server API.' : 'Anda sedang offline dan data ini belum tersimpan.');
    }
    throw e;
  } finally {
    clearTimeout(timer);
  }
}

/* ---------- riwayat ---------- */
function renderHistory() {
  const items = store.get('history', []);
  els.history.replaceChildren(...items.map((q) => {
    const b = document.createElement('button');
    b.type = 'button'; b.className = 'chip'; b.textContent = q;
    b.addEventListener('click', () => { els.q.value = q; runSearch(q); });
    return b;
  }));
  els.historyWrap.hidden = items.length === 0;
}
function pushHistory(q) {
  const items = [q, ...store.get('history', []).filter((x) => x !== q)].slice(0, HISTORY_MAX);
  store.set('history', items);
  renderHistory();
}

/* ---------- hasil ---------- */
const state = { items: [], shown: 0, total: null, serverPaged: false, page: 0, q: '', token: 0 };

function cardFor(p) {
  const li = document.createElement('li');
  const btn = document.createElement('button');
  btn.type = 'button'; btn.className = 'card';
  const name = document.createElement('span'); name.className = 'name'; name.textContent = p.name ?? '(tanpa nama)';
  const meta = document.createElement('span'); meta.className = 'meta';
  const code = document.createElement('span'); code.className = 'code'; code.textContent = p.barcode ?? '-';
  const price = document.createElement('span'); price.textContent = `${formatPrice(p.price)}${p.uom ? ' / ' + p.uom : ''}`;
  meta.append(code, price);
  btn.append(name, meta);
  btn.addEventListener('click', () => openDetail(p));
  li.append(btn);
  return li;
}

function updateMore() {
  const hasMore = state.shown < state.items.length || (state.serverPaged && state.items.length < state.total);
  els.more.hidden = !hasMore;
  els.more.disabled = false;
}

function renderMore() {
  const next = state.items.slice(state.shown, state.shown + PAGE_SIZE);
  els.results.append(...next.map(cardFor));
  state.shown += next.length;
  const total = state.total ?? state.items.length;
  setStatus(`Menampilkan ${state.shown} dari ${total} produk`);
  updateMore();
}

function resetResults(q) {
  Object.assign(state, { items: [], shown: 0, total: null, serverPaged: false, page: 0, q, token: state.token + 1 });
  els.results.replaceChildren();
  els.more.hidden = true;
}

async function fetchNamePage(page, token) {
  const { body, headers } = await apiGet('/products', { name: state.q, page, size: PAGE_SIZE });
  if (token !== state.token) return false; // pencarian lain sudah dimulai
  const list = Array.isArray(body) ? body : [];
  const totalHeader = headers.get('X-Total-Count');
  if (totalHeader !== null) {
    state.serverPaged = true;
    state.total = Number(totalHeader);
  } else {
    state.total = list.length;
  }
  state.page = page;
  state.items.push(...list);
  return true;
}

async function runSearch(raw) {
  const q = raw.trim();
  if (!q) return;
  els.q.value = q;
  resetResults(q);
  const token = state.token;
  const isBarcode = /^\d{8,14}$/.test(q);
  if (!isBarcode && q.length < 2) { setStatus('Nama produk minimal 2 huruf.', true); return; }

  setStatus('Mencari…');
  try {
    if (isBarcode) {
      const { body } = await apiGet('/products-barcode', { barcode: q, generateBarcode: false });
      if (token !== state.token) return;
      if (!body) throw new NotFoundError();
      state.items = [body]; state.total = 1;
      renderMore();
      pushHistory(q);
      openDetail(body);
    } else {
      if (!(await fetchNamePage(0, token))) return;
      if (state.items.length === 0) { setStatus(`Tidak ada produk untuk “${q}”.`); return; }
      renderMore();
      pushHistory(q);
    }
  } catch (e) {
    if (token !== state.token) return;
    if (e instanceof NotFoundError) setStatus(`Produk “${q}” tidak ditemukan.`);
    else setStatus(e.message, true);
  }
}

els.more.addEventListener('click', async () => {
  els.more.disabled = true;
  const token = state.token;
  try {
    if (state.shown >= state.items.length && state.serverPaged) {
      if (!(await fetchNamePage(state.page + 1, token))) return;
    }
    renderMore();
  } catch (e) {
    setStatus(e.message, true);
    els.more.disabled = false;
  }
});

els.form.addEventListener('submit', (e) => { e.preventDefault(); runSearch(els.q.value); });
els.clearHistory.addEventListener('click', () => { store.remove('history'); renderHistory(); });

/* ---------- detail ---------- */
async function openDetail(p) {
  els.dName.textContent = p.name ?? '';
  els.dBarcode.textContent = p.barcode ?? '-';
  els.dPrice.textContent = formatPrice(p.price);
  els.dUom.textContent = p.uom ?? '-';
  els.dImg.hidden = true; els.dImg.removeAttribute('src');
  els.dCopy.textContent = 'Salin barcode';
  if (!els.detail.open) els.detail.showModal();

  if (!p.barcode) return;
  try {
    const { body } = await apiGet('/products-barcode', { barcode: p.barcode, generateBarcode: true });
    if (els.dBarcode.textContent === p.barcode && body && body.imgBarcode) {
      els.dImg.src = `data:image/png;base64,${body.imgBarcode}`;
      els.dImg.hidden = false;
    }
  } catch { /* gambar barcode opsional */ }
}

els.dCopy.addEventListener('click', async () => {
  try {
    await navigator.clipboard.writeText(els.dBarcode.textContent);
    els.dCopy.textContent = 'Tersalin ✓';
  } catch { els.dCopy.textContent = 'Gagal menyalin'; }
});

/* ---------- scanner ---------- */
let scanStream = null;
let scanning = false;

function stopScan() {
  scanning = false;
  if (scanStream) { scanStream.getTracks().forEach((t) => t.stop()); scanStream = null; }
  els.video.srcObject = null;
}

async function startScan() {
  els.scanMsg.textContent = 'Menyalakan kamera…';
  els.scanner.showModal();
  try {
    const detector = new BarcodeDetector({ formats: ['ean_13', 'ean_8', 'upc_a', 'upc_e'] });
    scanStream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: { ideal: 'environment' } }, audio: false });
    els.video.srcObject = scanStream;
    await els.video.play();
    els.scanMsg.textContent = 'Posisikan barcode di dalam bingkai.';
    scanning = true;
    while (scanning) {
      try {
        const codes = await detector.detect(els.video);
        if (codes.length) {
          const value = codes[0].rawValue;
          stopScan();
          els.scanner.close();
          runSearch(value);
          return;
        }
      } catch { /* frame belum siap */ }
      await new Promise((r) => setTimeout(r, 200));
    }
  } catch (e) {
    stopScan();
    els.scanMsg.textContent = e && e.name === 'NotAllowedError'
      ? 'Izin kamera ditolak. Aktifkan di pengaturan browser.'
      : 'Kamera tidak dapat dipakai di perangkat ini.';
  }
}

els.scanner.addEventListener('close', stopScan);
if ('BarcodeDetector' in window && navigator.mediaDevices && navigator.mediaDevices.getUserMedia) {
  els.scanBtn.hidden = false;
  els.scanBtn.addEventListener('click', startScan);
}

/* ---------- pengaturan ---------- */
els.settingsBtn.addEventListener('click', () => { els.apiUrl.value = apiBase(); els.settings.showModal(); });
els.apiSave.addEventListener('click', (e) => {
  e.preventDefault();
  try {
    const u = new URL(els.apiUrl.value.trim());
    if (!/^https?:$/.test(u.protocol)) throw new Error('protokol');
    store.set('apiBase', u.href.replace(/\/+$/, ''));
    els.settings.close();
    setStatus('URL API disimpan.');
  } catch { els.apiUrl.setCustomValidity('URL tidak valid'); els.apiUrl.reportValidity(); els.apiUrl.setCustomValidity(''); }
});
els.apiReset.addEventListener('click', (e) => {
  e.preventDefault(); store.remove('apiBase'); els.apiUrl.value = DEFAULT_API;
});

/* ---------- online/offline + install + SW ---------- */
const syncOnline = () => { els.offline.hidden = navigator.onLine; };
addEventListener('online', syncOnline);
addEventListener('offline', syncOnline);
syncOnline();

let deferredInstall = null;
addEventListener('beforeinstallprompt', (e) => { e.preventDefault(); deferredInstall = e; els.install.hidden = false; });
els.install.addEventListener('click', async () => {
  if (!deferredInstall) return;
  deferredInstall.prompt();
  await deferredInstall.userChoice;
  deferredInstall = null; els.install.hidden = true;
});
addEventListener('appinstalled', () => { els.install.hidden = true; });

if ('serviceWorker' in navigator) {
  addEventListener('load', async () => {
    try {
      const reg = await navigator.serviceWorker.register('./sw.js');
      navigator.serviceWorker.addEventListener('message', (e) => {
        if (e.data && e.data.type === 'VERSION') els.version.textContent = `Versi aplikasi ${e.data.version}`;
      });
      (reg.active || navigator.serviceWorker.controller)?.postMessage('GET_VERSION');
    } catch { /* SW tidak tersedia (mis. http biasa) */ }
  });
}

/* ---------- init ---------- */
renderHistory();
const params = new URLSearchParams(location.search);
if (params.get('action') === 'scan' && !els.scanBtn.hidden) startScan();
else if (params.get('q')) runSearch(params.get('q'));
