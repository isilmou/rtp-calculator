// Run after placing Logo_-_RTP_Calculator.png in C:\RTP-Calculator
// Usage: node generate_icons.mjs  (from the project root)
// Requires: npm install sharp  (in the project root first)

import sharp from 'sharp';
import path from 'path';
import fs from 'fs';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

const SRC      = 'C:/RTP-Calculator/Logo - RTP Calculator.png';
const RES_DIR  = path.join(__dirname, 'app/src/main/res');
const BG_COLOR = { r: 17, g: 17, b: 17, alpha: 1 };   // #111111 – app background
const TRANSPARENT = { r: 0, g: 0, b: 0, alpha: 0 };

// Legacy launcher icon sizes (48dp × density multiplier)
// Adaptive foreground canvas sizes (108dp × density multiplier)
const DENSITIES = [
  { name: 'mdpi',    legacy: 48,  foreground: 108 },
  { name: 'hdpi',    legacy: 72,  foreground: 162 },
  { name: 'xhdpi',   legacy: 96,  foreground: 216 },
  { name: 'xxhdpi',  legacy: 144, foreground: 324 },
  { name: 'xxxhdpi', legacy: 192, foreground: 432 },
];

if (!fs.existsSync(SRC)) {
  console.error(`Source file not found: ${SRC}`);
  console.error('Place Logo_-_RTP_Calculator.png in C:\\RTP-Calculator and re-run.');
  process.exit(1);
}

async function generate() {
  for (const d of DENSITIES) {
    const dir = path.join(RES_DIR, `mipmap-${d.name}`);

    // --- Legacy square icon (dark background, logo fitted inside) ---
    await sharp(SRC)
      .resize(d.legacy, d.legacy, { fit: 'contain', background: BG_COLOR })
      .flatten({ background: BG_COLOR })
      .png()
      .toFile(path.join(dir, 'ic_launcher.png'));

    // --- Legacy round icon (same image; launcher applies circular mask) ---
    await sharp(SRC)
      .resize(d.legacy, d.legacy, { fit: 'contain', background: BG_COLOR })
      .flatten({ background: BG_COLOR })
      .png()
      .toFile(path.join(dir, 'ic_launcher_round.png'));

    // --- Adaptive foreground (transparent canvas, logo in safe zone ~62%) ---
    // Safe zone for adaptive icons: inner 72dp of 108dp canvas (≈66.7%)
    // Using 60% gives a small comfortable margin so the logo is never clipped.
    const logoSize = Math.round(d.foreground * 0.60);
    const pad      = Math.round((d.foreground - logoSize) / 2);

    await sharp(SRC)
      .resize(logoSize, logoSize, { fit: 'contain', background: TRANSPARENT })
      .extend({ top: pad, bottom: pad, left: pad, right: pad, background: TRANSPARENT })
      .png()
      .toFile(path.join(dir, 'ic_launcher_fg.png'));

    // Remove old webp files
    for (const old of ['ic_launcher.webp', 'ic_launcher_round.webp']) {
      const p = path.join(dir, old);
      if (fs.existsSync(p)) fs.unlinkSync(p);
    }

    console.log(`✓ mipmap-${d.name}`);
  }

  // Remove old vector foreground drawable (replaced by per-density PNGs)
  const oldFg = path.join(RES_DIR, 'drawable/ic_launcher_foreground.xml');
  if (fs.existsSync(oldFg)) { fs.unlinkSync(oldFg); console.log('✓ removed ic_launcher_foreground.xml'); }

  // Update adaptive icon XMLs to reference the new per-density mipmap foreground
  const adaptiveXml = `<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@mipmap/ic_launcher_fg" />
</adaptive-icon>\n`;
  const anydpiDir = path.join(RES_DIR, 'mipmap-anydpi-v26');
  fs.writeFileSync(path.join(anydpiDir, 'ic_launcher.xml'), adaptiveXml);
  fs.writeFileSync(path.join(anydpiDir, 'ic_launcher_round.xml'), adaptiveXml);
  console.log('✓ updated adaptive icon XMLs');

  console.log('\nAll icons generated. Rebuild in Android Studio: Build > Clean Project, then Run.');
}

generate().catch(err => { console.error(err); process.exit(1); });
