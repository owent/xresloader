// Rasterize the editable SVG/HTML sources using an installed Chrome or Edge.
// Requires Node.js 22+; no npm dependencies and no image generation API calls.
import { spawn } from 'node:child_process';
import { access, mkdir, mkdtemp, readFile, writeFile } from 'node:fs/promises';
import { join } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';
import { setTimeout as delay } from 'node:timers/promises';

const root = fileURLToPath(new URL('../../doc/brand/', import.meta.url));
const cache = join(root, '.preview-cache');
await mkdir(cache, { recursive: true });
const candidates = [process.env.BRAND_BROWSER,
  'C:/Program Files/Google/Chrome/Application/chrome.exe',
  'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',
  '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',
  '/usr/bin/chromium', '/usr/bin/chromium-browser', '/usr/bin/google-chrome'];
let executable;
for (const candidate of candidates.filter(Boolean)) {
  try { await access(candidate); executable = candidate; break; } catch { /* Try the next installed browser. */ }
}
if (!executable) throw new Error('Set BRAND_BROWSER to the absolute path of Chrome, Chromium, or Edge.');
await access(join(root, 'hero-art.png'));
const profile = await mkdtemp(join(cache, 'browser-'));
const browser = spawn(executable, [
  '--headless=new', '--disable-gpu', '--no-first-run', '--no-default-browser-check',
  '--disable-background-networking', '--disable-component-update', '--disable-extensions',
  '--disable-sync', '--remote-debugging-address=127.0.0.1', '--remote-debugging-port=0',
  `--user-data-dir=${profile}`, 'about:blank',
], { stdio: 'ignore', windowsHide: true });
let launchError;
browser.on('error', error => { launchError = error; });
let socket;
let nextId = 0;
const pending = new Map();
function command(method, params = {}, sessionId) {
  return new Promise((resolve, reject) => {
    const id = ++nextId;
    const timer = setTimeout(() => { pending.delete(id); reject(new Error(`${method} timed out`)); }, 20000);
    pending.set(id, { resolve, reject, timer });
    socket.send(JSON.stringify({ id, method, params, ...(sessionId ? { sessionId } : {}) }));
  });
}
try {
  let port;
  for (let attempt = 0; attempt < 100; attempt++) {
    if (launchError) throw launchError;
    if (browser.exitCode !== null) throw new Error(`Browser exited with code ${browser.exitCode}`);
    try { port = Number((await readFile(join(profile, 'DevToolsActivePort'), 'utf8')).split('\n')[0]); break; }
    catch { await delay(100); }
  }
  if (!port) throw new Error('Browser startup timed out.');
  const version = await (await fetch(`http://127.0.0.1:${port}/json/version`, { signal: AbortSignal.timeout(10000) })).json();
  socket = new WebSocket(version.webSocketDebuggerUrl);
  await new Promise((resolve, reject) => {
    socket.addEventListener('open', resolve, { once: true });
    socket.addEventListener('error', reject, { once: true });
  });
  socket.addEventListener('message', event => {
    const result = JSON.parse(event.data);
    const item = pending.get(result.id);
    if (!item) return;
    clearTimeout(item.timer);
    pending.delete(result.id);
    if (result.error) item.reject(new Error(JSON.stringify(result.error)));
    else item.resolve(result.result);
  });
  const { targetId } = await command('Target.createTarget', { url: 'about:blank' });
  const { sessionId } = await command('Target.attachToTarget', { targetId, flatten: true });
  const send = (method, params) => command(method, params, sessionId);
  await send('Page.enable');
  await send('Emulation.setEmulatedMedia', { features: [{ name: 'prefers-color-scheme', value: 'light' }] });
  async function render(source, output, width, height, { svg = false, background = null, full = false } = {}) {
    let url = pathToFileURL(join(root, source)).href;
    if (svg) {
      const wrapper = join(cache, 'render.html');
      await writeFile(wrapper, `<!doctype html><meta charset="utf-8"><style>html,body{margin:0;width:100%;height:100%;overflow:hidden;background:${background ?? 'transparent'}}img{display:block;width:100%;height:100%}</style><img src="${url}" alt="">`);
      url = pathToFileURL(wrapper).href;
    }
    await send('Emulation.setDeviceMetricsOverride', { width, height, deviceScaleFactor: 1, mobile: false });
    await send('Emulation.setDefaultBackgroundColorOverride', { color: { r: 0, g: 0, b: 0, a: 0 } });
    // Navigate through a blank document so a prior page cannot satisfy readiness.
    await send('Page.navigate', { url: 'about:blank' });
    const navigation = await send('Page.navigate', { url });
    if (navigation.errorText) throw new Error(navigation.errorText);
    let ready = false;
    for (let attempt = 0; attempt < 100; attempt++) {
      const result = await send('Runtime.evaluate', { expression: `location.href === ${JSON.stringify(url)} && document.readyState === 'complete'`, returnByValue: true });
      if (result.result.value) { ready = true; break; }
      await delay(50);
    }
    if (!ready) throw new Error(`Page did not load: ${source}`);
    const check = await send('Runtime.evaluate', {
      expression: `(async () => { await document.fonts.ready; await Promise.all([...document.images].map(i => i.decode())); return { overflow: document.documentElement.scrollWidth > innerWidth, images: document.images.length }; })()`,
      awaitPromise: true, returnByValue: true,
    });
    if (check.exceptionDetails) throw new Error(`Image/font loading failed for ${source}: ${JSON.stringify(check.exceptionDetails)}`);
    if (check.result.value.overflow) throw new Error(`Horizontal overflow in ${source} at ${width}px`);
    if (source === 'index.html') {
      const banners = await send('Runtime.evaluate', { expression: `[...document.querySelectorAll('.banner')].filter(i => getComputedStyle(i).display !== 'none').length`, returnByValue: true });
      if (banners.result.value !== 1) throw new Error('Preview must show exactly one theme banner');
    }
    if (full) {
      const layout = await send('Page.getLayoutMetrics');
      height = Math.ceil(layout.cssContentSize.height);
    }
    const screenshot = await send('Page.captureScreenshot', {
      format: 'png', captureBeyondViewport: true,
      clip: { x: 0, y: 0, width, height, scale: 1 },
    });
    const png = Buffer.from(screenshot.data, 'base64');
    if (png.readUInt32BE(16) !== width || png.readUInt32BE(20) !== height) throw new Error(`Incorrect PNG dimensions: ${output}`);
    await writeFile(join(root, output), png);
    console.log(`${output}: ${width} x ${height}`);
    return png;
  }
  const icons = new Map();
  for (const size of [16, 32, 48, 192, 256, 512, 1024]) {
    icons.set(size, await render('icon.svg', `icon-${size}.png`, size, size, { svg: true }));
  }
  await render('icon.svg', 'apple-touch-icon.png', 180, 180, { svg: true, background: '#102B32' });
  for (const theme of ['light', 'dark']) {
    await render(`logo-${theme}.svg`, `logo-${theme}.png`, 1060, 256, { svg: true });
    await render(`readme-${theme}.svg`, `readme-${theme}.png`, 1280, 400, { svg: true });
  }
  await render('social-card.html', 'social-preview.png', 1280, 640);
  // ICO supports PNG payloads; retain independently rendered small sizes.
  const sizes = [16, 32, 48, 256];
  const header = Buffer.alloc(6 + sizes.length * 16);
  header.writeUInt16LE(1, 2);
  header.writeUInt16LE(sizes.length, 4);
  let offset = header.length;
  sizes.forEach((size, index) => {
    const pos = 6 + index * 16;
    const png = icons.get(size);
    header[pos] = header[pos + 1] = size === 256 ? 0 : size;
    header.writeUInt16LE(1, pos + 4);
    header.writeUInt16LE(32, pos + 6);
    header.writeUInt32LE(png.length, pos + 8);
    header.writeUInt32LE(offset, pos + 12);
    offset += png.length;
  });
  await writeFile(join(root, 'favicon.ico'), Buffer.concat([header, ...sizes.map(size => icons.get(size))]));
  console.log('favicon.ico: 16, 32, 48, 256 px');
  await render('index.html', '.preview-cache/preview-wide.png', 1440, 1000, { full: true });
  await render('index.html', '.preview-cache/preview-mobile.png', 390, 844, { full: true });
  await send('Runtime.evaluate', { expression: `document.querySelector('[data-theme-choice="dark"]').click()` });
  const themeCheck = await send('Runtime.evaluate', { expression: `document.documentElement.dataset.theme === 'dark' && getComputedStyle(document.body).backgroundColor === 'rgb(16, 43, 50)' && [...document.querySelectorAll('.banner')].filter(i => getComputedStyle(i).display !== 'none').length === 1`, returnByValue: true });
  if (!themeCheck.result.value) throw new Error('Preview theme control failed');
  await send('Emulation.setEmulatedMedia', { features: [{ name: 'prefers-color-scheme', value: 'dark' }] });
  await render('index.html', '.preview-cache/preview-wide-dark.png', 1440, 1000, { full: true });
  await render('index.html', '.preview-cache/preview-mobile-dark.png', 390, 844, { full: true });
  console.log('Preview: images loaded, no horizontal overflow at 1440 / 390 px, theme toggle passed.');
} finally {
  if (socket?.readyState === WebSocket.OPEN) {
    try { await command('Browser.close'); } catch { /* Process fallback below. */ }
    socket.close();
  }
  if (browser.exitCode === null) browser.kill();
  for (const { reject, timer } of pending.values()) { clearTimeout(timer); reject(new Error('Browser closed')); }
}
