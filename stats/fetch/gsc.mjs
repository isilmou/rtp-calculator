// Search Console (rtpcalculator.com) : clics, impressions, CTR, position ; état des sitemaps.
// Limite API : 16 mois d'historique.

import { google } from 'googleapis';

const ROW_LIMIT = 25000;

async function discoverSite(gsc, domain) {
  const sites = (await gsc.sites.list()).data.siteEntry ?? [];
  const mine = sites.filter((s) => s.siteUrl.includes(domain) && s.permissionLevel !== 'siteUnverifiedUser');
  if (!mine.length) {
    throw new Error(`Aucune propriété Search Console contenant « ${domain} » pour ce compte. Propriétés vues : ${sites.map((s) => s.siteUrl).join(', ') || 'aucune'}`);
  }
  // Préférer la propriété de domaine (sc-domain:), qui agrège http/https/www.
  const domainProp = mine.find((s) => s.siteUrl.startsWith('sc-domain:'));
  return { siteUrl: (domainProp ?? mine[0]).siteUrl, all: sites };
}

async function query(gsc, siteUrl, body) {
  const rows = [];
  let startRow = 0;
  for (;;) {
    const res = (await gsc.searchanalytics.query({
      siteUrl,
      requestBody: { ...body, rowLimit: ROW_LIMIT, startRow, dataState: 'all' },
    })).data;
    for (const r of res.rows ?? []) {
      const o = {};
      (body.dimensions ?? []).forEach((d, i) => { o[d] = r.keys[i]; });
      rows.push({ ...o, clicks: r.clicks, impressions: r.impressions, ctr: r.ctr, position: r.position });
    }
    if (!res.rows || res.rows.length < ROW_LIMIT) break;
    startRow += ROW_LIMIT;
  }
  return rows;
}

export async function fetchGsc(auth, config, { startDate, endDate }) {
  const gsc = google.searchconsole({ version: 'v1', auth });
  let { siteUrl } = config.gsc;
  let discovered = null;
  if (!siteUrl) {
    discovered = await discoverSite(gsc, config.gsc.domain);
    siteUrl = discovered.siteUrl;
  }
  const range = { startDate, endDate };

  const [daily, dailyCountryDevice, queryPage, pages, queryCountry, sitemaps] = await Promise.all([
    query(gsc, siteUrl, { ...range, dimensions: ['date'] }),
    query(gsc, siteUrl, { ...range, dimensions: ['date', 'country', 'device'] }),
    query(gsc, siteUrl, { ...range, dimensions: ['query', 'page'] }),
    query(gsc, siteUrl, { ...range, dimensions: ['page'] }),
    query(gsc, siteUrl, { ...range, dimensions: ['query', 'country'] }),
    gsc.sitemaps.list({ siteUrl }).then((r) => r.data.sitemap ?? []),
  ]);

  return {
    site: { siteUrl, otherProperties: discovered?.all.map((s) => s.siteUrl) },
    range,
    daily,
    dailyCountryDevice,
    queryPage,
    pages,
    queryCountry,
    sitemaps,
  };
}
