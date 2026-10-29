// Chiffres clés du dernier relevé (ou d'un dossier donné), en Markdown sur la sortie standard.
//
//   npm run summary                      dernier relevé (data/latest.json)
//   npm run summary -- --dir=data/2026-10-25
//
// Lecture seule des JSON collectés par snapshot.mjs ; aucune requête réseau.

import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const args = Object.fromEntries(process.argv.slice(2).map((a) => { const m = a.match(/^--([^=]+)(?:=(.*))?$/); return m ? [m[1], m[2] ?? true] : [a, true]; }));

const latest = JSON.parse(fs.readFileSync(path.join(__dirname, 'data', 'latest.json'), 'utf8'));
const dir = path.resolve(__dirname, args.dir ?? latest.dir);
const status = JSON.parse(fs.readFileSync(path.join(dir, '_status.json'), 'utf8'));
// Période propre à chaque source (plusieurs collectes peuvent cohabiter dans un dossier du jour).
const rangeOf = (name) => status.sources[name]?.range ?? status.range;
const label = (r) => `${r.startDate} → ${r.endDate}`;
const load = (name) => { const f = path.join(dir, `${name}.json`); return fs.existsSync(f) ? JSON.parse(fs.readFileSync(f, 'utf8')) : null; };

const ymd = (d) => String(d).replace(/-/g, '').slice(0, 8);          // 2026-10-01 | 20261001 -> 20261001
const inRangeOf = (r) => (d) => { const x = ymd(d); return x >= ymd(r.startDate) && x <= ymd(r.endDate); };
const sum = (rows, k) => rows.reduce((a, r) => a + (Number(r[k]) || 0), 0);
const n = (v, dec = 0) => Number(v).toLocaleString('fr-FR', { minimumFractionDigits: dec, maximumFractionDigits: dec });
const pct = (v) => `${n(v * 100, 1)} %`;
const eur = (v) => `${n(v, 2)} €`;
const out = [];
const h = (t) => out.push('', `## ${t}`, '');
const table = (head, rows) => { out.push(`| ${head.join(' | ')} |`, `|${head.map(() => '---').join('|')}|`); for (const r of rows) out.push(`| ${r.join(' | ')} |`); };
out.push('# RTP Calculator : chiffres clés', '', `Relevé \`${path.relative(__dirname, dir)}\`, généré ${status.generatedAt.slice(0, 16).replace('T', ' ')} UTC. Période indiquée par source.`);
const failed = Object.entries(status.sources).filter(([, s]) => !s.ok).map(([k, s]) => `${k} (${s.error})`);
if (failed.length) out.push('', `**Sources en échec** : ${failed.join(', ')}.`);

// ---- Play ----
const play = load('play');
if (play?.bucket?.reports) {
  const r = play.bucket.reports;
  const allOv = r.installs_overview ?? [];
  const allSp = r.store_performance_traffic_source ?? [];
  const inRange = inRangeOf(rangeOf('play'));
  const ov = allOv.filter((x) => inRange(x.Date));
  const sp = allSp.filter((x) => inRange(x.Date));
  const rat = (r.ratings_overview ?? []).filter((x) => x['Total Average Rating']).sort((a, b) => a.Date.localeCompare(b.Date));
  const lastOv = [...ov].sort((a, b) => a.Date.localeCompare(b.Date)).at(-1);
  h(`Play Console (app Android), ${label(rangeOf('play'))}`);
  table(['Indicateur', 'Période', 'Depuis le début'], [
    ['Installations (utilisateurs)', n(sum(ov, 'Daily User Installs')), n(sum(allOv, 'Daily User Installs'))],
    ['Désinstallations (utilisateurs)', n(sum(ov, 'Daily User Uninstalls')), n(sum(allOv, 'Daily User Uninstalls'))],
    ['Appareils actifs (dernier jour connu)', lastOv ? `${n(lastOv['Active Device Installs'])} (${lastOv.Date})` : 'n/d', ''],
    ['Visiteurs de la fiche', n(sum(sp, 'Store listing visitors')), n(sum(allSp, 'Store listing visitors'))],
    ['Acquisitions depuis la fiche', n(sum(sp, 'Store listing acquisitions')), n(sum(allSp, 'Store listing acquisitions'))],
    ['Note moyenne cumulée', rat.length ? `${n(rat.at(-1)['Total Average Rating'], 2)} (${rat.at(-1).Date})` : 'n/d', ''],
    ['Avis (CSV / API)', `${(r.reviews ?? []).length} / ${(play.recentReviews ?? []).length}`, ''],
  ]);
  const crash = play.crashRate?.rows?.length ?? 0;
  out.push('', `Vitals : ${crash ? `${crash} lignes de taux de plantage` : "vides (app sous le seuil d'utilisateurs de Google)"} ; problèmes signalés : ${(play.issues ?? []).length}.`);
  const byCountry = {};
  for (const x of (r.installs_country ?? []).filter((y) => inRange(y.Date))) byCountry[x.Country] = (byCountry[x.Country] ?? 0) + (Number(x['Daily User Installs']) || 0);
  const topC = Object.entries(byCountry).filter(([, v]) => v > 0).sort((a, b) => b[1] - a[1]).slice(0, 6);
  if (topC.length) out.push('', `Installations par pays (période) : ${topC.map(([k, v]) => `${k} ${v}`).join(' · ')}.`);
}

// ---- AdMob ----
const admob = load('admob');
if (admob?.byDayApp) {
  const rows = admob.byDayApp;
  const earn = sum(rows, 'ESTIMATED_EARNINGS'); const imp = sum(rows, 'IMPRESSIONS');
  h(`AdMob (bannière de l'app), ${label(rangeOf('admob'))}`);
  table(['Indicateur', 'Période'], [
    ['Revenus estimés', eur(earn)],
    ['Requêtes / impressions / clics', `${n(sum(rows, 'AD_REQUESTS'))} / ${n(imp)} / ${n(sum(rows, 'CLICKS'))}`],
    ['eCPM', imp ? eur(earn / imp * 1000) : 'n/d'],
  ]);
}

// ---- AdSense ----
const adsense = load('adsense');
if (adsense) {
  h(`AdSense (site), ${label(rangeOf('adsense'))}`);
  if (!adsense.account) out.push(adsense.note ?? 'Aucun compte.');
  else {
    const rows = adsense.byDayCountryPlatform?.rows ?? [];
    table(['Indicateur', 'Valeur'], [
      ['Compte', `${adsense.account.state}${adsense.account.pendingTasks?.length ? ` (${adsense.account.pendingTasks.join(', ')})` : ''}`],
      ['Sites', (adsense.sites ?? []).map((s) => `${s.domain} : ${s.state}`).join(' · ') || 'aucun'],
      ["Blocs d'annonces", n((adsense.adClients ?? []).reduce((a, c) => a + (c.adUnits?.length ?? 0), 0))],
      ['Revenus / pages vues (période)', rows.length ? `${eur(sum(rows, 'ESTIMATED_EARNINGS'))} / ${n(sum(rows, 'PAGE_VIEWS'))}` : 'aucune donnée'],
      ['Alertes', (adsense.alerts ?? []).map((a) => a.type).join(', ') || 'aucune'],
    ]);
  }
}

// ---- Search Console ----
const gsc = load('gsc');
if (gsc?.daily) {
  const d = gsc.daily; const clicks = sum(d, 'clicks'); const imp = sum(d, 'impressions');
  const pos = imp ? d.reduce((a, r) => a + r.position * r.impressions, 0) / imp : 0;
  const sorted = [...d].sort((a, b) => a.date.localeCompare(b.date));
  const half = Math.floor(sorted.length / 2);
  const first = sorted.slice(0, half), second = sorted.slice(half);
  h(`Search Console (site), ${label(rangeOf('gsc'))}`);
  table(['Indicateur', 'Période', '1re moitié', '2e moitié'], [
    ['Clics', n(clicks), n(sum(first, 'clicks')), n(sum(second, 'clicks'))],
    ['Impressions', n(imp), n(sum(first, 'impressions')), n(sum(second, 'impressions'))],
    ['CTR', imp ? pct(clicks / imp) : 'n/d', '', ''],
    ['Position moyenne (pondérée)', n(pos, 1), '', ''],
  ]);
  const top = (rows, sortKey, k = 6) => [...rows].sort((a, b) => b[sortKey] - a[sortKey]).slice(0, k);
  if (gsc.queryCountry) {
    const byQ = {};
    for (const r of gsc.queryCountry) { const q = byQ[r.query] ??= { query: r.query, clicks: 0, impressions: 0, w: 0 }; q.clicks += r.clicks; q.impressions += r.impressions; q.w += r.position * r.impressions; }
    out.push('', 'Requêtes les plus vues : ' + top(Object.values(byQ), 'impressions').map((q) => `« ${q.query} » ${q.impressions} imp. / ${q.clicks} clic(s) / pos. ${n(q.w / q.impressions, 1)}`).join(' · ') + '.');
  }
  if (gsc.pages) out.push('', 'Pages les plus cliquées : ' + top(gsc.pages, 'clicks').filter((p) => p.clicks > 0).map((p) => `${p.page.replace('https://rtpcalculator.com', '')} ${p.clicks} (${p.impressions} imp.)`).join(' · ') + '.');
}

// ---- GA4 ----
const ga4 = load('ga4');
if (ga4?.daily) {
  const d = ga4.daily; const bots = (ga4.tech ?? []).filter((t) => t.browser === '(not set)');
  h(`Analytics GA4 (site, minoré par le Consent Mode), ${label(rangeOf('ga4'))}`);
  table(['Indicateur', 'Période'], [
    ['Sessions / utilisateurs actifs', `${n(sum(d, 'sessions'))} / ${n(sum(d, 'activeUsers'))}`],
    ['Sessions engagées', n(sum(d, 'engagedSessions'))],
    ['Sessions « navigateur non défini » (robots probables)', n(sum(bots, 'sessions'))],
    ['Clics sortants vers Google Play', ga4.playClicks ? n(sum(ga4.playClicks, 'eventCount')) : 'non collecté'],
  ]);
  const byCh = {}; for (const r of d) byCh[r.sessionDefaultChannelGroup] = (byCh[r.sessionDefaultChannelGroup] ?? 0) + r.sessions;
  out.push('', 'Sessions par canal : ' + Object.entries(byCh).sort((a, b) => b[1] - a[1]).map(([k, v]) => `${k} ${v}`).join(' · ') + '.');
  if (ga4.playClicks?.length) {
    const byP = {}; for (const r of ga4.playClicks) byP[r.pagePath] = (byP[r.pagePath] ?? 0) + r.eventCount;
    out.push('', 'Clics vers Play par page : ' + Object.entries(byP).sort((a, b) => b[1] - a[1]).map(([k, v]) => `${k} ${v}`).join(' · ') + '.');
  }
}

// ---- Source hors défaut présente dans le dossier ----
const inspect = load('inspect');
if (inspect?.urls) {
  const idx = inspect.urls.filter((u) => u.verdict === 'PASS').length;
  h('Indexation (source inspect)');
  out.push(`${inspect.urls.length} URL inspectées, ${idx} avec verdict PASS (détail dans le JSON).`);
}

out.push('');
process.stdout.write(out.join('\n'));
