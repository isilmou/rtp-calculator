// Envoie la fiche Play (RTP Calculator - App/play/listings/<langue>/{title,short,full}.txt)
// dans la Play Console et les met en examen chez Google.
//
//   npm run listing:push              à blanc : lit la fiche en ligne, montre les écarts, ne change rien
//   npm run listing:push -- --apply   écrit les champs modifiés, puis `edits.commit` avec
//                                     changesInReviewBehavior=ERROR_IF_IN_REVIEW
//
// Après --apply, les changements sont envoyés en examen immédiatement (Google refuse le drapeau
// changesNotSentForReview pour cette app : « Changes are sent for review automatically », 25/10/2026).
// Publication gérée : rien n'est en ligne avant le clic « Publier les modifications » de Julien ; ce
// script ne publie jamais. Si des changements sont déjà en examen, le commit est refusé
// (ERROR_IF_IN_REVIEW) au lieu de les annuler. En cas d'erreur avant le commit, le brouillon est
// supprimé. Site de contact aligné sur https://rtpcalculator.com.

import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import { google } from 'googleapis';
import { getAuth } from './lib/auth.mjs';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const config = JSON.parse(fs.readFileSync(path.join(__dirname, 'config.json'), 'utf8'));
const LISTINGS = path.resolve(__dirname, '..', 'RTP Calculator - App', 'play', 'listings');
const CONTACT_WEBSITE = 'https://rtpcalculator.com';
const LIMITS = { title: 30, shortDescription: 80, fullDescription: 4000 };
const FILES = { title: 'title.txt', shortDescription: 'short.txt', fullDescription: 'full.txt' };
const apply = process.argv.includes('--apply');

const read = (lang, file) =>
  fs.readFileSync(path.join(LISTINGS, lang, file), 'utf8').replace(/\r\n/g, '\n').trimEnd();
const len = (s) => [...s].length;

// Fiche locale
const local = {};
for (const lang of fs.readdirSync(LISTINGS).filter((d) => fs.statSync(path.join(LISTINGS, d)).isDirectory())) {
  local[lang] = Object.fromEntries(Object.entries(FILES).map(([k, f]) => [k, read(lang, f)]));
  for (const [k, max] of Object.entries(LIMITS)) {
    if (len(local[lang][k]) > max) throw new Error(`${lang} ${k} : ${len(local[lang][k])} > ${max}`);
  }
  if (/[—–’‘“”]/.test(Object.values(local[lang]).join(''))) throw new Error(`${lang} : tiret long ou apostrophe courbe`);
}

const { packageName } = config.play;
const publisher = google.androidpublisher({ version: 'v3', auth: getAuth() });
const editId = (await publisher.edits.insert({ packageName })).data.id;
let committed = false;
try {
  const [listings, details] = await Promise.all([
    publisher.edits.listings.list({ packageName, editId }),
    publisher.edits.details.get({ packageName, editId }),
  ]);
  const remote = Object.fromEntries((listings.data.listings ?? []).map((l) => [l.language, l]));

  // Écarts
  const changes = [];
  for (const [lang, loc] of Object.entries(local)) {
    const rem = remote[lang];
    if (!rem) throw new Error(`${lang} : langue absente de la fiche en ligne, ajout refusé (à créer dans la console)`);
    for (const k of Object.keys(FILES)) {
      if ((rem[k] ?? '') !== loc[k]) changes.push({ langue: lang, champ: k, 'en ligne': len(rem[k] ?? ''), local: len(loc[k]) });
    }
  }
  for (const lang of Object.keys(remote)) if (!local[lang]) console.log(`Avertissement : ${lang} en ligne sans dossier local, laissé tel quel.`);
  const contactChange = details.data.contactWebsite !== CONTACT_WEBSITE;
  if (contactChange) changes.push({ langue: '(détails)', champ: 'contactWebsite', 'en ligne': details.data.contactWebsite, local: CONTACT_WEBSITE });

  if (!changes.length) { console.log('Aucun écart : la fiche en ligne est identique aux fichiers.'); }
  else console.table(changes);

  if (!apply || !changes.length) {
    console.log(apply ? 'Rien à envoyer.' : 'Mode à blanc : rien envoyé. Relancer avec --apply pour écrire ces champs.');
  } else {
    const langs = [...new Set(changes.filter((c) => c.langue !== '(détails)').map((c) => c.langue))];
    for (const lang of langs) {
      await publisher.edits.listings.update({
        packageName, editId, language: lang,
        requestBody: { language: lang, ...local[lang], video: remote[lang].video ?? undefined },
      });
      console.log(`  ${lang} : fiche mise à jour dans le brouillon`);
    }
    if (contactChange) {
      await publisher.edits.details.patch({ packageName, editId, requestBody: { contactWebsite: CONTACT_WEBSITE } });
      console.log('  contactWebsite mis à jour dans le brouillon');
    }
    await publisher.edits.validate({ packageName, editId });
    await publisher.edits.commit({ packageName, editId, changesInReviewBehavior: 'ERROR_IF_IN_REVIEW' });
    committed = true;
    console.log('\nModifications validées et envoyées en examen chez Google (jusqu\'à 7 jours).');
    console.log('À faire par Julien après approbation : « Publier les modifications » dans la Play Console.');
  }
} finally {
  if (!committed) await publisher.edits.delete({ packageName, editId });
}
