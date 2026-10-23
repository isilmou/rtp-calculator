// Google Play : vitals (plantages, ANR) et problèmes via la Reporting API, rapports CSV
// mensuels (installations, notes, avis, performances de la fiche) via le bucket Cloud Storage,
// avis récents via l'API Android Publisher.

import fs from 'fs';
import path from 'path';
import { google } from 'googleapis';
import { toDateParts, fromDateParts, minYMD } from '../lib/dates.mjs';
import { decodeGoogleCsv, parseCsv } from '../lib/csv.mjs';

function flattenTimeline(rows) {
  return (rows ?? []).map((r) => {
    const o = { date: fromDateParts(r.startTime) };
    for (const d of r.dimensions ?? []) o[d.dimension] = d.stringValue ?? d.int64Value ?? d.value;
    for (const m of r.metrics ?? []) o[m.metric] = Number(m.decimalValue?.value ?? 0);
    return o;
  });
}

// Interroge un jeu de métriques vitals en respectant la fraîcheur annoncée par l'API.
const METRIC_SETS = { crashrate: 'crashRateMetricSet', anrrate: 'anrRateMetricSet' };

async function vitalsQuery(reporting, kind, appName, metrics, startDate, endDate) {
  const api = reporting.vitals[kind];
  const name = `${appName}/${METRIC_SETS[kind]}`;
  const meta = (await api.get({ name })).data;
  const daily = (meta.freshnessInfo?.freshnesses ?? []).find((f) => f.aggregationPeriod === 'DAILY');
  if (!daily) return { note: 'aucune donnée quotidienne disponible (seuil d\'utilisateurs non atteint)', rows: [] };
  const latest = daily.latestEndTime;
  const tz = latest.timeZone ?? { id: 'America/Los_Angeles' };
  const end = toDateParts(minYMD(endDate, fromDateParts(latest)));
  const rows = [];
  let pageToken;
  do {
    const res = (await api.query({
      name,
      requestBody: {
        timelineSpec: {
          aggregationPeriod: 'DAILY',
          startTime: { ...toDateParts(startDate), timeZone: tz },
          endTime: { ...end, timeZone: tz },
        },
        dimensions: ['versionCode'],
        metrics,
        pageSize: 1000,
        pageToken,
      },
    })).data;
    rows.push(...flattenTimeline(res.rows));
    pageToken = res.nextPageToken;
  } while (pageToken);
  return { latestAvailable: fromDateParts(latest), rows };
}

// Télécharge les CSV du bucket (cache local dans data/play-raw/, re-téléchargé si la taille change).
async function fetchBucketCsvs(auth, config, rawDir) {
  const storage = google.storage({ version: 'v1', auth });
  const { bucket, packageName } = config.play;
  const items = [];
  let pageToken;
  do {
    const res = (await storage.objects.list({ bucket, pageToken, maxResults: 1000 })).data;
    items.push(...(res.items ?? []));
    pageToken = res.nextPageToken;
  } while (pageToken);

  const csvs = items.filter((o) => o.name.endsWith('.csv') && o.name.includes(packageName));
  fs.mkdirSync(rawDir, { recursive: true });
  const reports = {};
  const pkg = packageName.replace(/\./g, '\\.');
  const re = new RegExp(`^(.+?)_${pkg}_(\\d{6})(?:_(.+))?\\.csv$`);

  for (const obj of csvs) {
    const local = path.join(rawDir, obj.name.replace(/\//g, '__'));
    const stale = !fs.existsSync(local) || String(fs.statSync(local).size) !== String(obj.size);
    if (stale) {
      const res = await storage.objects.get({ bucket, object: obj.name, alt: 'media' }, { responseType: 'arraybuffer' });
      fs.writeFileSync(local, Buffer.from(res.data));
    }
    const base = path.basename(obj.name);
    const m = base.match(re);
    const type = m ? `${m[1]}${m[3] ? '_' + m[3] : ''}` : path.dirname(obj.name).replace(/\//g, '_');
    const month = m ? m[2] : null;
    const rows = parseCsv(decodeGoogleCsv(fs.readFileSync(local)));
    (reports[type] ??= []).push(...rows.map((r) => ({ _month: month, ...r })));
  }
  return { fileCount: csvs.length, allObjects: items.map((o) => o.name), reports };
}

export async function fetchPlay(auth, config, { startDate, endDate, dataRoot }) {
  const { packageName } = config.play;
  const appName = `apps/${packageName}`;
  const reporting = google.playdeveloperreporting({ version: 'v1beta1', auth });
  const publisher = google.androidpublisher({ version: 'v3', auth });
  const out = { packageName, range: { startDate, endDate }, errors: {} };

  const step = async (key, fn) => {
    try { out[key] = await fn(); }
    catch (err) { out.errors[key] = err.errors?.[0]?.message ?? err.message; }
  };

  await step('crashRate', () => vitalsQuery(reporting, 'crashrate', appName,
    ['crashRate', 'userPerceivedCrashRate', 'distinctUsers'], startDate, endDate));
  await step('anrRate', () => vitalsQuery(reporting, 'anrrate', appName,
    ['anrRate', 'userPerceivedAnrRate', 'distinctUsers'], startDate, endDate));
  await step('issues', async () => {
    const s = toDateParts(startDate); const e = toDateParts(endDate);
    const res = (await reporting.vitals.errors.issues.search({
      parent: appName, pageSize: 100,
      'interval.startTime.year': s.year, 'interval.startTime.month': s.month, 'interval.startTime.day': s.day,
      'interval.endTime.year': e.year, 'interval.endTime.month': e.month, 'interval.endTime.day': e.day,
    })).data;
    return res.errorIssues ?? [];
  });
  await step('recentReviews', async () => {
    const res = (await publisher.reviews.list({ packageName, maxResults: 100 })).data;
    return (res.reviews ?? []).map((r) => {
      const c = r.comments?.[0]?.userComment ?? {};
      return {
        reviewId: r.reviewId, author: r.authorName, starRating: c.starRating, text: c.text,
        lastModified: c.lastModified?.seconds ? new Date(Number(c.lastModified.seconds) * 1000).toISOString() : null,
        appVersionCode: c.appVersionCode, appVersionName: c.appVersionName,
        reviewerLanguage: c.reviewerLanguage, device: c.device, androidOsVersion: c.androidOsVersion,
        developerReply: r.comments?.find((x) => x.developerComment)?.developerComment?.text ?? null,
      };
    });
  });
  await step('bucket', () => fetchBucketCsvs(auth, config, path.join(dataRoot, 'play-raw')));

  return out;
}
