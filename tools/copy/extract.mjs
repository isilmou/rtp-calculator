/* Extraction de tous les textes du site vers un document Word relisible par Julien.
   Usage : node copy/extract.mjs [--out=<dossier>]
   Produit <out>/site-textes-<date>.docx et <out>/snapshot.json (référence pour apply.mjs).

   Un texte identique repris sur plusieurs pages d'une même langue n'apparaît qu'une fois
   (première page où il figure) ; apply.mjs propage sa modification à toutes ses occurrences. */

import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import {
  Document, Packer, Paragraph, TextRun, ExternalHyperlink, Table, TableRow, TableCell,
  WidthType, ShadingType, BorderStyle, HeadingLevel, AlignmentType, PageBreak, LevelFormat,
} from 'docx';
import { LANGS, SITE, pagesOf, occurrencesOf, keyOf } from './lib.mjs';

const outDir = path.resolve(import.meta.dirname, (process.argv.find((a) => a.startsWith('--out=')) || '--out=out').slice(6));
fs.mkdirSync(outDir, { recursive: true });
const today = new Date().toISOString().slice(0, 10);
const token = crypto.randomBytes(3).toString('hex');

/* ---------- Collecte ---------- */

const snapshot = { generated: today, token, site: SITE, pages: [], rows: [] };
const rowIndex = new Map();   // lang + clé → row
let nOcc = 0;

for (const lang of LANGS) {
  const pages = pagesOf(lang);
  pages.forEach((page, pi) => {
    const src = fs.readFileSync(page.file, 'utf8');
    const occs = occurrencesOf(page, src);
    const entry = { lang: lang.code, key: page.key, label: page.label, url: page.url, file: path.relative(SITE, page.file).split(path.sep).join('/'), occurrences: [] };
    occs.forEach((o, oi) => {
      const id = `${lang.code}-${String(pi + 1).padStart(2, '0')}-${String(oi + 1).padStart(3, '0')}`;
      const k = lang.code + '\u0000' + keyOf(o.runs);
      let row = rowIndex.get(k);
      if (!row) {
        row = { id, lang: lang.code, page: entry.url, runs: o.runs, labels: [], count: 0 };
        rowIndex.set(k, row); snapshot.rows.push(row);
      }
      if (!row.labels.includes(o.label)) row.labels.push(o.label);
      row.count++;
      entry.occurrences.push({ id, row: row.id, kind: o.kind, label: o.label, raw: o.raw, quote: o.quote, tags: o.tags, plain: o.plain });
      nOcc++;
    });
    snapshot.pages.push(entry);
  });
}

/* ---------- Document Word ---------- */

const GRAY = '6B6B6B';
const text = (t, opts = {}) => new TextRun({ text: t, ...opts });
const para = (children, opts = {}) => new Paragraph({ children: Array.isArray(children) ? children : [text(children)], ...opts });

function runsToDocx(runs) {
  const out = [];
  for (const r of runs) {
    if (r.br) { out.push(new TextRun({ break: 1 })); continue; }
    const opts = { text: r.t, bold: !!r.b, italics: !!r.i };
    if (r.o) opts.shading = { type: ShadingType.CLEAR, fill: 'E6E6E6', color: 'auto' };
    if (r.a != null) out.push(new ExternalHyperlink({ children: [new TextRun({ ...opts, style: 'Hyperlink' })], link: r.a || '#' }));
    else out.push(new TextRun(opts));
  }
  return out;
}

const border = { style: BorderStyle.SINGLE, size: 4, color: 'C8C8C8' };
const borders = { top: border, bottom: border, left: border, right: border, insideHorizontal: border, insideVertical: border };
const W = [1750, 7888];   // A4 moins marges 2 × 1134 = 9638 DXA

function rowOf(row) {
  const plain = row.runs.map((r) => (r.br ? '' : r.t)).join('');
  const meta = [];
  meta.push(row.labels.slice(0, 2).join(' / ') + (row.labels.length > 2 ? ' …' : ''));
  if (row.labels.some((l) => /title|Meta description|Open Graph/.test(l))) meta.push(`${plain.length} car.`);
  if (row.count > 1) meta.push(`×${row.count}`);
  return new TableRow({
    cantSplit: true,
    children: [
      new TableCell({
        width: { size: W[0], type: WidthType.DXA },
        shading: { type: ShadingType.CLEAR, fill: 'F4F4F4', color: 'auto' },
        margins: { top: 60, bottom: 60, left: 90, right: 90 },
        children: [
          para([text(row.id, { font: 'Consolas', size: 15, color: '333333' })]),
          para([text(meta.join(' · '), { size: 15, color: GRAY })]),
        ],
      }),
      new TableCell({
        width: { size: W[1], type: WidthType.DXA },
        margins: { top: 60, bottom: 60, left: 110, right: 110 },
        children: [new Paragraph({ children: runsToDocx(row.runs) })],
      }),
    ],
  });
}

const children = [];
children.push(new Paragraph({ heading: HeadingLevel.TITLE, children: [text('rtpcalculator.com — tous les textes du site')] }));
children.push(para([text(`Extraction du ${today.split('-').reverse().join('/')} · jeton ${token} · ${snapshot.rows.length} textes distincts pour ${nOcc} emplacements sur ${snapshot.pages.length - LANGS.length} pages et le bandeau cookies.`, { color: GRAY })]));
children.push(new Paragraph({ heading: HeadingLevel.HEADING_1, children: [text('Mode d’emploi')] }));
const bullets = [
  'Modifiez uniquement la colonne de droite. La colonne grise (référence et nature du texte) sert à retrouver chaque emplacement dans le site : ne pas la toucher.',
  'Ne supprimez ni n’ajoutez de ligne dans les tableaux. Pour retirer un texte du site, laissez la cellule vide : je vous demanderai confirmation avant de l’appliquer.',
  'Gras, italique et liens sont reportés tels quels dans le site. Un texte sur fond gris est porté par une mise en forme particulière du site (mot surligné en jaune, flèche de menu…) : modifiable, mais à conserver comme bloc.',
  'Un texte repris à l’identique sur plusieurs pages d’une même langue n’apparaît qu’une fois, avec la mention « ×N » : sa modification s’appliquera partout (menus, pieds de page, boutons).',
  'Le compte de caractères est indiqué pour les balises <title> et les meta descriptions (Google tronque vers 60 et 155 caractères).',
  'Le site utilise l’apostrophe droite (\') ; Word tape une apostrophe courbe (’) : je la normaliserai. Les espaces insécables ajoutées par Word devant « : ; ? ! » seront signalées.',
  'Pour les versions EN, ES, DE et PT-BR : le français reste la langue maîtresse. Si vous modifiez un texte français sans sa traduction, dites-le-moi et je propagerai.',
];
for (const b of bullets) children.push(new Paragraph({ numbering: { reference: 'bullets', level: 0 }, children: [text(b)] }));

for (const lang of LANGS) {
  children.push(new Paragraph({ children: [new PageBreak()] }));
  children.push(new Paragraph({ heading: HeadingLevel.HEADING_1, children: [text(`${lang.label} (${lang.code})`)] }));
  for (const page of snapshot.pages.filter((p) => p.lang === lang.code)) {
    const rows = snapshot.rows.filter((r) => r.lang === lang.code && r.page === page.url);
    if (!rows.length) continue;
    children.push(new Paragraph({ heading: HeadingLevel.HEADING_2, children: [text(page.label)] }));
    children.push(para([text(page.key === 'cmp' ? page.file : `https://rtpcalculator.com${page.url}`, { color: GRAY, size: 17 }), text(`   ·   ${rows.length} texte(s)`, { color: GRAY, size: 17 })]));
    children.push(new Table({ columnWidths: W, width: { size: W[0] + W[1], type: WidthType.DXA }, borders, rows: rows.map(rowOf) }));
    children.push(para(''));
  }
}

const doc = new Document({
  creator: 'RTP Calculator — tools/copy/extract.mjs',
  title: 'rtpcalculator.com — textes du site',
  styles: {
    default: { document: { run: { font: 'Arial', size: 20 } } },
    paragraphStyles: [
      { id: 'Title', name: 'Title', basedOn: 'Normal', run: { size: 40, bold: true }, paragraph: { spacing: { after: 160 } } },
      { id: 'Heading1', name: 'Heading 1', basedOn: 'Normal', next: 'Normal', quickFormat: true, run: { size: 32, bold: true, color: '111111' }, paragraph: { spacing: { before: 360, after: 160 }, outlineLevel: 0 } },
      { id: 'Heading2', name: 'Heading 2', basedOn: 'Normal', next: 'Normal', quickFormat: true, run: { size: 24, bold: true, color: '111111' }, paragraph: { spacing: { before: 300, after: 60 }, keepNext: true, outlineLevel: 1 } },
    ],
    characterStyles: [{ id: 'Hyperlink', name: 'Hyperlink', run: { color: '0B5BD3', underline: {} } }],
  },
  numbering: { config: [{ reference: 'bullets', levels: [{ level: 0, format: LevelFormat.BULLET, text: '•', alignment: AlignmentType.LEFT, style: { paragraph: { indent: { left: 540, hanging: 270 } } } }] }] },
  sections: [{ properties: { page: { margin: { top: 1134, bottom: 1134, left: 1134, right: 1134 } } }, children }],
});

const docxPath = path.join(outDir, `site-textes-${today}.docx`);
fs.writeFileSync(docxPath, await Packer.toBuffer(doc));
snapshot.docx = path.basename(docxPath);
fs.writeFileSync(path.join(outDir, 'snapshot.json'), JSON.stringify(snapshot, null, 1), 'utf8');

const perLang = LANGS.map((l) => `${l.code} ${snapshot.rows.filter((r) => r.lang === l.code).length}`).join(', ');
console.log(`${docxPath}\n${snapshot.rows.length} textes distincts (${perLang}) pour ${nOcc} emplacements ; jeton ${token}.`);
