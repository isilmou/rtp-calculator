// Déploiement du site statique vers l'hébergement Infomaniak.
// Offre Starter : FTP en clair sur le port 21 uniquement (pas de TLS, pas de SSH).
// Le transfert est confié à curl (présent sur la machine) ; les identifiants sont lus par curl
// dans ../_secrets/netrc (--netrc-file) et n'apparaissent jamais sur la ligne de commande.
//
//   npm run check                       liste le dossier distant : prouve connexion + chemin
//   npm run init                        SANS FTP : relit chaque fichier local en HTTPS sur le site,
//                                       compare les SHA-256, construit manifest.json, liste les écarts
//   npm run diff                        fichiers nouveaux / modifiés par rapport au manifeste
//   npm run push -- --apply             envoie les fichiers du diff, vérifie en HTTPS, met à jour le manifeste
//   npm run push -- --apply --only=robots.txt,fr/index.html
//
// Options : --apply (sans lui, push = diff)   --only=<chemins séparés par des virgules>
//           --htaccess (inclut .htaccess, protégé par défaut)   --active (FTP mode actif)
//           --verbose
//
// Le script ne supprime JAMAIS rien côté serveur. Une suppression se fait à la main dans le
// Manager Infomaniak et se note dans journal.md.

import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import os from 'node:os';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const CONFIG_PATH = path.join(__dirname, 'config.json');
const MANIFEST_PATH = path.join(__dirname, 'manifest.json');
const SECRETS_DIR = path.join(__dirname, '..', '_secrets');
const SECRETS_PATH = path.join(SECRETS_DIR, 'deploy.json');
const NETRC_PATH = path.join(SECRETS_DIR, 'netrc');

const [command = 'diff', ...rest] = process.argv.slice(2);
const args = Object.fromEntries(
  rest.map((a) => {
    const m = a.match(/^--([^=]+)(?:=(.*))?$/);
    return m ? [m[1], m[2] ?? true] : [a, true];
  })
);

const config = JSON.parse(fs.readFileSync(CONFIG_PATH, 'utf8'));
const SOURCE_DIR = path.resolve(__dirname, config.sourceDir);
const SITE_URL = config.siteUrl.replace(/\/$/, '');

// ---------------------------------------------------------------- utilitaires

const sha256 = (buf) => crypto.createHash('sha256').update(buf).digest('hex');
const toPosix = (p) => p.split(path.sep).join('/');

function matchesPattern(rel, pattern) {
  if (pattern.startsWith('*.')) return rel.endsWith(pattern.slice(1));
  return rel === pattern || rel.startsWith(pattern + '/');
}

function listLocalFiles() {
  const out = [];
  const walk = (dir) => {
    for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
      const abs = path.join(dir, entry.name);
      const rel = toPosix(path.relative(SOURCE_DIR, abs));
      if (config.exclude.some((p) => matchesPattern(rel, p))) continue;
      if (entry.isDirectory()) walk(abs);
      else if (entry.isFile()) out.push(rel);
    }
  };
  walk(SOURCE_DIR);
  return out.sort();
}

const isProtected = (rel) => config.protected.some((p) => matchesPattern(rel, p));

// Git (core.autocrlf=true) livre la copie de travail en CRLF alors que l'index et la production
// sont en LF. Les fichiers texte sont donc normalisés en LF avant hachage et avant envoi, pour
// que la production reste en LF quel que soit le poste de travail.
const TEXT_EXT = new Set(['.html', '.htm', '.txt', '.xml', '.css', '.js', '.mjs', '.json', '.svg', '.webmanifest', '.md']);
const isText = (rel) => TEXT_EXT.has(path.extname(rel).toLowerCase()) || path.basename(rel) === '.htaccess';

function localContent(rel) {
  const buf = fs.readFileSync(path.join(SOURCE_DIR, rel));
  return isText(rel) ? Buffer.from(buf.toString('utf8').replace(/\r\n/g, '\n'), 'utf8') : buf;
}

function localEntry(rel) {
  const buf = localContent(rel);
  return { sha256: sha256(buf), size: buf.length };
}

// URL publique d'un fichier : les index.html sont servis à l'URL du dossier (règle .htaccess).
function publicUrl(rel) {
  if (rel === 'index.html') return `${SITE_URL}/`;
  if (rel.endsWith('/index.html')) return `${SITE_URL}/${rel.slice(0, -'index.html'.length)}`;
  return `${SITE_URL}/${rel}`;
}

async function fetchPublic(rel) {
  const url = `${publicUrl(rel)}?v=${Date.now()}`;
  const res = await fetch(url, { cache: 'no-store', redirect: 'manual', signal: AbortSignal.timeout(20000) });
  if (res.status !== 200) return { status: res.status, buf: null };
  return { status: 200, buf: Buffer.from(await res.arrayBuffer()) };
}

function readManifest() {
  if (!fs.existsSync(MANIFEST_PATH)) return { generatedAt: null, siteUrl: SITE_URL, files: {} };
  return JSON.parse(fs.readFileSync(MANIFEST_PATH, 'utf8'));
}

function writeManifest(manifest) {
  manifest.generatedAt = new Date().toISOString();
  manifest.siteUrl = SITE_URL;
  manifest.files = Object.fromEntries(Object.entries(manifest.files).sort(([a], [b]) => a.localeCompare(b)));
  fs.writeFileSync(MANIFEST_PATH, JSON.stringify(manifest, null, 1) + '\n');
}

function readSecrets() {
  if (!fs.existsSync(SECRETS_PATH) || !fs.existsSync(NETRC_PATH)) {
    throw new Error(`Identifiants absents : attendu ${SECRETS_PATH} et ${NETRC_PATH} (à créer par Julien).`);
  }
  const s = JSON.parse(fs.readFileSync(SECRETS_PATH, 'utf8'));
  if (!s.host) throw new Error('deploy.json : champ "host" manquant.');
  const remoteRoot = String(s.remoteRoot ?? '').replace(/^\/+|\/+$/g, '');
  return { host: s.host, remoteRoot, mode: args.active ? 'active' : (s.mode ?? 'passive') };
}

function ftpUrl(secrets, rel = '') {
  const p = [secrets.remoteRoot, rel].filter(Boolean).join('/');
  return `ftp://${secrets.host}/${p}${rel ? '' : '/'}`;
}

function curl(secrets, extra) {
  const base = [
    '--silent', '--show-error',
    '--netrc-file', NETRC_PATH,
    '--connect-timeout', '15', '--max-time', '120', '--retry', '2',
    ...(secrets.mode === 'active' ? ['--ftp-port', '-'] : []),
    ...(args.verbose ? ['--verbose'] : []),
  ];
  const r = spawnSync('curl', [...base, ...extra], { encoding: 'utf8' });
  if (r.error) throw new Error(`curl introuvable ou injoignable : ${r.error.message}`);
  if (r.status !== 0) throw new Error(`curl a échoué (code ${r.status}) : ${r.stderr.trim()}`);
  if (args.verbose && r.stderr) console.error(r.stderr);
  return r.stdout;
}

function computeDiff(manifest, files) {
  const result = { added: [], modified: [], unchanged: [], missingLocally: [] };
  const local = {};
  for (const rel of files) {
    local[rel] = localEntry(rel);
    const known = manifest.files[rel];
    if (!known) result.added.push(rel);
    else if (known.sha256 !== local[rel].sha256) result.modified.push(rel);
    else result.unchanged.push(rel);
  }
  for (const rel of Object.keys(manifest.files)) if (!(rel in local)) result.missingLocally.push(rel);
  return { ...result, local };
}

function printDiff(d) {
  const line = (label, list) => { if (list.length) console.log(`${label} (${list.length}) :\n  ${list.join('\n  ')}`); };
  line('Nouveaux', d.added);
  line('Modifiés', d.modified);
  line('Présents dans le manifeste mais absents en local (suppression manuelle à faire)', d.missingLocally);
  console.log(`Inchangés : ${d.unchanged.length}`);
  if (!d.added.length && !d.modified.length) console.log('Rien à déployer.');
}

// ---------------------------------------------------------------- commandes

async function cmdInit() {
  const files = listLocalFiles().filter((rel) => !isProtected(rel));
  const manifest = readManifest();
  const mismatched = [], missing = [], errors = [];
  let identical = 0;
  console.log(`Relecture HTTPS de ${files.length} fichiers sur ${SITE_URL} (hors ${config.protected.join(', ')})`);
  for (const rel of files) {
    const loc = localEntry(rel);
    try {
      const remote = await fetchPublic(rel);
      if (remote.status !== 200) { missing.push(`${rel} (HTTP ${remote.status})`); continue; }
      if (sha256(remote.buf) === loc.sha256) {
        identical++;
        manifest.files[rel] = { sha256: loc.sha256, size: loc.size, verifiedAt: new Date().toISOString() };
      } else {
        mismatched.push(`${rel} (local ${loc.size} o, production ${remote.buf.length} o)`);
        delete manifest.files[rel];
      }
    } catch (err) {
      errors.push(`${rel} : ${err.message}`);
    }
  }
  writeManifest(manifest);
  console.log(`Identiques : ${identical}  |  Différents : ${mismatched.length}  |  Absents en production : ${missing.length}  |  Erreurs : ${errors.length}`);
  if (mismatched.length) console.log(`Différents (à arbitrer avant tout push) :\n  ${mismatched.join('\n  ')}`);
  if (missing.length) console.log(`Absents en production :\n  ${missing.join('\n  ')}`);
  if (errors.length) console.log(`Erreurs :\n  ${errors.join('\n  ')}`);
  console.log(`Manifeste écrit : ${path.relative(process.cwd(), MANIFEST_PATH)}`);
}

function cmdDiff() {
  const files = listLocalFiles().filter((rel) => args.htaccess || !isProtected(rel));
  const d = computeDiff(readManifest(), files);
  printDiff(d);
  return d;
}

function cmdCheck() {
  const secrets = readSecrets();
  console.log(`Connexion FTP (${secrets.mode}) à ${secrets.host}, dossier « ${secrets.remoteRoot || '(racine)'} »`);
  const listing = curl(secrets, ['--list-only', ftpUrl(secrets)]);
  const names = listing.split(/\r?\n/).filter(Boolean);
  console.log(`OK — ${names.length} entrées : ${names.slice(0, 15).join(', ')}${names.length > 15 ? ', …' : ''}`);
}

async function verifyPublished(rel, expectedSha) {
  for (let attempt = 1; attempt <= 3; attempt++) {
    try {
      const remote = await fetchPublic(rel);
      if (remote.status === 200 && sha256(remote.buf) === expectedSha) return true;
    } catch (err) {
      console.log(`    relecture HTTPS, tentative ${attempt}/3 : ${err.message}`);
    }
    await new Promise((r) => setTimeout(r, 2000 * attempt));
  }
  return false;
}

async function cmdPush() {
  const manifest = readManifest();
  const all = listLocalFiles().filter((rel) => args.htaccess || !isProtected(rel));
  const d = computeDiff(manifest, all);
  let targets = [...d.added, ...d.modified];
  if (args.only) {
    const only = String(args.only).split(',').map((s) => s.trim()).filter(Boolean);
    const unknown = only.filter((rel) => !all.includes(rel));
    if (unknown.length) throw new Error(`--only : fichiers inconnus ou protégés : ${unknown.join(', ')}`);
    targets = only;
  }
  if (!args.apply) {
    printDiff(d);
    console.log('\nAucun envoi : ajouter --apply pour déployer.');
    return;
  }
  if (!targets.length) { console.log('Rien à déployer.'); return; }
  const secrets = readSecrets();
  console.log(`Envoi FTP (${secrets.mode}) de ${targets.length} fichier(s) vers ${secrets.host}/${secrets.remoteRoot}`);
  for (const rel of targets) {
    const loc = d.local[rel] ?? localEntry(rel);
    const t0 = Date.now();
    // Copie temporaire normalisée (LF) : c'est elle qui part, pas le fichier CRLF de la copie de travail.
    const tmp = path.join(os.tmpdir(), `rtp-deploy-${process.pid}-${path.basename(rel)}`);
    fs.writeFileSync(tmp, localContent(rel));
    try {
      curl(secrets, ['--ftp-create-dirs', '-T', tmp, ftpUrl(secrets, rel)]);
    } finally {
      fs.rmSync(tmp, { force: true });
    }
    const verified = isProtected(rel) ? null : await verifyPublished(rel, loc.sha256);
    if (verified === false) {
      console.log(`  ${rel}  ENVOYÉ mais la relecture HTTPS ne correspond pas — arrêt.`);
      process.exitCode = 1;
      return;
    }
    manifest.files[rel] = { sha256: loc.sha256, size: loc.size, deployedAt: new Date().toISOString() };
    writeManifest(manifest);
    console.log(`  ${rel}  OK ${verified === null ? '(non vérifiable en HTTPS)' : 'vérifié'}  ${((Date.now() - t0) / 1000).toFixed(1)}s`);
  }
  console.log('Déploiement terminé.');
}

const COMMANDS = { check: cmdCheck, init: cmdInit, diff: cmdDiff, push: cmdPush };
if (!COMMANDS[command]) {
  console.error(`Commande inconnue : ${command}. Attendu : ${Object.keys(COMMANDS).join(' | ')}`);
  process.exit(2);
}
try {
  await COMMANDS[command]();
} catch (err) {
  console.error(`ÉCHEC : ${err.message}`);
  process.exit(1);
}
