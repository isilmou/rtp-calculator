// Authentification OAuth (compte Google de Julien), lecture seule.
//
//   npm run auth      -> une seule fois : ouvre le navigateur, enregistre ../_secrets/token.json
//   getAuth()         -> utilisé par les collecteurs : recharge le jeton, le rafraîchit seul
//
// Les fichiers de ../_secrets/ ne sont lus que par ce script. Claude Code n'y a pas accès
// (règle deny dans .claude/settings.json) et Git les ignore.

import fs from 'fs';
import path from 'path';
import http from 'http';
import { exec } from 'child_process';
import { fileURLToPath } from 'url';
import { google } from 'googleapis';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const SECRETS_DIR = path.resolve(__dirname, '..', '..', '_secrets');
const CLIENT_PATH = path.join(SECRETS_DIR, 'oauth-client.json');
const TOKEN_PATH = path.join(SECRETS_DIR, 'token.json');

// Tous en lecture seule, sauf androidpublisher qui n'a pas de variante readonly
// (utilisé uniquement pour reviews.list).
export const SCOPES = [
  'https://www.googleapis.com/auth/admob.readonly',
  'https://www.googleapis.com/auth/adsense.readonly',
  'https://www.googleapis.com/auth/analytics.readonly',
  'https://www.googleapis.com/auth/webmasters.readonly',
  'https://www.googleapis.com/auth/playdeveloperreporting',
  'https://www.googleapis.com/auth/androidpublisher',
  'https://www.googleapis.com/auth/devstorage.read_only',
];

function loadClientConfig() {
  if (!fs.existsSync(CLIENT_PATH)) {
    throw new Error(
      `Client OAuth introuvable : ${CLIENT_PATH}\n` +
      'Télécharger le JSON de l\'ID client « Application de bureau » depuis Google Cloud ' +
      'et l\'enregistrer sous ce nom.'
    );
  }
  const raw = JSON.parse(fs.readFileSync(CLIENT_PATH, 'utf8'));
  const cfg = raw.installed ?? raw.web;
  if (!cfg?.client_id || !cfg?.client_secret) {
    throw new Error('oauth-client.json : clés client_id / client_secret absentes.');
  }
  return cfg;
}

function saveToken(tokens) {
  fs.mkdirSync(SECRETS_DIR, { recursive: true });
  fs.writeFileSync(TOKEN_PATH, JSON.stringify(tokens, null, 2), { mode: 0o600 });
}

function createClient(redirectUri) {
  const { client_id, client_secret } = loadClientConfig();
  const client = new google.auth.OAuth2(client_id, client_secret, redirectUri);
  // Google renvoie parfois un nouveau refresh_token : on fusionne pour ne rien perdre.
  client.on('tokens', (tokens) => {
    const previous = fs.existsSync(TOKEN_PATH) ? JSON.parse(fs.readFileSync(TOKEN_PATH, 'utf8')) : {};
    saveToken({ ...previous, ...tokens });
  });
  return client;
}

// Client prêt à l'emploi pour les collecteurs.
export function getAuth() {
  if (!fs.existsSync(TOKEN_PATH)) {
    throw new Error('Aucun jeton enregistré. Lancer d\'abord : npm run auth');
  }
  const client = createClient();
  client.setCredentials(JSON.parse(fs.readFileSync(TOKEN_PATH, 'utf8')));
  return client;
}

// Flux « loopback » : serveur HTTP local éphémère qui reçoit le code d'autorisation.
async function authorize() {
  const server = http.createServer();
  await new Promise((resolve) => server.listen(0, '127.0.0.1', resolve));
  const port = server.address().port;
  const redirectUri = `http://127.0.0.1:${port}`;
  const client = createClient(redirectUri);

  const url = client.generateAuthUrl({
    access_type: 'offline',
    prompt: 'consent',
    scope: SCOPES,
  });

  console.log('Ouverture du navigateur pour l\'autorisation Google (compte contact@rtpcalculator.com).');
  console.log('Si rien ne s\'ouvre, copier cette URL dans le navigateur :\n');
  console.log(url + '\n');
  // Entre guillemets, cmd.exe ne traite pas les « & » : aucun échappement à ajouter.
  exec(`start "" "${url}"`);

  const code = await new Promise((resolve, reject) => {
    server.on('request', (req, res) => {
      const params = new URL(req.url, redirectUri).searchParams;
      if (params.get('error')) {
        res.end('Autorisation refusée. Vous pouvez fermer cet onglet.');
        reject(new Error(`Autorisation refusée : ${params.get('error')}`));
        return;
      }
      const c = params.get('code');
      if (!c) { res.end(); return; }
      res.setHeader('Content-Type', 'text/plain; charset=utf-8');
      res.end('Autorisation reçue. Vous pouvez fermer cet onglet et revenir au terminal.');
      resolve(c);
    });
  });
  server.close();

  const { tokens } = await client.getToken(code);
  if (!tokens.refresh_token) {
    throw new Error(
      'Google n\'a pas renvoyé de refresh_token. Révoquer l\'accès de « RTP Calculator Stats » dans ' +
      'myaccount.google.com/permissions puis relancer npm run auth.'
    );
  }
  saveToken(tokens);
  console.log(`Jeton enregistré dans ${TOKEN_PATH}`);
  console.log('Portées accordées :', (tokens.scope ?? '').split(' ').join('\n  '));
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  authorize().catch((err) => {
    console.error('Échec de l\'autorisation :', err.message);
    process.exit(1);
  });
}
