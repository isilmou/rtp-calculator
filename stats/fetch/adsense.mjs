// AdSense (site rtpcalculator.com) : état du compte et du site (approbation), blocs d'annonces,
// alertes, problèmes de règlement ; rapport revenus / pages vues / impressions / clics / RPM
// par jour × pays × plateforme, et par jour × page.
//
// Un compte ou un site non approuvé n'est pas une erreur : l'état est consigné et les
// rapports reviennent vides (même logique que les vitals Play sous le seuil).

import { google } from 'googleapis';
import { toDateParts } from '../lib/dates.mjs';

const METRICS = [
  'PAGE_VIEWS', 'AD_REQUESTS', 'MATCHED_AD_REQUESTS', 'IMPRESSIONS', 'CLICKS',
  'ESTIMATED_EARNINGS', 'PAGE_VIEWS_RPM', 'IMPRESSIONS_RPM', 'IMPRESSIONS_CTR',
];

function flattenReport(data) {
  const headers = (data.headers ?? []).map((h) => h.name);
  const rows = (data.rows ?? []).map((r) => {
    const o = {};
    (r.cells ?? []).forEach((c, i) => {
      const v = c.value;
      o[headers[i]] = v !== undefined && v !== '' && !Number.isNaN(Number(v)) && !/^\d{4}-\d{2}-\d{2}$/.test(v)
        ? Number(v) : v;
    });
    return o;
  });
  const totals = {};
  (data.totals?.cells ?? []).forEach((c, i) => { if (c.value !== undefined && c.value !== '') totals[headers[i]] = Number(c.value); });
  return { rows, totals, warnings: data.warnings ?? [] };
}

export async function fetchAdsense(auth, config, { startDate, endDate }) {
  const adsense = google.adsense({ version: 'v2', auth });
  const out = { range: { startDate, endDate }, errors: {} };

  const step = async (key, fn) => {
    try { out[key] = await fn(); }
    catch (err) { out.errors[key] = err.errors?.[0]?.message ?? err.message; }
  };

  // Compte : state = READY | NEEDS_ATTENTION | CLOSED ; pendingTasks = étapes d'activation restantes.
  const accounts = (await adsense.accounts.list({ pageSize: 50 })).data.accounts ?? [];
  const wanted = `accounts/${config.adsense.publisherId}`;
  const account = accounts.find((a) => a.name === wanted) ?? accounts[0] ?? null;
  if (!account) {
    out.account = null;
    out.note = `Aucun compte AdSense accessible à ce compte Google (attendu : ${wanted}).`;
    return out;
  }
  out.account = {
    name: account.name, displayName: account.displayName, state: account.state,
    pendingTasks: account.pendingTasks ?? [], premium: account.premium ?? false,
    timeZone: account.timeZone?.id, createTime: account.createTime,
  };
  const parent = account.name;

  // Sites : state = REQUIRES_REVIEW | GETTING_READY | READY | NEEDS_ATTENTION (approbation par domaine).
  await step('sites', async () => {
    const res = (await adsense.accounts.sites.list({ parent, pageSize: 100 })).data;
    return (res.sites ?? []).map((s) => ({ domain: s.domain, state: s.state, autoAdsEnabled: s.autoAdsEnabled ?? false }));
  });
  await step('adClients', async () => {
    const res = (await adsense.accounts.adclients.list({ parent, pageSize: 100 })).data;
    const clients = res.adClients ?? [];
    const result = [];
    for (const c of clients) {
      const units = (await adsense.accounts.adclients.adunits.list({ parent: c.name, pageSize: 100 })).data.adUnits ?? [];
      result.push({
        name: c.name, productCode: c.productCode, reportingDimensionId: c.reportingDimensionId, state: c.state,
        adUnits: units.map((u) => ({ name: u.name, displayName: u.displayName, state: u.state, contentAdsSettings: u.contentAdsSettings })),
      });
    }
    return result;
  });
  await step('alerts', async () => (await adsense.accounts.alerts.list({ parent })).data.alerts ?? []);
  await step('policyIssues', async () => (await adsense.accounts.policyIssues.list({ parent, pageSize: 100 })).data.policyIssues ?? []);

  const s = toDateParts(startDate); const e = toDateParts(endDate);
  async function report(dimensions) {
    const res = (await adsense.accounts.reports.generate({
      account: parent,
      dateRange: 'CUSTOM',
      'startDate.year': s.year, 'startDate.month': s.month, 'startDate.day': s.day,
      'endDate.year': e.year, 'endDate.month': e.month, 'endDate.day': e.day,
      dimensions, metrics: METRICS,
      languageCode: 'fr', limit: 100000,
    })).data;
    return flattenReport(res);
  }
  await step('byDayCountryPlatform', () => report(['DATE', 'COUNTRY_CODE', 'PLATFORM_TYPE_NAME']));
  await step('byDayPage', () => report(['DATE', 'PAGE_URL']));
  await step('byDayAdUnit', () => report(['DATE', 'AD_UNIT_NAME', 'AD_FORMAT_NAME']));

  return out;
}
