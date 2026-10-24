// Collecte des statistiques des quatre sources -> data/AAAA-MM-JJ/<source>.json
//
//   npm run snapshot                       28 derniers jours (jusqu'à hier)
//   npm run snapshot -- --days=90
//   npm run snapshot -- --start=2026-06-01 --end=2026-09-30
//   npm run snapshot -- --all              depuis config.historyStart (GSC : 16 mois max)
//   npm run snapshot -- --source=gsc       une seule source : admob | ga4 | gsc | play
//   npm run snapshot -- --source=inspect   état d'indexation par URL (hors collecte par défaut : 59 appels)
//
// Une source en échec n'arrête pas les autres : l'erreur est consignée dans _status.json.

import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import { getAuth } from './lib/auth.mjs';
import { today, addDays, maxYMD } from './lib/dates.mjs';
import { fetchAdmob } from './fetch/admob.mjs';
import { fetchGa4 } from './fetch/ga4.mjs';
import { fetchGsc } from './fetch/gsc.mjs';
import { fetchPlay } from './fetch/play.mjs';
import { fetchInspect } from './fetch/inspect.mjs';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const CONFIG_PATH = path.join(__dirname, 'config.json');
const DATA_ROOT = path.join(__dirname, 'data');

const args = Object.fromEntries(
  process.argv.slice(2).map((a) => {
    const m = a.match(/^--([^=]+)(?:=(.*))?$/);
    return m ? [m[1], m[2] ?? true] : [a, true];
  })
);

const config = JSON.parse(fs.readFileSync(CONFIG_PATH, 'utf8'));
const endDate = args.end ?? addDays(today(), -1);
let startDate = args.start
  ?? (args.all ? config.historyStart : addDays(endDate, -(Number(args.days ?? 28) - 1)));

const SOURCES = {
  admob: fetchAdmob,
  ga4: fetchGa4,
  gsc: (auth, cfg, opts) => fetchGsc(auth, cfg, { ...opts, startDate: maxYMD(opts.startDate, addDays(opts.endDate, -485)) }),
  play: fetchPlay,
  inspect: fetchInspect,
};
const DEFAULT_SOURCES = ['admob', 'ga4', 'gsc', 'play'];
const wanted = args.source ? String(args.source).split(',') : DEFAULT_SOURCES;

const outDir = path.join(DATA_ROOT, today());
fs.mkdirSync(outDir, { recursive: true });

const auth = getAuth();
const status = { generatedAt: new Date().toISOString(), range: { startDate, endDate }, sources: {} };

console.log(`Collecte ${startDate} -> ${endDate} : ${wanted.join(', ')}`);
for (const name of wanted) {
  if (!SOURCES[name]) { console.error(`Source inconnue : ${name}`); continue; }
  const t0 = Date.now();
  try {
    const result = await SOURCES[name](auth, config, { startDate, endDate, dataRoot: DATA_ROOT });
    const file = path.join(outDir, `${name}.json`);
    fs.writeFileSync(file, JSON.stringify(result, null, 1));
    const counts = Object.fromEntries(
      Object.entries(result).filter(([, v]) => Array.isArray(v)).map(([k, v]) => [k, v.length])
    );
    status.sources[name] = { ok: true, file: path.relative(__dirname, file), counts, partialErrors: result.errors ?? undefined };
    console.log(`  ${name.padEnd(6)} OK  ${((Date.now() - t0) / 1000).toFixed(1)}s  ${JSON.stringify(counts)}`);
    if (result.errors && Object.keys(result.errors).length) {
      console.log(`         erreurs partielles : ${JSON.stringify(result.errors)}`);
    }
  } catch (err) {
    const detail = err.response?.data?.error ?? err.errors?.[0] ?? null;
    status.sources[name] = { ok: false, error: err.message, detail };
    console.log(`  ${name.padEnd(6)} ÉCHEC  ${err.message}`);
    if (detail) console.log(`         ${JSON.stringify(detail).slice(0, 600)}`);
  }
}

// Mémorise les identifiants découverts (propriété GA4, site GSC) pour les prochaines collectes.
let configChanged = false;
const ga4 = status.sources.ga4?.ok && JSON.parse(fs.readFileSync(path.join(outDir, 'ga4.json'), 'utf8')).property;
if (ga4?.propertyId && config.ga4.propertyId !== ga4.propertyId) { config.ga4.propertyId = ga4.propertyId; configChanged = true; }
const gsc = status.sources.gsc?.ok && JSON.parse(fs.readFileSync(path.join(outDir, 'gsc.json'), 'utf8')).site;
if (gsc?.siteUrl && config.gsc.siteUrl !== gsc.siteUrl) { config.gsc.siteUrl = gsc.siteUrl; configChanged = true; }
if (configChanged) {
  fs.writeFileSync(CONFIG_PATH, JSON.stringify(config, null, 2) + '\n');
  console.log('config.json mis à jour avec les identifiants découverts.');
}

fs.writeFileSync(path.join(outDir, '_status.json'), JSON.stringify(status, null, 2));
fs.writeFileSync(path.join(DATA_ROOT, 'latest.json'), JSON.stringify({ dir: path.relative(__dirname, outDir), ...status }, null, 2));
console.log(`Résultats dans ${path.relative(process.cwd(), outDir) || outDir}`);
