// Search Console, API URL Inspection : état d'indexation de chaque page du site, vu par Google.
// Lecture seule (scope webmasters.readonly). Quota : 2 000 appels/jour, 600/minute.
//
// URL inspectées : les <loc> du sitemap local + config.inspect.extraUrls (variantes http:// et
// www. encore présentes dans les rapports, pour voir quel canonical Google leur attribue).
// Pas de notion de période : l'API renvoie l'état courant de l'index.

import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import { google } from 'googleapis';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const SITEMAP = path.resolve(__dirname, '..', '..', 'RTP Calculator - Website', 'sitemap.xml');
const CONCURRENCY = 4;

function sitemapUrls() {
  const xml = fs.readFileSync(SITEMAP, 'utf8');
  return [...xml.matchAll(/<loc>([^<]+)<\/loc>/g)].map((m) => m[1]);
}

function flatten(url, r) {
  const idx = r.indexStatusResult ?? {};
  return {
    url,
    verdict: idx.verdict ?? null,
    coverageState: idx.coverageState ?? null,
    indexingState: idx.indexingState ?? null,
    robotsTxtState: idx.robotsTxtState ?? null,
    pageFetchState: idx.pageFetchState ?? null,
    crawledAs: idx.crawledAs ?? null,
    lastCrawlTime: idx.lastCrawlTime ?? null,
    googleCanonical: idx.googleCanonical ?? null,
    userCanonical: idx.userCanonical ?? null,
    sitemap: idx.sitemap ?? [],
    referringUrls: idx.referringUrls ?? [],
    mobileUsability: r.mobileUsabilityResult?.verdict ?? null,
    richResults: r.richResultsResult?.verdict ?? null,
    inspectionResultLink: r.inspectionResultLink ?? null,
  };
}

export async function fetchInspect(auth, config) {
  const gsc = google.searchconsole({ version: 'v1', auth });
  const { siteUrl } = config.gsc;
  if (!siteUrl) throw new Error('config.gsc.siteUrl absent : lancer d\'abord une collecte gsc');

  const urls = [...new Set([...sitemapUrls(), ...(config.inspect?.extraUrls ?? [])])];
  const results = [];
  const errors = {};
  let next = 0;

  async function worker() {
    while (next < urls.length) {
      const url = urls[next++];
      try {
        const res = await gsc.urlInspection.index.inspect({
          requestBody: { inspectionUrl: url, siteUrl, languageCode: 'fr-FR' },
        });
        results.push(flatten(url, res.data.inspectionResult ?? {}));
      } catch (err) {
        errors[url] = err.response?.data?.error?.message ?? err.message;
      }
    }
  }
  await Promise.all(Array.from({ length: CONCURRENCY }, worker));

  results.sort((a, b) => urls.indexOf(a.url) - urls.indexOf(b.url));
  return {
    site: { siteUrl },
    inspectedAt: new Date().toISOString(),
    urls: results,
    errors: Object.keys(errors).length ? errors : undefined,
  };
}
