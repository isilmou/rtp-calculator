// AdMob : rapport réseau (revenus, requêtes, impressions, eCPM, clics)
// par jour × pays × bloc d'annonces × format.

import { google } from 'googleapis';
import { toDateParts } from '../lib/dates.mjs';

const METRICS = [
  'ESTIMATED_EARNINGS', 'AD_REQUESTS', 'MATCHED_REQUESTS', 'IMPRESSIONS', 'CLICKS',
  'MATCH_RATE', 'SHOW_RATE', 'IMPRESSION_CTR', 'IMPRESSION_RPM',
];

function flattenRows(response) {
  // La réponse est une liste : [{ header }, { row }, ..., { footer }].
  const rows = [];
  let header = null;
  let footer = null;
  for (const item of response ?? []) {
    if (item.header) header = item.header;
    else if (item.footer) footer = item.footer;
    else if (item.row) {
      const r = {};
      for (const [k, v] of Object.entries(item.row.dimensionValues ?? {})) {
        r[k] = v.displayLabel && v.displayLabel !== v.value ? `${v.value} (${v.displayLabel})` : v.value;
      }
      for (const [k, v] of Object.entries(item.row.metricValues ?? {})) {
        if (v.microsValue !== undefined) r[k] = Number(v.microsValue) / 1e6;
        else if (v.integerValue !== undefined) r[k] = Number(v.integerValue);
        else if (v.doubleValue !== undefined) r[k] = Number(v.doubleValue);
      }
      rows.push(r);
    }
  }
  return { header, footer, rows };
}

export async function fetchAdmob(auth, config, { startDate, endDate }) {
  const admob = google.admob({ version: 'v1', auth });
  const parent = `accounts/${config.admob.publisherId}`;

  const account = (await admob.accounts.get({ name: parent })).data;

  async function report(dimensions) {
    const res = await admob.accounts.networkReport.generate({
      parent,
      requestBody: {
        reportSpec: {
          dateRange: { startDate: toDateParts(startDate), endDate: toDateParts(endDate) },
          dimensions,
          metrics: METRICS,
          localizationSettings: { currencyCode: account.currencyCode, languageCode: 'fr' },
        },
      },
    });
    return flattenRows(res.data);
  }

  const daily = await report(['DATE', 'COUNTRY', 'AD_UNIT', 'FORMAT']);
  const apps = await report(['DATE', 'APP', 'PLATFORM']);

  return {
    account: {
      publisherId: account.publisherId,
      currencyCode: account.currencyCode,
      reportingTimeZone: account.reportingTimeZone,
    },
    range: { startDate, endDate },
    byDayCountryAdUnitFormat: daily.rows,
    byDayApp: apps.rows,
    footer: daily.footer,
  };
}
