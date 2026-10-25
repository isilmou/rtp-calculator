// Play Console, API Android Publisher : texte de la fiche Play dans toutes les langues.
//
// L'API ne lit une fiche qu'à travers un brouillon de modification (« edit ») :
//   1. edits.insert    crée un brouillon vide (aucun effet tant qu'il n'est pas validé) ;
//   2. edits.listings.list + edits.details.get lisent le contenu courant ;
//   3. edits.delete    supprime le brouillon, dans un `finally`, même en cas d'erreur.
// `edits.commit` n'est jamais appelé : rien n'est modifié ni publié. Un brouillon
// oublié expirerait de lui-même sans effet. Hors collecte par défaut.

import { google } from 'googleapis';

export async function fetchListing(auth, config) {
  const { packageName } = config.play;
  const publisher = google.androidpublisher({ version: 'v3', auth });

  const editId = (await publisher.edits.insert({ packageName })).data.id;
  try {
    const [listings, details] = await Promise.all([
      publisher.edits.listings.list({ packageName, editId }),
      publisher.edits.details.get({ packageName, editId }),
    ]);
    const languages = (listings.data.listings ?? [])
      .map((l) => ({
        language: l.language,
        title: l.title ?? '',
        shortDescription: l.shortDescription ?? '',
        fullDescription: l.fullDescription ?? '',
        video: l.video ?? null,
        lengths: {
          title: [...(l.title ?? '')].length,
          shortDescription: [...(l.shortDescription ?? '')].length,
          fullDescription: [...(l.fullDescription ?? '')].length,
        },
      }))
      .sort((a, b) => a.language.localeCompare(b.language));
    return {
      packageName,
      fetchedAt: new Date().toISOString(),
      defaultLanguage: details.data.defaultLanguage ?? null,
      contactWebsite: details.data.contactWebsite ?? null,
      contactEmail: details.data.contactEmail ?? null,
      limits: { title: 30, shortDescription: 80, fullDescription: 4000 },
      languages,
    };
  } finally {
    await publisher.edits.delete({ packageName, editId });
  }
}
