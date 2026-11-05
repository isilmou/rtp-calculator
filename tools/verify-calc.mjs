// Relevé des styles calculés du calculateur (formulaire, historique, toast) et de la carte démo de l'accueil,
// pour prouver qu'une retouche de site.css ne les a pas déplacés ni recolorés.
//
//   node verify-calc.mjs --out=<fichier.json> [--theme=dark] [--langs=fr,en] [--widths=375,1280]
//   node verify-calc.mjs --compare <A.json> <B.json> [--geometry]
//
// État relevé : historique à deux analyses (une normale, un surebet), formulaire rempli (2,00 / 1,85).
// --geometry ne compare que les dimensions et la typographie (comparaison entre deux thèmes).
import { parseArgs } from './lib/env.mjs';
import fs from 'node:fs';
import { startServer } from './serve.mjs';

const args = parseArgs(process.argv.slice(2));

const BOX = ['display', 'fontSize', 'fontWeight', 'fontFamily', 'lineHeight', 'letterSpacing', 'textAlign',
  'paddingTop', 'paddingRight', 'paddingBottom', 'paddingLeft', 'marginTop', 'marginRight', 'marginBottom', 'marginLeft',
  'borderTopWidth', 'borderRightWidth', 'borderBottomWidth', 'borderLeftWidth', 'borderTopLeftRadius', 'visibility'];
const PAINT = ['color', 'backgroundColor', 'borderTopColor', 'borderRightColor', 'borderBottomColor', 'borderLeftColor',
  'outlineStyle', 'outlineWidth', 'boxShadow', 'opacity', 'colorScheme', 'textDecorationLine', 'textDecorationColor', 'fill', 'stroke'];

if (args.compare) { compare(args._[0], args._[1]); process.exit(0); }
if (!args.out) { console.error('Usage : node verify-calc.mjs --out=<fichier.json> [--theme=dark] | --compare <A> <B> [--geometry]'); process.exit(2); }

const { chromium } = await import('playwright');
const langs = args.langs ? String(args.langs).split(',') : ['fr', 'en', 'es', 'de', 'pt'];
const widths = args.widths ? String(args.widths).split(',').map(Number) : [375, 1280];
const theme = args.theme === 'dark' ? 'dark' : '';
const prefix = (l) => (l === 'en' ? '' : '/' + l);

const HISTORY = [
  { id: 'a1', date: '2026-07-04', time: '23:00', eventName: 'Paraguay - France', betType: '1X2', savedAt: 1,
    outcomes: [['Paraguay', 'Pinnacle', '18.00'], ['X', 'Bet365', '8.20'], ['France', 'Betfair', '1.19']] },
  { id: 'a2', date: '2026-07-05', time: '18:00', eventName: 'Surebet', betType: '', savedAt: 2,
    outcomes: [['A', '', '2.10'], ['B', '', '2.10']] },
].map((a) => ({ ...a, outcomes: a.outcomes.map(([label, bookmaker, d], i) => ({ id: a.id + i, label, bookmaker, format: 'EU',
  decimalRaw: d, fracN: '', fracD: '', usNeg: false, usVal: '', refDecimal: parseFloat(d) })) }));

const { server, url: base } = await startServer();
const BLOCKED = /googletagmanager\.com|google-analytics\.com|googlesyndication\.com|doubleclick\.net|googleadservices\.com/;
const browser = await chromium.launch();
const dump = {};
for (const width of widths) {
  const ctx = await browser.newContext({ viewport: { width, height: width < 600 ? 740 : 900 }, isMobile: width < 600, hasTouch: width < 600, locale: 'fr-FR', colorScheme: 'dark' });
  await ctx.route(/.*/, (r) => BLOCKED.test(r.request().url()) ? r.abort() : r.continue());
  await ctx.addInitScript(([history, th]) => {
    try {
      localStorage.setItem('rtp_consent_v1', JSON.stringify({ analytics: false, ads: false, ts: Date.now() }));
      localStorage.setItem('rtp_web_history_v1', JSON.stringify(history));
      localStorage.setItem('rtp_last_fmt', 'EU');
      if (th) localStorage.setItem('rtp_theme', th); else localStorage.removeItem('rtp_theme');
    } catch (e) {}
  }, [HISTORY, theme]);
  for (const lang of langs) {
    for (const [kind, p, roots] of [['app', prefix(lang) + '/app/', ['.calc', '#toast']], ['home', prefix(lang) + '/', ['.demo-card']]]) {
      const page = await ctx.newPage();
      await page.goto(base + p, { waitUntil: 'networkidle', timeout: 60000 });
      await page.evaluate(() => document.fonts.ready);
      if (kind === 'app') {
        const odds = page.locator('.odds-zone .fld');
        await odds.nth(0).fill('2.00'); await odds.nth(1).fill('1.85');
        await page.locator('#f-event').fill('Test');
        await page.locator('#f-event').blur();
      }
      await page.waitForTimeout(150);
      dump[`${lang} ${kind} @${width}`] = await page.evaluate(([roots, props]) => {
        const out = [];
        for (const sel of roots) {
          const root = document.querySelector(sel);
          if (!root) { out.push({ key: sel + ' ABSENT' }); continue; }
          [root, ...root.querySelectorAll('*')].forEach((el, i) => {
            const cs = getComputedStyle(el), r = el.getBoundingClientRect();
            const cls = typeof el.className === 'string' ? el.className : (el.className.baseVal || '');
            const row = { key: `${sel} #${i} ${el.tagName.toLowerCase()}${el.id ? '#' + el.id : ''}${cls ? '.' + cls.trim().split(/\s+/).join('.') : ''}`,
              rect: [r.x, r.y, r.width, r.height].map((v) => Math.round(v * 100) / 100).join(' ') };
            for (const k of props) row[k] = cs[k];
            if (el.tagName === 'INPUT') row.placeholderColor = getComputedStyle(el, '::placeholder').color;
            out.push(row);
          });
        }
        return out;
      }, [roots, [...BOX, ...PAINT]]);
      await page.close();
    }
  }
  await ctx.close();
}
await browser.close();
server.close();
fs.writeFileSync(args.out, JSON.stringify(dump));
console.log(`${Object.keys(dump).length} relevés, ${Object.values(dump).reduce((s, v) => s + v.length, 0)} éléments → ${args.out}`);

function compare(fa, fb) {
  if (!fa || !fb) { console.error('Usage : node verify-calc.mjs --compare <A.json> <B.json> [--geometry]'); process.exit(2); }
  const A = JSON.parse(fs.readFileSync(fa, 'utf8')), B = JSON.parse(fs.readFileSync(fb, 'utf8'));
  const keys = args.geometry ? ['rect', ...BOX] : ['rect', ...BOX, ...PAINT, 'placeholderColor'];
  const tally = {}; let total = 0, elements = 0;
  for (const view of Object.keys(A)) {
    if (!B[view]) { console.log(`✗ ${view} : absent du second relevé`); total++; continue; }
    if (A[view].length !== B[view].length) { console.log(`✗ ${view} : ${A[view].length} éléments contre ${B[view].length}`); total++; continue; }
    A[view].forEach((a, i) => {
      const b = B[view][i]; elements++;
      for (const k of keys) if (a[k] !== b[k]) {
        total++;
        const id = `${a.key.replace(/ #\d+ /, ' ')} · ${k} : ${a[k]} → ${b[k]}`;
        tally[id] = (tally[id] || 0) + 1;
      }
    });
  }
  const lines = Object.entries(tally).sort((x, y) => y[1] - x[1]);
  for (const [id, n] of lines.slice(0, Number(args.max) || 60)) console.log(`×${n}  ${id}`);
  if (lines.length > (Number(args.max) || 60)) console.log(`… ${lines.length - (Number(args.max) || 60)} autres écarts distincts`);
  console.log(`\n${elements} éléments comparés (${args.geometry ? 'géométrie' : 'géométrie et couleurs'}) : ${total} écart(s), ${lines.length} distinct(s).`);
}
