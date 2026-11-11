// Génère les favicons du site rtpcalculator.com à partir des SVG maîtres de logo/.
// Usage : node generate_favicons.mjs   (depuis "RTP Calculator - App")
//
// Lit uniquement logo/, écrit uniquement à la racine de "RTP Calculator - Website".
// Ne touche à rien dans app/src/main/res : l'icône adaptative Android n'est pas concernée.
//
// Pourquoi des fichiers et non un SVG en data: URI dans la balise <link> : Google n'affiche
// un favicon dans ses résultats que s'il peut télécharger un fichier à URL stable.

import sharp from 'sharp';
import path from 'path';
import fs from 'fs';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

const LOGO_DIR = path.join(__dirname, 'logo');
const SITE_DIR = path.join(__dirname, '..', 'RTP Calculator - Website');
const ROUNDED  = fs.readFileSync(path.join(LOGO_DIR, 'master-rounded.svg'));
const SQUARE   = fs.readFileSync(path.join(LOGO_DIR, 'master-square.svg'));
const BG_COLOR = '#0A0A0B';   // fond du logo maître

// Le viewBox des SVG maîtres fait 100 unités : on rastérise en 4x puis on réduit,
// ce qui donne un anticrénelage plus propre qu'un rendu direct aux petites tailles.
function render(svg, size) {
  const density = (72 * size * 4) / 100;
  return sharp(svg, { density }).resize(size, size, { kernel: 'lanczos3' });
}

// Conteneur ICO à entrées PNG (lu par tous les navigateurs et par Googlebot-Image).
function buildIco(images) {
  const header = Buffer.alloc(6);
  header.writeUInt16LE(0, 0);               // réservé
  header.writeUInt16LE(1, 2);               // type 1 = icône
  header.writeUInt16LE(images.length, 4);   // nombre d'images

  const entries = [];
  let offset = 6 + 16 * images.length;
  for (const { size, png } of images) {
    const e = Buffer.alloc(16);
    e.writeUInt8(size, 0);                  // largeur
    e.writeUInt8(size, 1);                  // hauteur
    e.writeUInt8(0, 2);                     // pas de palette
    e.writeUInt8(0, 3);                     // réservé
    e.writeUInt16LE(1, 4);                  // plans
    e.writeUInt16LE(32, 6);                 // bits par pixel
    e.writeUInt32LE(png.length, 8);
    e.writeUInt32LE(offset, 12);
    entries.push(e);
    offset += png.length;
  }
  return Buffer.concat([header, ...entries, ...images.map(i => i.png)]);
}

async function generate() {
  if (!fs.existsSync(SITE_DIR)) {
    throw new Error(`Dossier du site introuvable : ${SITE_DIR}`);
  }

  const icoImages = [];
  for (const size of [16, 32, 48]) {
    icoImages.push({ size, png: await render(ROUNDED, size).png().toBuffer() });
  }
  fs.writeFileSync(path.join(SITE_DIR, 'favicon.ico'), buildIco(icoImages));
  console.log('✓ favicon.ico (16, 32, 48)');

  await render(ROUNDED, 96).png().toFile(path.join(SITE_DIR, 'favicon-96x96.png'));
  console.log('✓ favicon-96x96.png');

  // iOS applique son propre masque et ne gère pas la transparence : fond plein, sans alpha.
  await render(SQUARE, 180)
    .flatten({ background: BG_COLOR })
    .png()
    .toFile(path.join(SITE_DIR, 'apple-touch-icon.png'));
  console.log('✓ apple-touch-icon.png (180)');
}

generate().catch(err => { console.error(err); process.exit(1); });
