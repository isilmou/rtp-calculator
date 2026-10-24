/* Réinjection dans le site des textes relus dans le document Word produit par extract.mjs.
   Usage : node copy/apply.mjs <fichier.docx> [--apply] [--empty-ok] [--snapshot=<json>]
   Sans --apply : relecture à blanc, rien n'est écrit. Tout ou rien : la moindre garde en échec
   (document d'une autre extraction, page modifiée depuis l'extraction) annule l'écriture. */

import fs from 'node:fs';
import path from 'node:path';
import { execFileSync } from 'node:child_process';
import { SITE, LANGS, pagesOf, occurrencesOf, keyOf, plainOf, normalizeRuns, encodeOccurrence } from './lib.mjs';

const args = process.argv.slice(2);
const docxPath = args.find((a) => !a.startsWith('--'));
const APPLY = args.includes('--apply'), EMPTY_OK = args.includes('--empty-ok');
const snapPath = path.resolve(import.meta.dirname, (args.find((a) => a.startsWith('--snapshot=')) || '--snapshot=out/snapshot.json').slice(11));
if (!docxPath) { console.error('Usage : node copy/apply.mjs <fichier.docx> [--apply] [--empty-ok]'); process.exit(2); }

const snapshot = JSON.parse(fs.readFileSync(snapPath, 'utf8'));
const rowsById = new Map(snapshot.rows.map((r) => [r.id, r]));

/* ---------- Lecture du .docx ---------- */

function unzipEntry(name) {
  if (fs.statSync(docxPath).isDirectory()) return fs.readFileSync(path.join(docxPath, name), 'utf8');  // .docx décompressé (tests)
  try { return execFileSync('unzip', ['-p', docxPath, name], { maxBuffer: 1 << 28 }).toString('utf8'); }
  catch (e) { throw new Error(`Impossible de lire ${name} dans ${docxPath} (unzip) : ${e.message}`); }
}
const documentXml = unzipEntry('word/document.xml');
const relsXml = unzipEntry('word/_rels/document.xml.rels');
const rels = new Map();
for (const m of relsXml.matchAll(/<Relationship\b([^>]*)\/>/g)) {
  const id = m[1].match(/\bId="([^"]*)"/), target = m[1].match(/\bTarget="([^"]*)"/);
  if (id && target) rels.set(id[1], xmlDecode(target[1]));
}

function xmlDecode(s) {
  return s.replace(/&(#x[0-9a-fA-F]+|#[0-9]+|amp|lt|gt|quot|apos);/g, (m, e) => {
    if (e[0] === '#') return String.fromCodePoint(parseInt(e[1] === 'x' ? e.slice(2) : e.slice(1), e[1] === 'x' ? 16 : 10));
    return { amp: '&', lt: '<', gt: '>', quot: '"', apos: "'" }[e];
  });
}

/** Analyseur XML minimal → arbre { name, attrs, children, text }. */
function parseXml(xml) {
  const root = { name: '#root', attrs: '', children: [] };
  const stack = [root];
  const re = /<\?[\s\S]*?\?>|<!--[\s\S]*?-->|<(\/?)([\w:.-]+)((?:\s+[\w:.-]+="[^"]*")*)\s*(\/?)>|([^<]+)/g;
  let m;
  while ((m = re.exec(xml))) {
    const top = stack[stack.length - 1];
    if (m[5] !== undefined) { if (top !== root) top.children.push({ text: xmlDecode(m[5]) }); continue; }
    if (m[2] === undefined) continue;
    if (m[1] === '/') { if (top.name !== m[2]) throw new Error(`XML : </${m[2]}> inattendu`); stack.pop(); continue; }
    const node = { name: m[2], attrs: m[3], children: [] };
    top.children.push(node);
    if (m[4] !== '/') stack.push(node);
  }
  return root;
}
const attr = (node, name) => { const m = node.attrs.match(new RegExp(`\\s${name}="([^"]*)"`)); return m ? xmlDecode(m[1]) : null; };
const find = (node, name) => node.children.find((c) => c.name === name);
const findAll = (node, name, out = []) => { for (const c of node.children) { if (c.name === name) out.push(c); else if (c.children) findAll(c, name, out); } return out; };

/** Un paragraphe Word → runs { t, b, i, a, o }. */
function paragraphRuns(p, inherited = {}) {
  const runs = [];
  const walk = (node, ctx) => {
    for (const c of node.children) {
      if (!c.name) continue;
      if (c.name === 'w:pPr' || c.name === 'w:del' || c.name === 'w:moveFrom') continue;
      if (c.name === 'w:hyperlink') {
        const rid = attr(c, 'r:id'), anchor = attr(c, 'w:anchor');
        walk(c, { ...ctx, a: rid ? rels.get(rid) ?? '' : anchor ? '#' + anchor : '' });
        continue;
      }
      if (c.name === 'w:r') {
        const pr = find(c, 'w:rPr');
        const flag = (n) => { const e = pr && find(pr, n); if (!e) return false; const v = attr(e, 'w:val'); return v == null || !['0', 'false', 'off'].includes(v); };
        const shd = pr && find(pr, 'w:shd');
        const fmt = { ...ctx, b: flag('w:b'), i: flag('w:i') };
        if (shd && /^e6e6e6$/i.test(attr(shd, 'w:fill') || '')) fmt.o = 'span';
        if (pr && find(pr, 'w:rStyle') && attr(find(pr, 'w:rStyle'), 'w:val') === 'Hyperlink' && fmt.a == null) fmt.a = '';
        for (const k of c.children) {
          if (k.name === 'w:t') runs.push({ ...fmt, t: k.children.map((x) => x.text || '').join('') });
          else if (k.name === 'w:br') runs.push({ br: true });
          else if (k.name === 'w:tab') runs.push({ ...fmt, t: ' ' });
          else if (k.name === 'w:noBreakHyphen') runs.push({ ...fmt, t: '‑' });
          else if (k.name === 'w:sym') runs.push({ ...fmt, t: String.fromCodePoint(parseInt(attr(k, 'w:char') || '20', 16)) });
        }
        continue;
      }
      if (c.children) walk(c, ctx);
    }
  };
  walk(p, inherited);
  return runs;
}

const doc = parseXml(documentXml);
const body = find(find(doc, 'w:document'), 'w:body');
const allText = findAll(body, 'w:t').map((t) => t.children.map((x) => x.text || '').join('')).join('');
const tokenInDoc = (allText.match(/jeton ([0-9a-f]{6})/) || [])[1];
if (tokenInDoc !== snapshot.token) {
  console.error(`Ce document (jeton ${tokenInDoc || 'absent'}) ne correspond pas à l'extraction de référence (jeton ${snapshot.token}).`);
  process.exit(1);
}

const docRows = new Map();
for (const tbl of findAll(body, 'w:tbl')) {
  for (const tr of tbl.children.filter((c) => c.name === 'w:tr')) {
    const cells = tr.children.filter((c) => c.name === 'w:tc');
    if (cells.length < 2) continue;
    const idPara = cells[0].children.find((c) => c.name === 'w:p');
    const id = idPara ? plainOf(paragraphRuns(idPara)).trim() : '';
    if (!/^[a-z]{2}-\d{2}-\d{3}$/.test(id)) continue;
    const runs = [];
    cells[1].children.filter((c) => c.name === 'w:p').forEach((p, i) => { if (i) runs.push({ br: true }); runs.push(...paragraphRuns(p)); });
    if (docRows.has(id)) { console.error(`Référence ${id} en double dans le document`); process.exit(1); }
    docRows.set(id, runs);
  }
}

/* ---------- Normalisation de ce que Word a pu changer ---------- */

function tidy(runs, original) {
  const notes = [];
  const origPlain = plainOf(original);
  for (const r of runs) {
    if (r.br) continue;
    let t = r.t;
    if (/[‘’]/.test(t) && !/[‘’]/.test(origPlain)) { t = t.replace(/[‘’]/g, "'"); notes.push('apostrophe courbe redressée'); }
    if (/[“”]/.test(t) && !/[“”]/.test(origPlain)) { t = t.replace(/[“”]/g, '"'); notes.push('guillemets anglais redressés'); }
    if (/[  ](?=[:;?!»])|«[  ]/.test(t) && !/[  ](?=[:;?!»])|«[  ]/.test(origPlain)) {
      t = t.replace(/[  ](?=[:;?!»])/g, ' ').replace(/«[  ]/g, '« ');
      notes.push('espace insécable Word retirée devant la ponctuation');
    }
    r.t = t;
  }
  return { runs: normalizeRuns(runs), notes };
}

/* ---------- Comparaison ---------- */

const changes = [];      // { row, runs, notes }
const emptied = [], missing = [];
for (const row of snapshot.rows) {
  const docRuns = docRows.get(row.id);
  if (!docRuns) { missing.push(row.id); continue; }
  const { runs, notes } = tidy(docRuns, row.runs);
  if (keyOf(runs) === keyOf(row.runs)) continue;
  if (!plainOf(runs).trim()) { emptied.push(row); if (!EMPTY_OK) continue; }
  changes.push({ row, runs, notes: [...new Set(notes)] });
}

/* ---------- Réécriture, fichier par fichier ---------- */

const pagesByLang = Object.fromEntries(LANGS.map((l) => [l.code, pagesOf(l)]));
const pageByUrl = new Map(snapshot.pages.map((p) => [p.lang + p.url, p]));
const edits = new Map();   // fichier → [{ occ (snapshot), change }]
for (const change of changes) {
  for (const page of snapshot.pages.filter((p) => p.lang === change.row.lang)) {
    for (const occ of page.occurrences) {
      if (occ.row !== change.row.id) continue;
      if (!edits.has(page.file)) edits.set(page.file, []);
      edits.get(page.file).push({ occ, page, change });
    }
  }
}

const report = [];
let guardFailed = false;
const written = [];
for (const [file, list] of edits) {
  const page0 = list[0].page;
  const pageDef = pagesByLang[page0.lang].find((p) => path.relative(SITE, p.file).split(path.sep).join('/') === file);
  const abs = path.join(SITE, file);
  let src = fs.readFileSync(abs, 'utf8');
  const fresh = occurrencesOf(pageDef, src);
  const snapOccs = pageByUrl.get(page0.lang + page0.url).occurrences;
  if (fresh.length !== snapOccs.length) { console.error(`${file} : ${fresh.length} emplacements aujourd'hui contre ${snapOccs.length} à l'extraction — page modifiée depuis, réextraire.`); guardFailed = true; continue; }
  for (let i = 0; i < fresh.length; i++) {
    if (fresh[i].raw !== snapOccs[i].raw) { console.error(`${file} : l'emplacement ${snapOccs[i].id} a changé depuis l'extraction (« ${fresh[i].plain.slice(0, 60)} »). Réextraire.`); guardFailed = true; break; }
  }
  if (guardFailed) continue;
  const byId = new Map(fresh.map((o, i) => [snapOccs[i].id, o]));
  const ops = list.map(({ occ, change }) => {
    const live = byId.get(occ.id);
    const { raw, warnings } = encodeOccurrence(occ.kind, change.runs, { ...occ, tags: live.tags });
    report.push({ id: change.row.id, at: occ.id, file, label: occ.label, before: occ.plain, after: plainOf(change.runs), notes: [...change.notes, ...warnings] });
    return { start: live.start, end: live.end, raw };
  }).sort((a, b) => b.start - a.start);
  for (const op of ops) src = src.slice(0, op.start) + op.raw + src.slice(op.end);
  if (APPLY) { fs.writeFileSync(abs, src, 'utf8'); written.push(file); }
}

/* ---------- Compte rendu ---------- */

const short = (s) => (s.length > 110 ? s.slice(0, 110) + '…' : s).replace(/\n/g, '⏎');
const byRow = new Map();
for (const r of report) { if (!byRow.has(r.id)) byRow.set(r.id, { ...r, files: [] }); byRow.get(r.id).files.push(r.file); byRow.get(r.id).notes = [...new Set([...byRow.get(r.id).notes, ...r.notes])]; }
console.log(`Document : ${path.basename(docxPath)} (jeton ${snapshot.token}) — ${docRows.size} lignes lues, ${snapshot.rows.length} attendues.`);
for (const r of byRow.values()) {
  console.log(`\n${r.id}  ${r.label}  →  ${r.files.length} fichier(s) : ${r.files.join(', ')}`);
  console.log(`  avant : ${short(r.before)}`);
  console.log(`  après : ${short(r.after)}`);
  if (r.notes.length) console.log(`  notes : ${r.notes.join(' ; ')}`);
}
if (emptied.length && !EMPTY_OK) console.log(`\n${emptied.length} texte(s) vidé(s) dans le document, NON appliqués (relancer avec --empty-ok pour les retirer du site) : ${emptied.map((r) => r.id).join(', ')}`);
if (missing.length) console.log(`\n${missing.length} référence(s) absente(s) du document, laissées telles quelles : ${missing.slice(0, 20).join(', ')}${missing.length > 20 ? '…' : ''}`);
console.log(`\n${byRow.size} texte(s) modifié(s), ${report.length} emplacement(s), ${edits.size} fichier(s).`);
if (guardFailed) { console.error('\nGarde en échec : rien n\'a été écrit.'); process.exit(1); }
if (APPLY) console.log(written.length ? `Écrit : ${written.join(', ')}\nPenser à : node sitemap.mjs` : 'Rien à écrire.');
else console.log(changes.length ? 'Relecture à blanc : relancer avec --apply pour écrire.' : 'Aucune modification détectée.');
