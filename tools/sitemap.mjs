// Génère "RTP Calculator - Website/sitemap.xml" à partir des pages du site.
//
//   node tools/sitemap.mjs            régénère le sitemap, affiche le résumé
//   node tools/sitemap.mjs --check    ne rien écrire ; code 1 si le sitemap n'est pas à jour
//
// Pour chaque index.html :
//   <loc>      = balise <link rel="canonical"> (vérifiée contre le chemin du fichier)
//   <lastmod>  = date du dernier commit Git du fichier ; aujourd'hui si modifié non commité
//   xhtml:link = balises <link rel="alternate" hreflang> du <head>, réciprocité vérifiée
// Le sitemap n'est jamais édité à la main : un lastmod faux est pire qu'un lastmod absent.

import fs from 'fs';
import path from 'path';
import { spawnSync } from 'child_process';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const ROOT = path.resolve(__dirname, '..');
const SITE_DIR = path.join(ROOT, 'RTP Calculator - Website');
const SITE_URL = 'https://rtpcalculator.com';
const OUTPUT = path.join(SITE_DIR, 'sitemap.xml');
const IGNORED_DIRS = new Set(['img', 'node_modules', '.git']);

const check = process.argv.includes('--check');
const errors = [];
const warnings = [];

function listPages(dir) {
  const out = [];
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    if (entry.isDirectory()) {
      if (!IGNORED_DIRS.has(entry.name)) out.push(...listPages(path.join(dir, entry.name)));
    } else if (entry.name === 'index.html') {
      out.push(path.join(dir, entry.name));
    }
  }
  return out;
}

function git(args) {
  const res = spawnSync('git', args, { cwd: ROOT, encoding: 'utf8' });
  if (res.status !== 0) throw new Error(`git ${args.join(' ')} : ${res.stderr.trim()}`);
  return res.stdout.trim();
}

function todayYMD() {
  return new Date().toISOString().slice(0, 10);
}

function lastmod(file) {
  const rel = path.relative(ROOT, file).split(path.sep).join('/');
  const dirty = git(['status', '--porcelain', '--', rel]);
  if (dirty) {
    warnings.push(`${rel} : modifications non commitées, lastmod = aujourd'hui`);
    return todayYMD();
  }
  const date = git(['log', '-1', '--format=%cs', '--', rel]);
  if (!date) {
    warnings.push(`${rel} : inconnu de Git, lastmod = aujourd'hui`);
    return todayYMD();
  }
  return date;
}

function parsePage(file) {
  const rel = path.relative(SITE_DIR, path.dirname(file)).split(path.sep).join('/');
  const expectedLoc = SITE_URL + '/' + (rel ? rel + '/' : '');
  const html = fs.readFileSync(file, 'utf8');
  const head = html.slice(0, html.indexOf('</head>'));

  const canonical = head.match(/<link\s+rel="canonical"\s+href="([^"]+)"/)?.[1];
  if (!canonical) errors.push(`${rel || '/'} : pas de balise canonical`);
  else if (canonical !== expectedLoc) errors.push(`${rel || '/'} : canonical ${canonical} ≠ chemin ${expectedLoc}`);

  const alternates = [];
  for (const m of head.matchAll(/<link\s+rel="alternate"\s+hreflang="([^"]+)"\s+href="([^"]+)"/g)) {
    alternates.push({ hreflang: m[1], href: m[2] });
  }
  if (!alternates.length) errors.push(`${rel || '/'} : aucune balise hreflang`);
  if (!alternates.some((a) => a.hreflang === 'x-default')) errors.push(`${rel || '/'} : pas de x-default`);

  return { file, loc: canonical ?? expectedLoc, alternates, lastmod: lastmod(file) };
}

// Réciprocité : chaque page citée dans un groupe hreflang doit exister et porter le même groupe.
function checkReciprocity(pages) {
  const byLoc = new Map(pages.map((p) => [p.loc, p]));
  const key = (p) => p.alternates.map((a) => `${a.hreflang}=${a.href}`).sort().join('\n');
  for (const page of pages) {
    const self = page.alternates.filter((a) => a.hreflang !== 'x-default' && a.href === page.loc);
    if (!self.length) errors.push(`${page.loc} : la page ne figure pas dans son propre groupe hreflang`);
    for (const alt of page.alternates) {
      const target = byLoc.get(alt.href);
      if (!target) { errors.push(`${page.loc} : hreflang ${alt.hreflang} → ${alt.href} introuvable`); continue; }
      if (key(target) !== key(page)) errors.push(`${page.loc} : groupe hreflang différent de ${alt.href}`);
    }
  }
}

function escapeXml(s) {
  return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

function render(pages) {
  const lines = [
    '<?xml version="1.0" encoding="UTF-8"?>',
    '<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9"',
    '        xmlns:xhtml="http://www.w3.org/1999/xhtml">',
  ];
  for (const p of pages) {
    lines.push('  <url>');
    lines.push(`    <loc>${escapeXml(p.loc)}</loc>`);
    lines.push(`    <lastmod>${p.lastmod}</lastmod>`);
    for (const a of p.alternates) {
      lines.push(`    <xhtml:link rel="alternate" hreflang="${escapeXml(a.hreflang)}" href="${escapeXml(a.href)}"/>`);
    }
    lines.push('  </url>');
  }
  lines.push('</urlset>');
  return lines.join('\n') + '\n';
}

const pages = listPages(SITE_DIR).map(parsePage).sort((a, b) => a.loc.localeCompare(b.loc));
checkReciprocity(pages);

for (const w of warnings) console.warn(`Avertissement : ${w}`);
if (errors.length) {
  for (const e of errors) console.error(`Erreur : ${e}`);
  console.error(`${errors.length} erreur(s), sitemap non écrit.`);
  process.exit(1);
}

const xml = render(pages);
const current = fs.existsSync(OUTPUT) ? fs.readFileSync(OUTPUT, 'utf8').replace(/\r\n/g, '\n') : '';
const dates = [...new Set(pages.map((p) => p.lastmod))].sort();
const summary = `${pages.length} pages, ${pages.reduce((n, p) => n + p.alternates.length, 0)} liens hreflang, lastmod ${dates.join(', ')}`;

if (check) {
  if (xml === current) { console.log(`Sitemap à jour : ${summary}`); process.exit(0); }
  console.error(`Sitemap à régénérer (node tools/sitemap.mjs) : ${summary}`);
  process.exit(1);
}
fs.writeFileSync(OUTPUT, xml, 'utf8');
console.log(`${xml === current ? 'Sitemap inchangé' : 'Sitemap écrit'} : ${summary}`);
