// Google Analytics 4 (site rtpcalculator.com) : trafic, pages, événements, sources.
// Rappel : les chiffres sont minorés par les refus de consentement (Consent Mode).

import { google } from 'googleapis';

const PAGE = 100000;

// Retrouve l'ID numérique de la propriété à partir du measurementId (G-...).
async function discoverProperty(auth, measurementId) {
  const admin = google.analyticsadmin({ version: 'v1beta', auth });
  const summaries = (await admin.accountSummaries.list({ pageSize: 200 })).data.accountSummaries ?? [];
  for (const acc of summaries) {
    for (const prop of acc.propertySummaries ?? []) {
      const streams = (await admin.properties.dataStreams.list({ parent: prop.property })).data.dataStreams ?? [];
      if (streams.some((s) => s.webStreamData?.measurementId === measurementId)) {
        return { propertyId: prop.property.replace('properties/', ''), displayName: prop.displayName, account: acc.displayName };
      }
    }
  }
  throw new Error(`Aucune propriété GA4 avec le flux ${measurementId} accessible à ce compte.`);
}

async function runReport(data, property, spec) {
  const rows = [];
  let offset = 0;
  let headers;
  for (;;) {
    const res = (await data.properties.runReport({
      property,
      requestBody: { ...spec, limit: PAGE, offset, keepEmptyRows: false },
    })).data;
    headers = headers ?? {
      dims: (res.dimensionHeaders ?? []).map((h) => h.name),
      mets: (res.metricHeaders ?? []).map((h) => h.name),
    };
    for (const r of res.rows ?? []) {
      const o = {};
      headers.dims.forEach((n, i) => { o[n] = r.dimensionValues[i].value; });
      headers.mets.forEach((n, i) => { o[n] = Number(r.metricValues[i].value); });
      rows.push(o);
    }
    offset += PAGE;
    if (!res.rows?.length || offset >= Number(res.rowCount ?? 0)) break;
  }
  return rows;
}

const dims = (...names) => names.map((name) => ({ name }));
const mets = (...names) => names.map((name) => ({ name }));

export async function fetchGa4(auth, config, { startDate, endDate }) {
  let { propertyId } = config.ga4;
  let discovered = null;
  if (!propertyId) {
    discovered = await discoverProperty(auth, config.ga4.measurementId);
    propertyId = discovered.propertyId;
  }
  const property = `properties/${propertyId}`;
  const data = google.analyticsdata({ version: 'v1beta', auth });
  const dateRanges = [{ startDate, endDate }];

  const [daily, pages, pagesMonthly, events, sources, languages, landing] = await Promise.all([
    runReport(data, property, {
      dateRanges,
      dimensions: dims('date', 'country', 'deviceCategory', 'sessionDefaultChannelGroup'),
      metrics: mets('activeUsers', 'newUsers', 'sessions', 'engagedSessions', 'screenPageViews', 'averageSessionDuration'),
    }),
    runReport(data, property, {
      dateRanges,
      dimensions: dims('pagePath', 'country'),
      metrics: mets('screenPageViews', 'activeUsers', 'averageSessionDuration', 'engagementRate'),
    }),
    runReport(data, property, {
      dateRanges,
      dimensions: dims('yearMonth', 'pagePath'),
      metrics: mets('screenPageViews', 'activeUsers'),
    }),
    runReport(data, property, {
      dateRanges,
      dimensions: dims('yearMonth', 'eventName'),
      metrics: mets('eventCount', 'totalUsers'),
    }),
    runReport(data, property, {
      dateRanges,
      dimensions: dims('yearMonth', 'sessionSource', 'sessionMedium'),
      metrics: mets('sessions', 'activeUsers', 'engagedSessions'),
    }),
    runReport(data, property, {
      dateRanges,
      dimensions: dims('language', 'country'),
      metrics: mets('activeUsers', 'sessions'),
    }),
    runReport(data, property, {
      dateRanges,
      dimensions: dims('landingPage', 'sessionDefaultChannelGroup'),
      metrics: mets('sessions', 'engagedSessions', 'bounceRate'),
    }),
  ]);

  return {
    property: { propertyId, ...(discovered ?? {}) },
    range: { startDate, endDate },
    daily,
    pages,
    pagesMonthly,
    events,
    sources,
    languages,
    landing,
  };
}
