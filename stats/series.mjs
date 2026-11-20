// Séries d'évolution (mois, semaines, avant / après une date) à partir d'un relevé complet, en Markdown.
//
//   npm run series                                   dernier relevé (data/latest.json), à collecter avec --all
//   npm run series -- --dir=data/2026-11-20
//   npm run series -- --pivot=2026-10-25 --window=28 ajoute un tableau avant / après cette date
//   npm run series -- --exclude=2026-10-27           jours d'anomalie Search Console (défaut : 2026-10-27)
//   npm run series -- --top=15                       nombre de pages, requêtes et pays détaillés
//
// Lecture seule des JSON collectés par snapshot.mjs ; aucune requête réseau.
// Totaux Search Console : toujours depuis `daily` ou `dailyPage` (les requêtes rares sont anonymisées).

import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import { addDays, parseYMD } from './lib/dates.mjs';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const args = Object.fromEntries(process.argv.slice(2).map((a) => { const m = a.match(/^--([^=]+)(?:=(.*))?$/); return m ? [m[1], m[2] ?? true] : [a, true]; }));

const dataRoot = path.join(__dirname, 'data');
const latest = JSON.parse(fs.readFileSync(path.join(dataRoot, 'latest.json'), 'utf8'));
const dir = path.resolve(__dirname, args.dir ?? latest.dir);
const status = JSON.parse(fs.readFileSync(path.join(dir, '_status.json'), 'utf8'));
const config = JSON.parse(fs.readFileSync(path.join(__dirname, 'config.json'), 'utf8'));
const load = (name, d = dir) => { const f = path.join(d, `${name}.json`); return fs.existsSync(f) ? JSON.parse(fs.readFileSync(f, 'utf8')) : null; };
const EXCLUDE = new Set(String(args.exclude ?? '2026-10-27').split(',').filter(Boolean));
const TOP = Number(args.top ?? 12);

// ---- utilitaires ----
const ymd = (d) => { const s = String(d); return s.includes('-') ? s.slice(0, 10) : `${s.slice(0, 4)}-${s.slice(4, 6)}-${s.slice(6, 8)}`; };
const monthOf = (d) => d.slice(0, 7);
const weekOf = (d) => addDays(d, -((parseYMD(d).getUTCDay() + 6) % 7));          // lundi de la semaine
const num = (v) => Number(v) || 0;
const n = (v, dec = 0) => Number(v).toLocaleString('fr-FR', { minimumFractionDigits: dec, maximumFractionDigits: dec });
const pct = (a, b) => (b ? `${n(a / b * 100, 1)} %` : '');
const eur = (v) => `${n(v, 2)} €`;
const out = [];
const h = (t, lvl = 2) => out.push('', `${'#'.repeat(lvl)} ${t}`, '');
const table = (head, rows) => { out.push(`| ${head.join(' | ')} |`, `|${head.map(() => '---').join('|')}|`); for (const r of rows) out.push(`| ${r.join(' | ')} |`); };
const note = (t) => out.push('', t);
const short = (u) => u.replace('https://rtpcalculator.com', '') || '/';

// Regroupe des lignes journalières { date, ...mesures } par clé de période et additionne les mesures.
function bucket(rows, keyOf, fields) {
  const m = new Map();
  for (const r of rows) {
    const k = keyOf(r.date);
    const b = m.get(k) ?? Object.fromEntries([['key', k], ['days', new Set()], ...fields.map((f) => [f, 0])]);
    for (const f of fields) b[f] += num(r[f]);
    b.days.add(r.date);
    m.set(k, b);
  }
  return [...m.values()].sort((a, b) => a.key.localeCompare(b.key));
}
const pivotBy = (rows, rowKey, colKey, field) => {
  const m = new Map();
  for (const r of rows) { const o = m.get(r[rowKey]) ?? {}; o[r[colKey]] = (o[r[colKey]] ?? 0) + num(r[field]); m.set(r[rowKey], o); }
  return m;
};
const total = (o) => Object.values(o).reduce((a, b) => a + b, 0);
const rangeLabel = (name) => { const r = status.sources[name]?.range ?? status.range; return `${r.startDate} → ${r.endDate}`; };

out.push('# RTP Calculator : séries d\'évolution', '', `Relevé \`${path.relative(__dirname, dir)}\`, généré ${status.generatedAt.slice(0, 16).replace('T', ' ')} UTC. Semaines commençant le lundi ; la dernière période de chaque tableau est incomplète.`);

const windows = [];   // tableau avant / après, rempli par chaque source si --pivot
const pivot = args.pivot && args.pivot !== true ? String(args.pivot) : null;
const win = Number(args.window ?? 28);
const inWin = (d, side) => (side === 'avant' ? d >= addDays(pivot, -win) && d < pivot : d >= pivot && d < addDays(pivot, win));

// ================= Play =================
const play = load('play');
if (play?.bucket?.reports) {
  const r = play.bucket.reports;
  const ov = (r.installs_overview ?? []).map((x) => ({ date: ymd(x.Date), installs: num(x['Daily User Installs']), uninstalls: num(x['Daily User Uninstalls']), active: num(x['Active Device Installs']) }));
  const sp = (r.store_performance_traffic_source ?? []).map((x) => ({ date: ymd(x.Date), visitors: num(x['Store listing visitors']), acquisitions: num(x['Store listing acquisitions']) }));
  const activeAt = new Map(ov.map((x) => [x.date, x.active]));
  const playRows = (keyOf) => {
    const a = bucket(ov, keyOf, ['installs', 'uninstalls']);
    const b = new Map(bucket(sp, keyOf, ['visitors', 'acquisitions']).map((x) => [x.key, x]));
    let cumI = 0, cumU = 0;
    return a.map((x) => {
      cumI += x.installs; cumU += x.uninstalls;
      const last = [...x.days].sort().at(-1); const s = b.get(x.key) ?? { visitors: 0, acquisitions: 0 };
      return [x.key, n(x.installs), n(x.uninstalls), pct(x.uninstalls, x.installs), n(x.installs - x.uninstalls), n(activeAt.get(last)), n(s.visitors), n(s.acquisitions), pct(s.acquisitions, s.visitors), n(cumI), n(cumU)];
    });
  };
  const head = ['Période', 'Installs', 'Désinstalls', 'Désinst. / inst.', 'Solde', 'Actifs fin', 'Visiteurs fiche', 'Acquisitions', 'Conversion', 'Cumul inst.', 'Cumul désinst.'];
  h(`Play Console (app Android), ${rangeLabel('play')}`);
  h('Par mois', 3); table(head, playRows(monthOf));
  h('Par semaine', 3); table(head, playRows(weekOf));

  // Pays × mois
  const cRows = (r.installs_country ?? []).map((x) => ({ country: x.Country, month: monthOf(ymd(x.Date)), installs: num(x['Daily User Installs']), uninstalls: num(x['Daily User Uninstalls']) }));
  const months = [...new Set(cRows.map((x) => x.month))].sort();
  const byC = pivotBy(cRows, 'country', 'month', 'installs');
  const byCU = pivotBy(cRows, 'country', 'month', 'uninstalls');
  const topC = [...byC.entries()].map(([k, o]) => [k, o, total(o)]).filter((x) => x[2] > 0).sort((a, b) => b[2] - a[2]);
  h('Installations par pays et par mois (désinstallations entre parenthèses)', 3);
  table(['Pays', ...months, 'Total'], topC.slice(0, TOP).map(([k, o, t]) => [k, ...months.map((m) => (o[m] || byCU.get(k)?.[m] ? `${o[m] ?? 0} (${byCU.get(k)?.[m] ?? 0})` : '')), `${t} (${total(byCU.get(k) ?? {})})`]));
  note(`${topC.length} pays avec au moins une installation ; hors tableau : ${n(topC.slice(TOP).reduce((a, x) => a + x[2], 0))} installations.`);

  // Langues
  const lRows = (r.installs_language ?? []).map((x) => ({ k: x.Language, month: monthOf(ymd(x.Date)), installs: num(x['Daily User Installs']) }));
  const byL = [...pivotBy(lRows, 'k', 'month', 'installs').entries()].map(([k, o]) => [k, o, total(o)]).filter((x) => x[2] > 0).sort((a, b) => b[2] - a[2]);
  h('Installations par langue système et par mois', 3);
  table(['Langue', ...months, 'Total'], byL.slice(0, TOP).map(([k, o, t]) => [k || '(non renseignée)', ...months.map((m) => o[m] || ''), t]));

  // Parc au dernier jour connu : version de l'app, version d'Android, appareil
  const park = (rows, key, label) => {
    const dates = [...new Set(rows.map((x) => ymd(x.Date)))].sort();
    const lastD = dates.filter((d) => rows.some((x) => ymd(x.Date) === d && num(x['Active Device Installs']) > 0)).at(-1);
    if (!lastD) return;
    const list = rows.filter((x) => ymd(x.Date) === lastD && num(x['Active Device Installs']) > 0).map((x) => [x[key], num(x['Active Device Installs'])]).sort((a, b) => b[1] - a[1]);
    note(`**${label}** (appareils actifs le ${lastD}) : ${list.map(([k, v]) => `${k} ${v}`).join(' · ')}.`);
  };
  h('Parc actif', 3);
  park(r.installs_app_version ?? [], 'App Version Code', 'Code de version');
  park(r.installs_os_version ?? [], 'Android OS Version', 'API Android');
  park(r.installs_device ?? [], 'Device', 'Appareil');
  park(r.installs_country ?? [], 'Country', 'Pays');
  const rat = (r.ratings_overview ?? []).filter((x) => x['Total Average Rating']).sort((a, b) => a.Date.localeCompare(b.Date)).at(-1);
  note(`Note cumulée : ${rat ? `${rat['Total Average Rating']} (${rat.Date})` : 'n/d'} ; avis : ${(r.reviews ?? []).map((x) => `${x['Star Rating']} étoiles le ${String(x['Review Last Update Date and Time']).slice(0, 10)} (v${x['App Version Name']}, ${x['Reviewer Language']})`).join(', ') || 'aucun'} ; vitals : ${play.crashRate?.rows?.length ? 'renseignés' : 'vides'} ; problèmes signalés : ${(play.issues ?? []).length}.`);

  if (pivot) for (const side of ['avant', 'après']) {
    const a = ov.filter((x) => inWin(x.date, side)); const s = sp.filter((x) => inWin(x.date, side));
    const c = (r.installs_country ?? []).filter((x) => inWin(ymd(x.Date), side) && num(x['Daily User Installs']) > 0);
    const cc = {}; for (const x of c) cc[x.Country] = (cc[x.Country] ?? 0) + num(x['Daily User Installs']);
    const I = a.reduce((t, x) => t + x.installs, 0), U = a.reduce((t, x) => t + x.uninstalls, 0);
    const V = s.reduce((t, x) => t + x.visitors, 0), A = s.reduce((t, x) => t + x.acquisitions, 0);
    windows.push([`Play, ${side}`, `${a.length} j`, `installs ${I}, désinstalls ${U} (${pct(U, I)}), fiche ${V} visiteurs / ${A} acquisitions (${pct(A, V)}), pays : ${Object.entries(cc).sort((x, y) => y[1] - x[1]).map(([k, v]) => `${k} ${v}`).join(' ')}`]);
  }
}

// ================= AdMob =================
const admob = load('admob');
if (admob?.byDayApp) {
  const d = admob.byDayApp.map((x) => ({ date: ymd(x.DATE), requests: num(x.AD_REQUESTS), impressions: num(x.IMPRESSIONS), clicks: num(x.CLICKS), earnings: num(x.ESTIMATED_EARNINGS) }));
  const daysIn = (key, isMonth) => { if (!isMonth) return 7; const [y, m] = key.split('-').map(Number); return new Date(Date.UTC(y, m, 0)).getUTCDate(); };
  const rows = (keyOf, isMonth) => { let cum = 0; return bucket(d, keyOf, ['requests', 'impressions', 'clicks', 'earnings']).map((x) => { cum += x.earnings; return [x.key, n(x.days.size), n(x.requests), n(x.requests / daysIn(x.key, isMonth), 1), n(x.impressions), n(x.clicks), eur(x.earnings), x.impressions ? eur(x.earnings / x.impressions * 1000) : '', eur(cum)]; }); };
  const head = ['Période', 'Jours avec requêtes', 'Requêtes', 'Requêtes / jour calendaire', 'Impressions', 'Clics', 'Revenus', 'eCPM', 'Cumul'];
  h(`AdMob (bannière de l'app), ${rangeLabel('admob')}`);
  note('Les requêtes servent de proxy d\'ouverture de l\'app. La dernière période est rapportée à sa durée complète, donc minorée.');
  h('Par mois', 3); table(head, rows(monthOf, true));
  h('Par semaine', 3); table(head, rows(weekOf, false));
  const cr = (admob.byDayCountryAdUnitFormat ?? []).map((x) => ({ country: x.COUNTRY, month: monthOf(ymd(x.DATE)), requests: num(x.AD_REQUESTS), earnings: num(x.ESTIMATED_EARNINGS) }));
  const months = [...new Set(cr.map((x) => x.month))].sort();
  const byReq = pivotBy(cr, 'country', 'month', 'requests'); const byEarn = pivotBy(cr, 'country', 'month', 'earnings');
  h('Requêtes par pays et par mois', 3);
  table(['Pays', ...months, 'Total', 'Revenus'], [...byReq.entries()].map(([k, o]) => [k, o, total(o)]).sort((a, b) => b[2] - a[2]).slice(0, TOP).map(([k, o, t]) => [k, ...months.map((m) => o[m] || ''), t, eur(total(byEarn.get(k) ?? {}))]));
  if (pivot) for (const side of ['avant', 'après']) { const a = d.filter((x) => inWin(x.date, side)); const R = a.reduce((t, x) => t + x.requests, 0); windows.push([`AdMob, ${side}`, `${win} j`, `requêtes ${R} (${n(R / win, 1)} / jour), revenus ${eur(a.reduce((t, x) => t + x.earnings, 0))}`]); }
}

// ================= AdSense =================
const adsense = load('adsense');
if (adsense?.account) {
  h('AdSense (site)');
  note(`Compte ${adsense.account.state} ; ${(adsense.sites ?? []).map((s) => `${s.domain} : ${s.state}`).join(', ')} ; blocs : ${(adsense.adClients ?? []).reduce((a, c) => a + (c.adUnits?.length ?? 0), 0)} ; lignes de rapport : ${adsense.byDayCountryPlatform?.rows?.length ?? 0} ; alertes : ${(adsense.alerts ?? []).map((a) => a.type).join(', ') || 'aucune'}.`);
}

// ================= Search Console =================
const gsc = load('gsc');
if (gsc?.daily) {
  const d = gsc.daily.map((x) => ({ ...x, w: x.position * x.impressions }));
  const gRows = (keyOf, rowsIn = d) => { let cum = 0; return bucket(rowsIn, keyOf, ['clicks', 'impressions', 'w']).map((x) => { cum += x.clicks; return [x.key, n(x.impressions), n(x.clicks), pct(x.clicks, x.impressions), x.impressions ? n(x.w / x.impressions, 1) : '', n(cum)]; }); };
  const head = ['Période', 'Impressions', 'Clics', 'CTR', 'Position', 'Cumul clics'];
  const clean = d.filter((x) => !EXCLUDE.has(x.date));
  h(`Search Console (site), ${rangeLabel('gsc')}`);
  const ex = d.filter((x) => EXCLUDE.has(x.date));
  note(`Jours d'anomalie retirés des tableaux « hors anomalies » : ${ex.map((x) => `${x.date} (${x.impressions} impressions, ${x.clicks} clics, position ${n(x.position, 1)})`).join(', ') || 'aucun'}.`);
  h('Par mois, hors anomalies', 3); table(head, gRows(monthOf, clean));
  h('Par mois, brut', 3); table(head, gRows(monthOf));
  h('Par semaine, hors anomalies', 3); table(head, gRows(weekOf, clean));

  const lastMonths = [...new Set(d.map((x) => monthOf(x.date)))].sort().slice(-4);
  const cell = (o) => (o && o.impressions ? `${o.clicks} / ${o.impressions} / ${n(o.w / o.impressions, 1)}` : '');
  const detail = (rows, key, title, fmt = (k) => k) => {
    const m = new Map();
    for (const x of rows) {
      if (EXCLUDE.has(x.date)) continue;
      const o = m.get(x[key]) ?? { all: { clicks: 0, impressions: 0, w: 0 } };
      const mo = monthOf(x.date); const c = o[mo] ??= { clicks: 0, impressions: 0, w: 0 };
      for (const t of [c, o.all]) { t.clicks += x.clicks; t.impressions += x.impressions; t.w += x.position * x.impressions; }
      m.set(x[key], o);
    }
    const sorted = [...m.entries()].sort((a, b) => b[1].all.impressions - a[1].all.impressions);
    h(`${title} : clics / impressions / position, hors anomalies`, 3);
    table([title.split(' ')[0], ...lastMonths, 'Toute la période'], sorted.slice(0, TOP).map(([k, o]) => [fmt(k), ...lastMonths.map((mo) => cell(o[mo])), cell(o.all)]));
    note(`${sorted.length} lignes au total ; hors tableau : ${n(sorted.slice(TOP).reduce((a, x) => a + x[1].all.impressions, 0))} impressions, ${n(sorted.slice(TOP).reduce((a, x) => a + x[1].all.clicks, 0))} clics.`);
  };
  if (gsc.dailyPage) detail(gsc.dailyPage, 'page', 'Pages', short);
  if (gsc.dailyQuery) { detail(gsc.dailyQuery, 'query', 'Requêtes connues (les requêtes rares sont anonymisées, totaux partiels)'); }
  if (gsc.dailyCountryDevice) { detail(gsc.dailyCountryDevice, 'country', 'Pays'); detail(gsc.dailyCountryDevice, 'device', 'Appareils'); }
  const sm = gsc.sitemaps?.[0];
  if (sm) note(`Sitemap : soumis le ${sm.lastSubmitted?.slice(0, 10)}, téléchargé le ${sm.lastDownloaded?.slice(0, 10)}, ${sm.contents?.[0]?.submitted} URL, ${sm.errors} erreur(s), ${sm.warnings} avertissement(s).`);
  if (pivot) for (const side of ['avant', 'après']) { const a = clean.filter((x) => inWin(x.date, side)); const I = a.reduce((t, x) => t + x.impressions, 0), C = a.reduce((t, x) => t + x.clicks, 0); windows.push([`Search Console, ${side}`, `${a.length} j`, `impressions ${I}, clics ${C}, CTR ${pct(C, I)}, position ${I ? n(a.reduce((t, x) => t + x.w, 0) / I, 1) : ''}`]); }
}

// ================= GA4 =================
const ga4 = load('ga4');
if (ga4?.daily) {
  const d = ga4.daily.map((x) => ({ ...x, date: ymd(x.date) }));
  const chans = [...new Set(d.map((x) => x.sessionDefaultChannelGroup))];
  const gaRows = (keyOf) => {
    const m = new Map();
    for (const x of d) {
      const k = keyOf(x.date); const o = m.get(k) ?? { sessions: 0, engaged: 0, mobile: 0, views: 0, ch: {} };
      o.sessions += x.sessions; o.engaged += x.engagedSessions; o.views += x.screenPageViews; if (x.deviceCategory !== 'desktop') o.mobile += x.sessions;
      o.ch[x.sessionDefaultChannelGroup] = (o.ch[x.sessionDefaultChannelGroup] ?? 0) + x.sessions; m.set(k, o);
    }
    return [...m.entries()].sort((a, b) => a[0].localeCompare(b[0])).map(([k, o]) => [k, n(o.sessions), n(o.engaged), n(o.views), n(o.mobile), ...chans.map((c) => o.ch[c] || '')]);
  };
  const head = ['Période', 'Sessions', 'Engagées', 'Pages vues', 'Mobile et tablette', ...chans];
  h(`Analytics GA4 (site, minoré par le Consent Mode), ${rangeLabel('ga4')}`);
  note(`Robots probables : ${n((ga4.tech ?? []).filter((t) => t.browser === '(not set)').reduce((a, t) => a + t.sessions, 0))} sessions à navigateur « (not set) », concentrées sur le canal Direct. Le canal Organic Search est la lecture fiable.`);
  h('Par mois', 3); table(head, gaRows(monthOf));
  h('Par semaine', 3); table(head, gaRows(weekOf));
  if (ga4.events) {
    const months = [...new Set(ga4.events.map((x) => x.yearMonth))].sort();
    const ev = pivotBy(ga4.events, 'eventName', 'yearMonth', 'eventCount'); const us = pivotBy(ga4.events, 'eventName', 'yearMonth', 'totalUsers');
    h('Événements par mois : nombre (utilisateurs)', 3);
    table(['Événement', ...months.map((m) => `${m.slice(0, 4)}-${m.slice(4)}`)], [...ev.entries()].sort((a, b) => total(b[1]) - total(a[1])).map(([k, o]) => [k, ...months.map((m) => (o[m] ? `${o[m]} (${us.get(k)?.[m] ?? 0})` : ''))]));
  }
  if (ga4.pagesMonthly) {
    const months = [...new Set(ga4.pagesMonthly.map((x) => x.yearMonth))].sort().slice(-4);
    const pv = pivotBy(ga4.pagesMonthly, 'pagePath', 'yearMonth', 'screenPageViews');
    h('Pages vues par page, 4 derniers mois', 3);
    table(['Page', ...months.map((m) => `${m.slice(0, 4)}-${m.slice(4)}`)], [...pv.entries()].map(([k, o]) => [k, o, months.reduce((a, m) => a + (o[m] ?? 0), 0)]).filter((x) => x[2] > 0).sort((a, b) => b[2] - a[2]).slice(0, TOP).map(([k, o]) => [k, ...months.map((m) => o[m] || '')]));
  }
  if (ga4.tech) {
    h('Appareils, navigateurs et systèmes (toute la période)', 3);
    table(['Appareil', 'Navigateur', 'Système', 'Sessions'], [...ga4.tech].sort((a, b) => b.sessions - a.sessions).map((t) => [t.deviceCategory, t.browser, t.operatingSystem, t.sessions]));
  }
  if (ga4.playClicks?.length) note(`Clics sortants vers Google Play : ${ga4.playClicks.map((x) => `${ymd(x.date)} ${x.pagePath} (${x.country}${/utm_/.test(decodeURIComponent(x.linkUrl)) ? ', lien UTM' : ''})`).join(' · ')}.`);
  if (pivot) for (const side of ['avant', 'après']) { const a = d.filter((x) => inWin(x.date, side)); const org = a.filter((x) => x.sessionDefaultChannelGroup === 'Organic Search'); windows.push([`GA4, ${side}`, `${win} j`, `sessions ${a.reduce((t, x) => t + x.sessions, 0)}, dont organiques ${org.reduce((t, x) => t + x.sessions, 0)}`]); }
}

// ================= Indexation =================
const inspect = load('inspect');
if (inspect?.urls) {
  const extra = new Set(config.inspect?.extraUrls ?? []);
  const current = inspect.urls.filter((u) => !extra.has(u.url));
  const currentSet = new Set(current.map((u) => u.url));
  const snaps = fs.readdirSync(dataRoot).filter((x) => /^\d{4}-\d{2}-\d{2}$/.test(x)).sort().map((x) => [x, load('inspect', path.join(dataRoot, x))]).filter(([x, v]) => v?.urls && path.join(dataRoot, x) <= dir);
  const states = [...new Set(snaps.flatMap(([, v]) => v.urls.filter((u) => currentSet.has(u.url)).map((u) => u.coverageState)))];
  h(`Indexation (URL Inspection), ${current.length} pages du sitemap actuel`);
  table(['État', ...snaps.map(([x]) => x)], [
    ...states.map((s) => [s, ...snaps.map(([, v]) => v.urls.filter((u) => currentSet.has(u.url) && u.coverageState === s).length)]),
    ['Absente du relevé', ...snaps.map(([, v]) => current.length - v.urls.filter((u) => currentSet.has(u.url)).length)],
  ]);
  const crawl = (u) => (u.lastCrawlTime ? u.lastCrawlTime.slice(0, 10) : 'jamais');
  h('Pages du sitemap non indexées', 3);
  table(['URL', 'État', 'Dernier crawl'], current.filter((u) => u.verdict !== 'PASS').map((u) => [short(u.url), u.coverageState, crawl(u)]));
  const byWeek = {}; for (const u of current) { const k = u.lastCrawlTime ? weekOf(crawl(u)) : 'jamais'; byWeek[k] = (byWeek[k] ?? 0) + 1; }
  h('Fraîcheur du crawl : pages par semaine du dernier passage de Google', 3);
  table(['Semaine du dernier crawl', 'Pages'], Object.entries(byWeek).sort((a, b) => a[0].localeCompare(b[0])).map(([k, v]) => [k, v]));
  note(`Dix crawls les plus anciens : ${[...current].filter((u) => u.lastCrawlTime).sort((a, b) => a.lastCrawlTime.localeCompare(b.lastCrawlTime)).slice(0, 10).map((u) => `${short(u.url)} ${crawl(u)}`).join(' · ')}.`);
  const ext = inspect.urls.filter((u) => extra.has(u.url));
  if (ext.length) { h('URL hors sitemap suivies (variantes et anciennes pages en 301)', 3); table(['URL', 'État', 'Dernier crawl', 'Canonical Google'], ext.map((u) => [u.url.replace('https://rtpcalculator.com', ''), u.coverageState, crawl(u), u.googleCanonical ? short(u.googleCanonical) : ''])); }
}

if (pivot && windows.length) { h(`Avant / après le ${pivot} (${win} jours de part et d'autre, jour du pivot dans « après »)`); table(['Source', 'Durée', 'Valeurs'], windows); }

out.push('');
process.stdout.write(out.join('\n'));
