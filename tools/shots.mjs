// Captures d'écran pleine page + mesures de mise en page, et comparaison avant/après.
//
//   node shots.mjs                               → copie locale, 8 pages × 375/768/1280 → tools/shots/<date>-local/
//   node shots.mjs --url=https://rtpcalculator.com → production (GA4/AdSense bloqués : aucune session comptée)
//   node shots.mjs --pages=/fr/,/fr/app/ --widths=375,1280 --label=lot1
//   node shots.mjs --cmp                         → garde la bannière de consentement visible
//   node shots.mjs --compare <dossierA> <dossierB> → planches côte à côte + différence
//
// Chaque dossier contient report.json et report.md : défilement horizontal, boutons sur
// plusieurs lignes, entrées de menu hors écran, abscisse des H2, tailles < 12 px.
import { DEFAULT_PAGES, DEFAULT_WIDTHS, SHOTS_DIR, parseArgs, slug, timestamp } from './lib/env.mjs';
import fs from 'node:fs';
import path from 'node:path';
import { startServer } from './serve.mjs';

const args = parseArgs(process.argv.slice(2));

if (args.compare) { await compare(args._[0], args._[1]); process.exit(0); }

const { chromium } = await import('playwright');
const pages = args.pages ? String(args.pages).split(',') : DEFAULT_PAGES;
const widths = args.widths ? String(args.widths).split(',').map(Number) : DEFAULT_WIDTHS;

let base = args.url, server = null;
if (!base) { ({ server, url: base } = await startServer()); }
const isLocal = /localhost|127\.0\.0\.1/.test(base);
const label = args.label || (isLocal ? 'local' : 'prod');
const outDir = path.join(SHOTS_DIR, `${timestamp()}-${label}`);
fs.mkdirSync(outDir, { recursive: true });

const BLOCKED = /googletagmanager\.com|google-analytics\.com|googlesyndication\.com|doubleclick\.net|googleadservices\.com/;
const browser = await chromium.launch();
const report = [];

for (const width of widths) {
  const ctx = await browser.newContext({
    viewport: { width, height: width < 600 ? 740 : 900 },
    deviceScaleFactor: width < 600 ? 2 : 1,
    isMobile: width < 600, hasTouch: width < 600,
    locale: 'fr-FR', colorScheme: 'dark',
  });
  await ctx.route(/.*/, (route) => BLOCKED.test(route.request().url()) ? route.abort() : route.continue());
  if (!args.cmp) {
    await ctx.addInitScript(() => {
      try { localStorage.setItem('rtp_consent_v1', JSON.stringify({ analytics: false, ads: false, ts: Date.now() })); } catch (e) {}
    });
  }
  for (const p of pages) {
    const page = await ctx.newPage();
    const url = base.replace(/\/$/, '') + p;
    const resp = await page.goto(url, { waitUntil: 'networkidle', timeout: 60000 }).catch((e) => ({ error: e.message }));
    if (!resp || resp.error || !resp.ok()) {
      report.push({ page: p, width, error: resp?.error || `HTTP ${resp?.status()}` });
      console.error(`✗ ${p} @${width} : ${resp?.error || resp?.status()}`);
      await page.close(); continue;
    }
    await page.evaluate(() => document.fonts.ready);
    await page.waitForTimeout(250);
    const m = await page.evaluate(measure);
    const file = `${slug(p)}-${width}.png`;
    await page.screenshot({ path: path.join(outDir, file), fullPage: true });
    report.push({ page: p, width, file, ...m });
    console.log(`✓ ${p} @${width} → ${file}${m.overflowX ? `  ⚠ débordement horizontal ${m.scrollWidth}px` : ''}`);
    await page.close();
  }
  await ctx.close();
}
await browser.close();
if (server) server.close();

fs.writeFileSync(path.join(outDir, 'report.json'), JSON.stringify({ base, date: new Date().toISOString(), report }, null, 2));
fs.writeFileSync(path.join(outDir, 'report.md'), renderReport(base, report));
console.log(`\nDossier : ${outDir}\n`);
console.log(renderReport(base, report));

/* ---------- mesures exécutées dans la page ---------- */
function measure() {
  const vw = document.documentElement.clientWidth;
  const sw = document.documentElement.scrollWidth;
  // Nombre de lignes de texte : rectangles des nœuds texte seulement (les icônes SVG sont ignorées).
  const lines = (el) => {
    const tops = new Set();
    const walker = document.createTreeWalker(el, NodeFilter.SHOW_TEXT);
    for (let n = walker.nextNode(); n; n = walker.nextNode()) {
      if (!n.textContent.trim()) continue;
      const r = document.createRange(); r.selectNodeContents(n);
      for (const x of r.getClientRects()) if (x.width > 0) tops.add(Math.round(x.top / 4));
    }
    return tops.size;
  };
  const txt = (el) => (el.textContent || '').trim().replace(/\s+/g, ' ').slice(0, 50);
  const buttons = [...document.querySelectorAll('.btn, .play-btn, .cta a, .btn-save, .add-btn, #rtp-cmp button')]
    .map((el) => ({ text: txt(el), lines: lines(el), w: Math.round(el.getBoundingClientRect().width), h: Math.round(el.getBoundingClientRect().height),
      textAlign: getComputedStyle(el).textAlign, overflow: el.scrollHeight > el.clientHeight + 1 }))
    .filter((b) => b.lines > 1 || b.overflow);
  const nav = document.querySelector('.nav-links');
  let navHidden = [];
  if (nav) {
    const nr = nav.getBoundingClientRect();
    navHidden = [...nav.querySelectorAll(':scope > a, :scope > .nav-dd > a')]
      .filter((a) => a.getBoundingClientRect().right > nr.right + 1 || a.getBoundingClientRect().left < nr.left - 1)
      .map(txt);
  }
  const h2 = [...document.querySelectorAll('main h2, main h1')].map((el) => ({ tag: el.tagName, text: txt(el), x: Math.round(el.getBoundingClientRect().left) }));
  const brand = document.querySelector('.brand'); const main = document.querySelector('main .wrap, main.wrap, main > .wrap');
  const edges = { brandX: brand ? Math.round(brand.getBoundingClientRect().left) : null, contentX: main ? Math.round(main.getBoundingClientRect().left + parseFloat(getComputedStyle(main).paddingLeft)) : null };
  const small = {};
  for (const el of document.querySelectorAll('body *')) {
    if (!el.childNodes.length || !el.innerText || !el.innerText.trim()) continue;
    const fs = parseFloat(getComputedStyle(el).fontSize);
    if (fs < 12 && [...el.childNodes].some((n) => n.nodeType === 3 && n.textContent.trim())) {
      const k = `${fs}px ${el.className ? '.' + String(el.className).split(' ')[0] : el.tagName.toLowerCase()}`;
      small[k] = (small[k] || 0) + 1;
    }
  }
  const header = document.querySelector('header.site');
  return { viewport: vw, scrollWidth: sw, overflowX: sw > vw, pageHeight: document.documentElement.scrollHeight,
    headerHeight: header ? Math.round(header.getBoundingClientRect().height) : null, sticky: header ? getComputedStyle(header).position : null,
    buttonsMultiline: buttons, navHidden, headings: h2, edges, smallText: small };
}

function renderReport(base, rep) {
  const L = [`# Captures — ${base}`, ''];
  L.push('| Page | Larg. | Déford. horiz. | En-tête | Menu hors écran | Boutons > 1 ligne | Logo / contenu (x) | Texte < 12 px |', '|---|---|---|---|---|---|---|---|');
  for (const r of rep) {
    if (r.error) { L.push(`| ${r.page} | ${r.width} | erreur : ${r.error} | | | | | |`); continue; }
    L.push(`| ${r.page} | ${r.width} | ${r.overflowX ? `**oui** (${r.scrollWidth})` : 'non'} | ${r.headerHeight} px ${r.sticky === 'sticky' ? 'collant' : ''} | ${r.navHidden.length ? '**' + r.navHidden.join(', ') + '**' : '—'} | ${r.buttonsMultiline.length ? '**' + r.buttonsMultiline.map((b) => `« ${b.text} » (${b.lines} l., ${b.textAlign})`).join(' ; ') + '**' : '—'} | ${r.edges.brandX} / ${r.edges.contentX} | ${Object.entries(r.smallText).map(([k, v]) => `${k} ×${v}`).join(', ') || '—'} |`);
  }
  L.push('', '## Abscisse des titres (alignement des sections)', '');
  for (const r of rep) if (!r.error && r.headings.length > 1) L.push(`- ${r.page} @${r.width} : ${r.headings.map((h) => `${h.tag} « ${h.text} » x=${h.x}`).join(' · ')}`);
  return L.join('\n') + '\n';
}

/* ---------- comparaison avant / après ---------- */
async function compare(dirA, dirB) {
  if (!dirA || !dirB) { console.error('Usage : node shots.mjs --compare <dossierA> <dossierB>'); process.exit(2); }
  const { PNG } = await import('pngjs');
  const pixelmatch = (await import('pixelmatch')).default;
  const out = path.join(SHOTS_DIR, `${timestamp()}-compare`);
  fs.mkdirSync(out, { recursive: true });
  const names = fs.readdirSync(dirA).filter((f) => f.endsWith('.png') && fs.existsSync(path.join(dirB, f)));
  const rows = [];
  for (const f of names) {
    const a = PNG.sync.read(fs.readFileSync(path.join(dirA, f)));
    const b = PNG.sync.read(fs.readFileSync(path.join(dirB, f)));
    const W = Math.max(a.width, b.width), H = Math.max(a.height, b.height);
    const pad = (img) => { const p = new PNG({ width: W, height: H, fill: true }); p.data.fill(0); for (let i = 0; i < p.data.length; i += 4) p.data[i + 3] = 255; PNG.bitblt(img, p, 0, 0, img.width, img.height, 0, 0); return p; };
    const A = pad(a), B = pad(b), D = new PNG({ width: W, height: H });
    const diff = pixelmatch(A.data, B.data, D.data, W, H, { threshold: 0.1, alpha: 0.35, diffColor: [255, 0, 90] });
    const gap = 16, sheet = new PNG({ width: W * 3 + gap * 2, height: H });
    for (let i = 0; i < sheet.data.length; i += 4) { sheet.data[i] = 60; sheet.data[i + 1] = 60; sheet.data[i + 2] = 60; sheet.data[i + 3] = 255; }
    PNG.bitblt(A, sheet, 0, 0, W, H, 0, 0); PNG.bitblt(B, sheet, 0, 0, W, H, W + gap, 0); PNG.bitblt(D, sheet, 0, 0, W, H, (W + gap) * 2, 0);
    fs.writeFileSync(path.join(out, f), PNG.sync.write(sheet));
    rows.push({ f, diff, pct: (100 * diff / (W * H)).toFixed(2), hA: a.height, hB: b.height });
    console.log(`${f} : ${diff} px différents (${rows.at(-1).pct} %), hauteur ${a.height} → ${b.height}`);
  }
  fs.writeFileSync(path.join(out, 'compare.json'), JSON.stringify({ dirA, dirB, rows }, null, 2));
  console.log(`\nPlanches (avant | après | différence) : ${out}`);
}
