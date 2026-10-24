/* RTP Calculator — extraction et réinjection des textes du site.
   Bibliothèque commune à extract.mjs (HTML → Word) et apply.mjs (Word → HTML).

   Principe : un analyseur HTML minimal qui conserve les offsets exacts dans la source,
   pour que chaque « occurrence » (bloc de texte, attribut, chaîne JSON-LD ou JS) soit
   relue puis réécrite sans jamais reformater le reste du fichier. */

import fs from 'node:fs';
import path from 'node:path';

export const SITE = path.resolve(import.meta.dirname, '..', '..', 'RTP Calculator - Website');

/* ---------- Langues et pages, dans l'ordre du document ---------- */

export const LANGS = [
  { code: 'fr', dir: 'fr', label: 'Français' },
  { code: 'en', dir: '',   label: 'English' },
  { code: 'es', dir: 'es', label: 'Español' },
  { code: 'de', dir: 'de', label: 'Deutsch' },
  { code: 'pt', dir: 'pt', label: 'Português (Brasil)' },
];

const PAGE_LABELS = {
  home: 'Accueil', app: 'Calculateur en ligne', guides: 'Index des guides',
  about: 'À propos', privacy: 'Politique de confidentialité', cmp: 'Bandeau cookies (assets/cmp.js)',
};

/** Liste ordonnée des pages d'une langue : accueil, calculateur, index des guides,
 *  les guides dans l'ordre du menu de l'accueil, à-propos, confidentialité. */
export function pagesOf(lang) {
  const root = lang.dir ? path.join(SITE, lang.dir) : SITE;
  const url = (p) => '/' + [lang.dir, p].filter(Boolean).join('/') + (p || lang.dir ? '/' : '');
  const pages = [];
  const add = (key, rel, label) => {
    const file = path.join(root, rel, 'index.html');
    if (!fs.existsSync(file)) throw new Error(`Page absente : ${file}`);
    pages.push({ key, label, file, url: url(rel), lang: lang.code });
  };
  add('home', '', PAGE_LABELS.home);
  add('app', 'app', PAGE_LABELS.app);
  add('guides', 'guides', PAGE_LABELS.guides);
  const home = fs.readFileSync(path.join(root, 'index.html'), 'utf8');
  const dd = home.match(/<div class="dd">([\s\S]*?)<\/div>/);
  if (!dd) throw new Error(`Menu des guides introuvable dans l'accueil ${lang.code}`);
  const seen = new Set();
  for (const m of dd[1].matchAll(/<a href="([^"]+)">([^<]*)<\/a>/g)) {
    const href = m[1];
    const slug = href.replace(/\/$/, '').split('/').pop();
    if (slug === 'guides' || seen.has(slug)) continue;
    seen.add(slug);
    add('guide:' + slug, 'guides/' + slug, 'Guide : ' + decodeEntities(m[2]));
  }
  const guidesDir = path.join(root, 'guides');
  for (const d of fs.readdirSync(guidesDir, { withFileTypes: true })) {
    if (d.isDirectory() && !seen.has(d.name)) throw new Error(`Guide ${lang.code}/${d.name} absent du menu`);
  }
  const aboutDir = fs.readdirSync(root, { withFileTypes: true })
    .filter((d) => d.isDirectory() && !['app', 'guides', 'privacy', 'fr', 'es', 'de', 'pt', 'assets', 'img'].includes(d.name))
    .map((d) => d.name);
  if (aboutDir.length !== 1) throw new Error(`Page à-propos ambiguë pour ${lang.code} : ${aboutDir.join(', ')}`);
  add('about', aboutDir[0], PAGE_LABELS.about);
  add('privacy', 'privacy', PAGE_LABELS.privacy);
  pages.push({ key: 'cmp', label: PAGE_LABELS.cmp, file: path.join(SITE, 'assets', 'cmp.js'), url: '/assets/cmp.js', lang: lang.code });
  return pages;
}

/* ---------- Entités ---------- */

const NAMED = {
  nbsp: ' ', amp: '&', lt: '<', gt: '>', quot: '"', apos: "'",
  rarr: '→', larr: '←', divide: '÷', minus: '−', times: '×', mdash: '—', ndash: '–',
  hellip: '…', laquo: '«', raquo: '»', euro: '€', deg: '°', plusmn: '±', copy: '©', middot: '·',
};

export function decodeEntities(s) {
  return s.replace(/&(#x[0-9a-fA-F]+|#[0-9]+|[a-zA-Z]+);/g, (m, e) => {
    if (e[0] === '#') return String.fromCodePoint(parseInt(e[1] === 'x' ? e.slice(2) : e.slice(1), e[1] === 'x' ? 16 : 10));
    if (e in NAMED) return NAMED[e];
    throw new Error(`Entité inconnue : ${m}`);
  });
}

/** Encodage minimal pour un contenu d'élément : seuls &, <, > et l'espace insécable. */
export function encodeText(s) {
  return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/ /g, '&nbsp;');
}
export function encodeAttr(s, quote = '"') {
  const base = s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/ /g, '&nbsp;');
  return quote === '"' ? base.replace(/"/g, '&quot;') : base.replace(/'/g, '&#39;');
}

/* ---------- Analyseur HTML minimal, offsets conservés ---------- */

const VOID = new Set(['meta', 'link', 'input', 'br', 'hr', 'img', 'source', 'wbr']);
const OPAQUE = /<!--[\s\S]*?-->|<script\b[^>]*>[\s\S]*?<\/script>|<style\b[^>]*>[\s\S]*?<\/style>|<svg\b[^>]*>[\s\S]*?<\/svg>|<!DOCTYPE[^>]*>/gi;
const TAG = /<(\/?)([a-zA-Z][a-zA-Z0-9]*)((?:\s+[^\s=>]+(?:=(?:"[^"]*"|'[^']*'|[^\s>]+))?)*)\s*(\/?)>/g;

export function parseHtml(html) {
  const root = { tag: '#root', children: [], start: 0, end: html.length, contentStart: 0, contentEnd: html.length, attrs: '' };
  const stack = [root];
  let pos = 0;
  const pushText = (from, to) => { if (to > from) stack[stack.length - 1].children.push({ text: true, start: from, end: to }); };
  const re = new RegExp(OPAQUE.source + '|' + TAG.source, 'gi');
  let m;
  while ((m = re.exec(html))) {
    pushText(pos, m.index);
    pos = re.lastIndex;
    const parent = stack[stack.length - 1];
    if (m[1] === undefined && m[2] === undefined) {           // bloc opaque
      const name = m[0].slice(1, 7).toLowerCase();
      const kind = name.startsWith('script') ? 'script' : name.startsWith('svg') ? 'svg' : name.startsWith('style') ? 'style' : 'other';
      parent.children.push({ opaque: kind, start: m.index, end: pos, raw: m[0] });
      continue;
    }
    const closing = m[1] === '/', tag = m[2].toLowerCase(), attrs = m[3], selfClose = m[4] === '/';
    if (closing) {
      if (parent.tag !== tag) throw new Error(`Balise </${tag}> inattendue à l'offset ${m.index} (ouverte : <${parent.tag}>)`);
      parent.contentEnd = m.index; parent.end = pos;
      stack.pop();
      continue;
    }
    const node = { tag, attrs, start: m.index, contentStart: pos, children: [], parent };
    parent.children.push(node);
    if (VOID.has(tag) || selfClose) { node.contentEnd = pos; node.end = pos; continue; }
    stack.push(node);
  }
  pushText(pos, html.length);
  if (stack.length !== 1) throw new Error(`Balise <${stack[stack.length - 1].tag}> jamais fermée`);
  return root;
}

export function attrOf(node, name) {
  const re = new RegExp(`(?:^|\\s)${name}=("([^"]*)"|'([^']*)')`);
  const m = node.attrs.match(re);
  if (!m) return null;
  const quote = m[1][0];
  const value = quote === '"' ? m[2] : m[3];
  // offset de la valeur dans la source : début du tag + position dans attrs
  const tagOpenLen = 1 + node.tag.length;
  const valueOffsetInAttrs = m.index + m[0].indexOf(m[1]) + 1;
  return { value, quote, start: node.start + tagOpenLen + valueOffsetInAttrs, end: node.start + tagOpenLen + valueOffsetInAttrs + value.length };
}

/* ---------- Représentation « riche » d'un texte : suite de runs ---------- */

const INLINE = new Set(['strong', 'em', 'b', 'i', 'a', 'span', 'br', 'code', 'sup', 'sub', 'abbr', 'small', 'u', 'mark', 'time']);
const CANDIDATE = new Set(['div', 'span', 'a', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6', 'p', 'li', 'td', 'th', 'title', 'button', 'label', 'summary', 'figcaption', 'dt', 'dd']);
const FMT = { strong: 'b', b: 'b', em: 'i', i: 'i', a: 'a' };

function hasBlockDescendant(node, depth = 0) {
  for (const c of node.children) {
    if (c.text) continue;
    if (c.opaque) { if (c.opaque === 'svg' && depth > 0) return true; continue; }
    if (!INLINE.has(c.tag) || hasBlockDescendant(c, depth + 1)) return true;
  }
  return false;
}

/** Transforme le HTML intérieur d'une feuille en runs + liste ordonnée des balises d'origine. */
export function htmlToRuns(html, node) {
  const runs = [], tags = [], interior = [];
  const walk = (n, fmt) => {
    for (const c of n.children) {
      if (c.text) { runs.push({ ...fmt, t: decodeEntities(html.slice(c.start, c.end)) }); continue; }
      if (c.opaque) { if (c.opaque === 'script' || c.opaque === 'style') throw new Error('Script inattendu dans un texte'); interior.push(c); continue; }
      if (c.tag === 'br') { runs.push({ br: true }); continue; }
      const type = FMT[c.tag] || 'o';
      tags.push({ type, open: html.slice(c.start, c.contentStart), close: html.slice(c.contentEnd, c.end) });
      const f = { ...fmt };
      if (type === 'b') f.b = true; else if (type === 'i') f.i = true;
      else if (type === 'a') f.a = (attrOf(c, 'href') || { value: '' }).value;
      else f.o = c.tag;
      walk(c, f);
    }
  };
  walk(node, {});
  const out = normalizeRuns(runs);
  if (interior.length && out.length) throw new Error(`SVG ou commentaire au milieu d'un texte : ${html.slice(interior[0].start, interior[0].start + 60)}`);
  return { runs: out, tags };
}

/** Fusionne les runs voisins de même format, réduit les blancs, retire les blancs de bord. */
export function normalizeRuns(runs) {
  const out = [];
  for (const r of runs) {
    if (r.br) { out.push({ br: true }); continue; }
    const last = out[out.length - 1];
    if (last && !last.br && sameFmt(last, r)) last.t += r.t; else out.push({ ...r });
  }
  for (const r of out) if (!r.br) r.t = r.t.replace(/[ \t\r\n]+/g, ' ');
  if (out.length && !out[0].br) out[0].t = out[0].t.replace(/^ +/, '');
  if (out.length && !out[out.length - 1].br) out[out.length - 1].t = out[out.length - 1].t.replace(/ +$/, '');
  return out.filter((r) => r.br || r.t !== '');
}
function sameFmt(a, b) { return !!a.b === !!b.b && !!a.i === !!b.i && (a.a || null) === (b.a || null) && (a.o || null) === (b.o || null); }

export function plainOf(runs) { return runs.map((r) => (r.br ? '\n' : r.t)).join(''); }

/** Clé de dédoublonnage : texte et mise en forme, sans les cibles de liens. */
export function keyOf(runs) {
  return runs.map((r) => (r.br ? '\u0001' : `${r.b ? 'B' : ''}${r.i ? 'I' : ''}${r.a != null ? 'A' : ''}${r.o ? 'O' : ''}|${r.t}`)).join('\u0002');
}

/** Reconstruit le HTML intérieur à partir de runs, en réutilisant les balises d'origine
 *  dans l'ordre (par type). Renvoie { html, warnings }. */
export function runsToHtml(runs, originalTags) {
  const queues = { b: [], i: [], a: [], o: [] };
  for (const t of originalTags) queues[t.type].push(t);
  const warnings = [];
  const generic = (type, run) => {
    warnings.push(`balise ${type === 'a' ? 'lien' : type === 'b' ? 'gras' : type === 'i' ? 'italique' : 'autre'} ajoutée sans équivalent d'origine`);
    if (type === 'a') return { open: `<a href="${encodeAttr(run.a || '#')}">`, close: '</a>' };
    if (type === 'b') return { open: '<strong>', close: '</strong>' };
    if (type === 'i') return { open: '<em>', close: '</em>' };
    return { open: '<span>', close: '</span>' };
  };
  let html = '';
  const open = [];   // pile { type, close }
  const fmtOf = (r) => [r.b && 'b', r.i && 'i', r.a != null && 'a', r.o && 'o'].filter(Boolean);
  for (const r of runs) {
    if (r.br) { html += '<br>'; continue; }
    const want = fmtOf(r);
    // fermer ce qui n'est plus voulu (dépiler jusqu'au premier élément à fermer)
    let keep = 0;
    while (keep < open.length && want.includes(open[keep].type)) keep++;
    while (open.length > keep) html += open.pop().close;
    for (const type of want) {
      if (open.some((o) => o.type === type)) continue;
      const t = queues[type].shift() || generic(type, r);
      if (type === 'a' && t.open.startsWith('<a') && r.a != null) {
        const href = attrOf({ tag: 'a', attrs: t.open.slice(2, -1), start: 0 }, 'href');
        if (href && decodeEntities(href.value) !== r.a) warnings.push(`cible de lien modifiée : ${decodeEntities(href.value)} → ${r.a}`);
      }
      html += t.open; open.push({ type, close: t.close });
    }
    html += encodeText(r.t);
  }
  while (open.length) html += open.pop().close;
  const left = Object.values(queues).reduce((n, q) => n + q.length, 0);
  if (left) warnings.push(`${left} balise(s) d'origine disparue(s)`);
  return { html, warnings };
}

/* ---------- Énumération des occurrences d'une page ---------- */

const META_LABELS = {
  'name=description': 'Meta description', 'property=og:title': 'Titre Open Graph',
  'property=og:description': 'Description Open Graph', 'property=og:image:alt': 'Texte alternatif de l’image de partage',
};
const ATTR_LABELS = { 'aria-label': 'Libellé d’accessibilité', placeholder: 'Texte indicatif du champ', alt: 'Texte alternatif', title: 'Info-bulle' };

function elementLabel(node) {
  const cls = (attrOf(node, 'class') || { value: '' }).value;
  const anc = (p) => { for (let n = node.parent; n; n = n.parent) if (p(n)) return n; return null; };
  switch (node.tag) {
    case 'title': return 'Balise <title>';
    case 'h1': return 'Titre H1'; case 'h2': return 'Titre H2'; case 'h3': return 'Titre H3'; case 'h4': return 'Titre H4';
    case 'p': return cls.includes('kicker') ? 'Surtitre' : cls.includes('lede') || cls === 'sub' || cls === 'dek' ? 'Chapeau' : cls.includes('date') || cls === 'updated' ? 'Date' : cls.includes('disclaimer') ? 'Avertissement de pied de page' : 'Paragraphe';
    case 'li': return 'Élément de liste';
    case 'td': return 'Cellule de tableau'; case 'th': return 'En-tête de tableau';
    case 'button': return 'Bouton';
    case 'a':
      if (/\bbtn\b|play-btn/.test(cls) || anc((n) => n.tag === 'div' && /\bcta\b/.test((attrOf(n, 'class') || { value: '' }).value))) return 'Bouton';
      if (cls === 'brand') return 'Nom du site (en-tête)';
      if (anc((n) => n.tag === 'nav')) return 'Lien de menu';
      if (anc((n) => n.tag === 'footer')) return 'Lien de pied de page';
      return 'Lien';
    case 'span': case 'div':
      if (anc((n) => n.tag === 'footer')) return 'Pied de page';
      if (/demo|odds/.test(cls)) return 'Carte d’exemple (accueil)';
      return 'Texte';
    default: return node.tag;
  }
}

/** Conteneur sans texte propre dont tous les enfants sont eux-mêmes candidats (ex. div.foot-row,
 *  div.dd, p.gall) : on descend dedans au lieu d'en faire une feuille. */
function isWrapperOnly(node, html) {
  let elements = 0;
  for (const c of node.children) {
    if (c.text) { if (html.slice(c.start, c.end).trim()) return false; continue; }
    if (c.opaque) continue;
    if (!CANDIDATE.has(c.tag)) return false;
    elements++;
  }
  return elements > 0;
}

/** Renvoie la liste ordonnée des occurrences d'une page HTML. */
export function occurrencesOfHtml(html) {
  const root = parseHtml(html);
  const occ = [];
  const push = (o) => { if (/\p{L}/u.test(o.plain)) occ.push(o); };
  const attrOcc = (node, name, label) => {
    const a = attrOf(node, name);
    if (!a) return;
    const runs = normalizeRuns([{ t: decodeEntities(a.value) }]);
    push({ kind: 'attr', start: a.start, end: a.end, raw: a.value, quote: a.quote, runs, plain: plainOf(runs), label, tags: [] });
  };
  const visit = (node) => {
    for (const c of node.children) {
      if (c.text) continue;
      if (c.opaque) {
        if (c.opaque === 'script') scriptOccurrences(c, html, push);
        continue;
      }
      if (c.tag === 'meta') {
        const key = ['name', 'property'].map((k) => { const a = attrOf(c, k); return a ? `${k}=${a.value}` : null; }).find(Boolean);
        if (key in META_LABELS) attrOcc(c, 'content', META_LABELS[key]);
        continue;
      }
      if (c.tag === 'nav' && (attrOf(c, 'class') || { value: '' }).value === 'lang') continue;
      if (c.tag !== 'nav') for (const name of Object.keys(ATTR_LABELS)) attrOcc(c, name, ATTR_LABELS[name]);
      if (CANDIDATE.has(c.tag) && !hasBlockDescendant(c) && !isWrapperOnly(c, html)) {
        // feuille : on retire les SVG et blancs de bord pour ne garder que la partie texte
        let s = c.contentStart, e = c.contentEnd;
        const kids = c.children;
        let i = 0, j = kids.length - 1;
        while (i <= j && (kids[i].opaque === 'svg' || kids[i].opaque === 'other' || (kids[i].text && !html.slice(kids[i].start, kids[i].end).trim()))) i++;
        while (j >= i && (kids[j].opaque === 'svg' || kids[j].opaque === 'other' || (kids[j].text && !html.slice(kids[j].start, kids[j].end).trim()))) j--;
        if (i > j) continue;
        s = kids[i].start; e = kids[j].end;
        const m1 = html.slice(s, e).match(/^\s*/)[0].length; s += m1;
        const m2 = html.slice(s, e).match(/\s*$/)[0].length; e -= m2;
        const sub = { children: kids.slice(i, j + 1) };
        const { runs, tags } = htmlToRuns(html, sub);
        push({ kind: 'html', start: s, end: e, raw: html.slice(s, e), runs, plain: plainOf(runs), label: elementLabel(c), tags });
        continue;
      }
      visit(c);
    }
  };
  visit(root);
  occ.sort((a, b) => a.start - b.start);
  for (let i = 1; i < occ.length; i++) if (occ[i].start < occ[i - 1].end) throw new Error(`Occurrences chevauchantes à l'offset ${occ[i].start}`);
  return occ;
}

const JS_STRING = /(\b[a-zA-Z_]+)\s*[:=]\s*"((?:[^"\\]|\\.)*)"/g;

function scriptOccurrences(node, html, push) {
  const raw = node.raw;
  if (/type="application\/ld\+json"/.test(raw)) {
    const m = raw.match(/"headline":\s*"((?:[^"\\]|\\.)*)"/);
    if (!m) throw new Error('JSON-LD sans headline');
    const start = node.start + m.index + m[0].indexOf(m[1]);
    const plain = JSON.parse('"' + m[1] + '"');
    const runs = normalizeRuns([{ t: plain }]);
    push({ kind: 'json', start, end: start + m[1].length, raw: m[1], runs, plain: plainOf(runs), label: 'Titre (données structurées)', tags: [] });
    return;
  }
  const a = raw.indexOf('/* ============ i18n'), b = raw.indexOf('/* ============ modèle');
  if (a < 0 || b < 0) return;  // scripts GA4, etc.
  jsStrings(raw.slice(a, b), node.start + a, push, 'Texte du calculateur');
}

export function jsStrings(src, base, push, labelPrefix, skip = new Set()) {
  for (const m of src.matchAll(JS_STRING)) {
    if (skip.has(m[1])) continue;
    const start = base + m.index + m[0].lastIndexOf(m[2]);
    const plain = JSON.parse('"' + m[2] + '"');
    const runs = normalizeRuns([{ t: plain }]);
    push({ kind: 'js', start, end: start + m[2].length, raw: m[2], runs, plain: plainOf(runs), label: `${labelPrefix} (${m[1]})`, tags: [] });
  }
}

/** Occurrences de assets/cmp.js pour une langue. */
export function occurrencesOfCmp(src, lang) {
  const re = new RegExp(`\\n    ${lang}: \\{([\\s\\S]*?)\\n    \\}`);
  const m = src.match(re);
  if (!m) throw new Error(`Bloc ${lang} introuvable dans cmp.js`);
  const occ = [];
  jsStrings(m[1], m.index + m[0].indexOf(m[1]), (o) => occ.push(o), 'Bandeau cookies', new Set(['privacy_url']));
  return occ;
}

export function occurrencesOf(page, src) {
  return page.key === 'cmp' ? occurrencesOfCmp(src, page.lang) : occurrencesOfHtml(src);
}

/** Réencode une occurrence selon sa nature. Renvoie { raw, warnings }. */
export function encodeOccurrence(kind, runs, occ) {
  const warnings = [];
  if (kind === 'html') { const r = runsToHtml(runs, occ.tags); return { raw: r.html, warnings: r.warnings }; }
  const fmt = runs.some((r) => r.br || r.b || r.i || r.a != null || r.o);
  if (fmt) warnings.push('mise en forme ignorée (texte brut attendu)');
  const plain = plainOf(runs).replace(/\n/g, ' ');
  if (kind === 'attr') return { raw: encodeAttr(plain, occ.quote), warnings };
  if (kind === 'json' || kind === 'js') return { raw: JSON.stringify(plain).slice(1, -1), warnings };
  throw new Error(`Nature inconnue : ${kind}`);
}
