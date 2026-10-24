// Contrôle d'accessibilité avec axe-core (WCAG 2.x A/AA + bonnes pratiques) sur la copie locale.
//   node a11y.mjs                                  → 8 pages × 375/1280 → tools/shots/<date>-a11y/
//   node a11y.mjs --pages=/fr/,/fr/app/ --url=https://rtpcalculator.com
import { DEFAULT_PAGES, SHOTS_DIR, parseArgs, slug, timestamp } from './lib/env.mjs';
import fs from 'node:fs';
import path from 'node:path';
import { startServer } from './serve.mjs';

const args = parseArgs(process.argv.slice(2));
const { chromium } = await import('playwright');
const { default: AxeBuilder } = await import('@axe-core/playwright');

const pages = args.pages ? String(args.pages).split(',') : DEFAULT_PAGES;
const widths = args.widths ? String(args.widths).split(',').map(Number) : [375, 1280];
let base = args.url, server = null;
if (!base) ({ server, url: base } = await startServer());
const outDir = path.join(SHOTS_DIR, `${timestamp()}-${args.label || 'a11y'}`);
fs.mkdirSync(outDir, { recursive: true });

const BLOCKED = /googletagmanager\.com|google-analytics\.com|googlesyndication\.com|doubleclick\.net/;
const browser = await chromium.launch();
const all = [];
for (const width of widths) {
  const ctx = await browser.newContext({ viewport: { width, height: width < 600 ? 740 : 900 }, isMobile: width < 600, hasTouch: width < 600, colorScheme: 'dark' });
  await ctx.route(/.*/, (r) => BLOCKED.test(r.request().url()) ? r.abort() : r.continue());
  if (!args.cmp) await ctx.addInitScript(() => { try { localStorage.setItem('rtp_consent_v1', JSON.stringify({ analytics: false, ads: false, ts: Date.now() })); } catch (e) {} });
  for (const p of pages) {
    const page = await ctx.newPage();
    await page.goto(base.replace(/\/$/, '') + p, { waitUntil: 'networkidle', timeout: 60000 });
    await page.evaluate(() => document.fonts.ready);
    const res = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa', 'wcag21aa', 'wcag22aa', 'best-practice']).analyze();
    for (const v of res.violations) {
      all.push({ page: p, width, id: v.id, impact: v.impact, help: v.help, nodes: v.nodes.length,
        samples: v.nodes.slice(0, 4).map((n) => ({ target: n.target.join(' '), html: n.html.slice(0, 120), msg: (n.any[0] || n.all[0] || {}).message || '' })) });
    }
    console.log(`${p} @${width} : ${res.violations.length} règle(s) en défaut, ${res.violations.reduce((s, v) => s + v.nodes.length, 0)} élément(s)`);
    fs.writeFileSync(path.join(outDir, `${slug(p)}-${width}.json`), JSON.stringify(res.violations, null, 2));
    await page.close();
  }
  await ctx.close();
}
await browser.close();
if (server) server.close();

const L = [`# axe-core — ${base}`, '', '| Page | Larg. | Règle | Impact | Éléments | Exemple |', '|---|---|---|---|---|---|'];
for (const v of all) L.push(`| ${v.page} | ${v.width} | ${v.id} | ${v.impact} | ${v.nodes} | \`${v.samples[0]?.target}\` — ${v.samples[0]?.msg} |`);
L.push('', '## Synthèse par règle', '');
const byRule = {};
for (const v of all) { byRule[v.id] = byRule[v.id] || { help: v.help, impact: v.impact, nodes: 0, pages: new Set() }; byRule[v.id].nodes += v.nodes; byRule[v.id].pages.add(`${v.page}@${v.width}`); }
for (const [id, r] of Object.entries(byRule).sort((a, b) => b[1].nodes - a[1].nodes)) L.push(`- **${id}** (${r.impact}) : ${r.nodes} élément(s) sur ${r.pages.size} page(s) — ${r.help}`);
fs.writeFileSync(path.join(outDir, 'report.md'), L.join('\n') + '\n');
fs.writeFileSync(path.join(outDir, 'report.json'), JSON.stringify(all, null, 2));
console.log(`\n${L.slice(L.indexOf('## Synthèse par règle')).join('\n')}\nDossier : ${outDir}`);
