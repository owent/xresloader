// Native vector sources for the xresloader identity. Run from any directory.
import { mkdir, writeFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';

const output = fileURLToPath(new URL('../../doc/brand/', import.meta.url));
await mkdir(output, { recursive: true });
const c = { ink: '#102B32', paper: '#F4F7F3', mint: '#90EDC2', teal: '#168A79', muted: '#526A6C', line: '#D8E3DD' };

// A continuous rising ribbon, an outgoing ribbon, and a detached table cell.
// Keeping the three pieces distinct preserves the silhouette at 16 px.
function mark(primary = c.ink, secondary = c.teal, cell = c.teal) {
  return `<path fill="${primary}" d="M24 84 84 24h16a4 4 0 0 1 4 4v16l-60 60H28a4 4 0 0 1-4-4Z"/>
  <path fill="${secondary}" d="m60 48 44 44v8a4 4 0 0 1-4 4H84L48 68Z"/>
  <rect x="24" y="24" width="22" height="22" rx="4" fill="${cell}"/>`;
}

// Original geometric lettering, drawn as paths; no font files or remote fonts.
const letters = {
  x: [35, 'M3 13 30 49M30 13 3 49'],
  r: [28, 'M5 49V14M5 26Q9 12 23 14'],
  e: [36, 'M5 30H31Q31 13 18 13C0 13 0 49 18 49Q26 49 31 44'],
  s: [33, 'M29 17Q24 13 17 13C0 13 0 29 17 31C36 33 34 49 17 49Q8 49 3 44'],
  l: [16, 'M6 1V42Q6 49 12 49'],
  o: [37, 'M19 13C-1 13-1 49 19 49C39 49 39 13 19 13Z'],
  a: [37, 'M31 14V49M31 25C31 9 4 9 4 31C4 53 31 53 31 37'],
  d: [37, 'M31 1V49M31 25C31 9 4 9 4 31C4 53 31 53 31 37'],
};
function wordmark(color, x = 0, y = 0, scale = 1) {
  let cursor = 0;
  const paths = [...'xresloader'].map(letter => {
    const [width, path] = letters[letter];
    const svg = `<path transform="translate(${cursor} 0)" d="${path}"/>`;
    cursor += width + 7;
    return svg;
  }).join('');
  return `<g transform="translate(${x} ${y}) scale(${scale})" fill="none" stroke="${color}" stroke-width="5.5" stroke-linecap="round" stroke-linejoin="round">${paths}</g>`;
}
function svg(width, height, title, description, body) {
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" role="img" aria-labelledby="title desc">
<title id="title">${title}</title>
<desc id="desc">${description}</desc>
${body}
</svg>\n`;
}
async function save(name, body) { await writeFile(`${output}/${name}`, body, 'utf8'); }
const desc = 'A detached table cell and two crossing ribbons form an X, representing structured data conversion.';
await save('mark.svg', svg(128, 128, 'xresloader mark', desc, mark()));
await save('mark-inverse.svg', svg(128, 128, 'xresloader mark for dark backgrounds', desc, mark(c.mint, '#3CB99A', c.paper)));
await save('mark-mono.svg', svg(128, 128, 'xresloader monochrome mark', desc, mark('currentColor', 'currentColor', 'currentColor')));
const tile = `<rect width="128" height="128" rx="28" fill="${c.ink}"/>${mark(c.mint, '#3CB99A', c.paper)}`;
await save('icon.svg', svg(128, 128, 'xresloader app icon', desc, tile));
await save('favicon.svg', svg(128, 128, 'xresloader', desc, tile));
for (const [theme, ink, primary, secondary, cell] of [
  ['light', c.ink, c.ink, c.teal, c.teal],
  ['dark', c.paper, c.mint, '#3CB99A', c.paper],
]) {
  await save(`logo-${theme}.svg`, svg(530, 128, 'xresloader', desc, `${mark(primary, secondary, cell)}${wordmark(ink, 142, 38, .96)}`));
}

function sheet(x, y, stroke) {
  return `<g transform="translate(${x} ${y})" fill="none" stroke="${stroke}" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect width="32" height="36" rx="5"/><path d="M0 11h32M0 23h32M11 11v25"/></g>`;
}
for (const dark of [false, true]) {
  const bg = dark ? c.ink : c.paper;
  const fg = dark ? c.paper : c.ink;
  const accent = dark ? c.mint : c.teal;
  const muted = dark ? '#AAC3BE' : c.muted;
  const line = dark ? '#36524F' : c.line;
  const body = `<rect width="1280" height="400" rx="24" fill="${bg}"/>
  <path d="M800 0v400M840 0v400M880 0v400M920 0v400M960 0v400M1000 0v400M1040 0v400M1080 0v400M1120 0v400M1160 0v400M1200 0v400M1240 0v400M800 40h480M800 80h480M800 120h480M800 160h480M800 200h480M800 240h480M800 280h480M800 320h480M800 360h480" fill="none" stroke="${line}" opacity=".45"/>
  <g transform="translate(42 44) scale(.72)">${mark(dark ? c.mint : c.ink, dark ? '#3CB99A' : c.teal, accent)}</g>
  ${wordmark(fg, 152, 70, 1.13)}
  <g font-family="Segoe UI,Arial,sans-serif">
    <text x="62" y="211" fill="${fg}" font-size="39" font-weight="600" letter-spacing="-1">From tables to game data.</text>
    <text x="64" y="253" fill="${muted}" font-size="20">Schema-driven. Multi-format. Built for your pipeline.</text>
    <text x="64" y="338" fill="${accent}" font-size="15" letter-spacing="2">EXCEL</text>
    <path d="M132 332h35m-6-5 6 5-6 5" fill="none" stroke="${accent}" stroke-width="1.5"/>
    <text x="187" y="338" fill="${muted}" font-size="16">Protobuf · MessagePack · Lua · JavaScript · JSON · XML</text>
  </g>
  <rect x="906" y="82" width="212" height="236" rx="22" fill="${dark ? '#1C3B3E' : '#E8F0E9'}" stroke="${line}"/>
  <g transform="translate(914 96) scale(1.5)">${mark(dark ? c.mint : c.ink, dark ? '#3CB99A' : c.teal, accent)}</g>
  <rect x="860" y="250" width="70" height="78" rx="14" fill="${bg}" stroke="${line}"/>
  ${sheet(879, 271, accent)}
  <rect x="1090" y="70" width="64" height="64" rx="14" fill="${accent}"/>
  <path d="m1113 91-8 11 8 11m18-22 8 11-8 11" fill="none" stroke="${bg}" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>`;
  await save(`readme-${dark ? 'dark' : 'light'}.svg`, svg(1280, 400, 'xresloader — From tables to game data.', 'Schema-driven spreadsheet conversion to Protobuf, MessagePack, Lua, JavaScript, JSON and XML.', body));
}

const featureIcons = {
  spreadsheet: '<rect x="3" y="3" width="18" height="18" rx="3"/><path d="M3 9h18M3 15h18M9 9v12"/>',
  schema: '<rect x="8" y="3" width="8" height="5" rx="1.5"/><rect x="2" y="16" width="7" height="5" rx="1.5"/><rect x="15" y="16" width="7" height="5" rx="1.5"/><path d="M12 8v4M5.5 16v-4h13v4"/>',
  validation: '<path d="m12 2 8 3v6c0 5-4 8-8 11-4-3-8-6-8-11V5Z"/><path d="m8 12 3 3 5-6"/>',
  export: '<path d="M4 12h13m-4-4 4 4-4 4M4 5V3h16v18H4v-2"/>',
};
await mkdir(`${output}/features`, { recursive: true });
for (const [name, paths] of Object.entries(featureIcons)) {
  await save(`features/${name}.svg`, svg(24, 24, `xresloader ${name}`, `24 pixel ${name} icon.`, `<g fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">${paths}</g>`));
}
console.log(`Wrote 13 SVG assets to ${output}`);
