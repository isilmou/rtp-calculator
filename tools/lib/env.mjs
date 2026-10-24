// Confine Playwright au périmètre du dépôt (règle 7 de CLAUDE.md) :
// les navigateurs sont téléchargés dans tools/.browsers/, jamais dans le profil utilisateur.
// À importer AVANT tout import de 'playwright'.
import path from 'node:path';
import { fileURLToPath } from 'node:url';

export const TOOLS_DIR = path.dirname(path.dirname(fileURLToPath(import.meta.url)));
export const ROOT_DIR = path.dirname(TOOLS_DIR);
export const SITE_DIR = path.join(ROOT_DIR, 'RTP Calculator - Website');
export const SHOTS_DIR = path.join(TOOLS_DIR, 'shots');
export const BROWSERS_DIR = path.join(TOOLS_DIR, '.browsers');

process.env.PLAYWRIGHT_BROWSERS_PATH = BROWSERS_DIR;
process.env.PLAYWRIGHT_SKIP_BROWSER_GC = '1';

export const DEFAULT_PAGES = [
  '/fr/', '/fr/app/', '/fr/guides/', '/fr/guides/formats-de-cotes/', '/fr/a-propos/', '/fr/privacy/',
  '/de/', '/de/app/',
];
export const DEFAULT_WIDTHS = [375, 768, 1280];
export const LOCAL_PORT = 8123;

/** Nom de fichier sûr pour une URL de page : '/fr/guides/x/' → 'fr-guides-x', '/' → 'root'. */
export function slug(pagePath) {
  const s = pagePath.replace(/^\/|\/$/g, '').replace(/\//g, '-');
  return s || 'root';
}

/** Analyse `--cle=valeur` et `--drapeau` ; les arguments nus vont dans `_`. */
export function parseArgs(argv) {
  const out = { _: [] };
  for (const a of argv) {
    const m = /^--([^=]+)(?:=(.*))?$/.exec(a);
    if (m) out[m[1]] = m[2] === undefined ? true : m[2];
    else out._.push(a);
  }
  return out;
}

export function timestamp() {
  const d = new Date();
  const p = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}-${p(d.getHours())}${p(d.getMinutes())}`;
}
