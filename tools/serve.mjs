// Serveur statique minimal (node:http, sans dépendance) pour la copie locale du site.
// Les liens du site sont en racine (/fr/app/, /assets/…) : file:// ne convient pas.
//   node serve.mjs            → http://localhost:8123/
//   import { startServer }    → utilisé par shots.mjs et a11y.mjs
import http from 'node:http';
import fs from 'node:fs';
import path from 'node:path';
import { pathToFileURL } from 'node:url';
import { SITE_DIR, LOCAL_PORT } from './lib/env.mjs';

const MIME = {
  '.html': 'text/html; charset=utf-8', '.css': 'text/css; charset=utf-8', '.js': 'text/javascript; charset=utf-8',
  '.mjs': 'text/javascript; charset=utf-8', '.json': 'application/json', '.xml': 'application/xml',
  '.txt': 'text/plain; charset=utf-8', '.png': 'image/png', '.jpg': 'image/jpeg', '.webp': 'image/webp',
  '.svg': 'image/svg+xml', '.ico': 'image/x-icon', '.woff2': 'font/woff2', '.woff': 'font/woff',
};

function resolveFile(urlPath) {
  const clean = decodeURIComponent(urlPath.split('?')[0]).replace(/\\/g, '/');
  let fp = path.resolve(SITE_DIR, '.' + clean);
  if (!fp.startsWith(SITE_DIR)) return null; // pas de sortie du docroot
  if (fs.existsSync(fp) && fs.statSync(fp).isDirectory()) {
    if (!clean.endsWith('/')) return { redirect: clean + '/' };
    fp = path.join(fp, 'index.html');
  }
  return fs.existsSync(fp) ? { file: fp } : null;
}

export function startServer(port = LOCAL_PORT) {
  return new Promise((resolve, reject) => {
    const srv = http.createServer((req, res) => {
      const r = resolveFile(req.url || '/');
      if (!r) { res.writeHead(404, { 'content-type': 'text/plain' }); return res.end('404'); }
      if (r.redirect) { res.writeHead(301, { location: r.redirect }); return res.end(); }
      res.writeHead(200, { 'content-type': MIME[path.extname(r.file).toLowerCase()] || 'application/octet-stream', 'cache-control': 'no-store' });
      fs.createReadStream(r.file).pipe(res);
    });
    srv.on('error', reject);
    srv.listen(port, '127.0.0.1', () => resolve({ server: srv, url: `http://localhost:${port}` }));
  });
}

if (process.argv[1] && import.meta.url === pathToFileURL(path.resolve(process.argv[1])).href) {
  const { url } = await startServer();
  console.log(`Site local : ${url}  (racine : ${SITE_DIR}) — Ctrl+C pour arrêter`);
}
