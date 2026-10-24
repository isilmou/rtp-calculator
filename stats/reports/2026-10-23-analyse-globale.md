# Analyse globale des statistiques — 23/10/2026

Source : snapshot `stats/data/2026-10-23/` (01/01 → 22/10/2026, AdMob + GA4 + Search Console +
Play). Inventaire fonctionnel de l'app et du site issu du code à la même date. Les volumes sont
très faibles partout : chaque chiffre est un ordre de grandeur, pas une tendance solide.

## 1. App Android (Play Console, 06/05 → 19/10/2026)

| Indicateur | Valeur |
|---|---|
| Installations / désinstallations cumulées | 95 / 91 |
| Appareils actifs (fin de période) | 7 (plafond observé : 12 le 08/09) |
| Visiteurs fiche Play / acquisitions | 359 / 86 → conversion fiche 24 % |
| Source de trafic Play | 100 % « Other » (agrégé sous le seuil de volume) |
| Note | 5,0 — 1 avis (Myanmar, 04/09, sans texte, v2.1.0) |
| Vitals | 0 ligne (sous le seuil) |

Installations par mois : mai 16, juin 14, juillet 9, août 21, septembre 22, octobre 13 (partiel).
Les désinstallations suivent les installations le jour même ou le lendemain.

Pays : Myanmar 32, Brésil 16, France 5, Indonésie 4, Nigeria 4, Côte d'Ivoire 4, Bénin 3,
États‑Unis 2. Opérateurs : Mytel, ATOM, Ooredoo, MPT (Myanmar) ; TIM, Vivo, Claro (Brésil).
Appareils : Xiaomi/Redmi et Tecno d'entrée de gamme. Langues système : en_US 32, pt_BR 15,
fr_FR 14, en_GB 9, my_MM 5. Android 14–16 majoritaires.

**Lecture.** La moitié des installations vient de marchés où « RTP » désigne le taux de
redistribution des machines à sous. Ces utilisateurs cherchent un outil casino, installent,
constatent l'écart, désinstallent. La rétention nulle est d'abord un problème d'intention de
recherche sur le nom « RTP Calculator », pas un problème de qualité d'app. Les 5 installations
françaises (« TRJ ») sont les seules où le vocabulaire correspond à l'usage.

**Usage réel** (AdMob comme proxy d'ouverture) : octobre = 66 requêtes en 22 jours, soit 2 à 3
ouvertures par jour pour 7 appareils. Pics attribuables aux tests internes : 10/05 (FR, 91 req.)
et 19/09 (FR, 41 req.). Un seul utilisateur intensif repéré : Mali, 28/08, 79 impressions.

## 2. Monétisation (AdMob, 06/05 → 22/10/2026)

| Indicateur | Valeur |
|---|---|
| Revenus cumulés | 1,36 € |
| Requêtes / impressions / clics | 1 081 / 605 / 15 |
| eCPM moyen | ≈ 2,25 € |
| Format | 1 unité, bannière 320×50 (historique + formulaire) |
| Répartition | Myanmar 0,89 € (dont 0,50 € le 04/09), Brésil 0,28 €, France 0,06 € |

Impressions France de mai et septembre = surtout tests internes (avant `setTestDeviceIds`,
2.1.1). Remplissage faible sur Malaisie (40/244) et Kazakhstan (0/12).
**Lecture.** Pas un levier à ce volume : aucun format ne changera l'ordre de grandeur.

## 3. Site — Search Console (06/2026 → 22/10/2026)

| Mois | Impressions | Clics | Position |
|---|---|---|---|
| Juin | 20 | 0 | 13,5 |
| Juillet | 102 | 0 | 26,0 |
| Août | 204 | 6 | 12,6 |
| Septembre | 184 | 3 | 12,5 |
| Octobre (22 j) | 250 | 6 | 10,8 |

Impressions ×2 depuis juillet, position qui remonte. Pic du 20 au 22/10 : 45 à 55 impressions
par jour contre 5 avant (semaine du 19/10 : 159, FR/BE/US/BR/ES, surtout mobile), origine
inconnue. CTR global 2 % (15 clics / 760 impressions).

Pays (impressions) : France 211, États‑Unis 129, Portugal 73, Brésil 40, Allemagne 33,
Royaume‑Uni 31, Belgique 21, Canada 20, Espagne 19. Desktop 428 / mobile 326.

| Requête | Pays | Impr. | Position | Intention |
|---|---|---|---|---|
| calculo rtp | PT / BR | 74 | 39–45 | casino |
| calcul trj | FR / BE / CH | 70 | ≈ 10 | paris sportifs |
| calculateur trj | FR / BE / CH / NO | 33 | ≈ 9 | paris sportifs |
| rtp calculator | US / GB / DE | 40 | 13–17 | ambiguë, surtout casino |
| calculate rtp, rtp calculation | US / AU | 20 | 10–25 | ambiguë |
| trj calcul, taux de retour paris, trj paris sportifs | FR / BE | 25 | 10–25 | paris sportifs |
| marge bookmaker | BE / GB | 7 | 10–14 | paris sportifs |
| casino rtp calculator | US / IN | 8 | 24 | casino |

Pages : accueil EN 245 impr. (pos. 12), `/fr/` 216 (pos. 10), lexique FR 43 (pos. 8,4),
index guides FR 18 (pos. 8,7), marge bookmaker FR 12, formats de cotes FR 12. Guides PT et DE
en positions 25–55.

**Problème technique** : Google affiche encore `http://rtpcalculator.com/fr/` (216 impr.),
`http://rtpcalculator.com/es/` et `https://www.rtpcalculator.com/` (51 impr.) à côté des URL
canoniques. Le `.htaccess` actuel (X-Forwarded-Proto, cibles absolues) est correct : reliquat
de l'ancienne redirection cassée avant le 04/07, non re-canonisé. Sitemap : 55 URL soumises,
« indexées : 0 » selon l'API (champ peu fiable).

## 4. Site — GA4

Quasi inexploitable : le pic de juin (164 utilisateurs, 133 « US / desktop / direct » à 0–3 s)
et la série régulière du 15/06 au 01/07 (3 à 7 sessions/jour, 15 s, 2 pages) sont des robots
(référents `aicrawler.store`, `aisearchindex.space`). Juillet → octobre : ≈ 25 sessions
humaines, dont des sessions FR de 800–900 s vraisemblablement internes. Organique réel
identifié : ≈ 10 sessions en 4 mois + 1 session via ChatGPT (16/10, `/fr`). L'écart avec les
15 clics Search Console est cohérent avec un Consent Mode qui perd une partie des visites.

## 5. Croisement site ↔ app

- Le site attire FR / BE / CH / US ; l'app attire Myanmar / Brésil / Afrique de l'Ouest. Les
  deux canaux ne se parlent pas : aucune attribution site → Play (liens sans `referrer`).
- Seul segment aligné (intention, langue, produit) : francophones cherchant « TRJ », là où le
  site est en page 1.
- En anglais, le vocabulaire paris sportifs est *vig / overround / bookmaker margin / implied
  probability*, pas « RTP ». En portugais, « calculo rtp » = machines à sous.

## 6. Diagnostic

1. Mauvaise cible, pas mauvais produit : 50 % des installs viennent de la confusion RTP‑casino.
2. Le site est l'actif qui progresse, mais convertit mal (CTR 2 %) et n'alimente pas l'app
   de façon mesurable.
3. Dette SEO : canonicalisation http/https/www non réglée côté Google.
4. Mesure aveugle : aucune analytics in‑app.
5. Monétisation hors sujet à ce volume.

## 7. Pistes retenues (classées), ordre d'exécution décidé le 23/10/2026

| Ordre | Piste | Effort | Impact |
|---|---|---|---|
| 0 | Outil de déploiement du site par Claude : SFTP/SSH clé ed25519, script `deploy/`, secrets dans `_secrets/` | moyen | prérequis |
| 1 | Consigner l'analyse (ce rapport + journal) | faible | nécessaire |
| 2 | Site, un lot : title/meta/CTA héro sur `/fr/` puis 4 langues ; `referrer` UTM sur les liens Play ; `og:image` ; dédoublonnage de la politique de confidentialité sur l'accueil ; `pt-BR` en hreflang ; texte de `/app/` étoffé | faible | élevé |
| 3 | Sitemap avec `lastmod` + hreflang ; source `inspect` (API URL Inspection) dans `stats/` ; demande de réindexation de `/`, `/fr/`, `/es/` | faible | moyen |
| 4 | Fiche Play : lever l'ambiguïté casino dès le titre, 5 langues (copie FR validée puis propagée, saisie manuelle). Moins d'installs attendues, mais utiles | faible | élevé |
| 5 | Deux guides FR : « Comment calculer le TRJ d'un pari sportif » (requête exacte « calcul trj ») et « TRJ et plafond légal de 85 % en France (ANJ) », puis traductions | moyen | élevé |
| 6 | App 2.2.0 : afficher marge et surebet (déjà calculés dans `calculateRtp`) ; URL du site dans l'image partagée ; analytics in‑app (Firebase) = décision séparée | faible–moyen | moyen |
| 7 | Repositionnement EN sur *bookmaker margin / overround* (titres, H1, marge dans le calculateur web) ; puis `npm run report` (comparaison de snapshots) | moyen | moyen |

Vérifications : disparition des URL `http://` et `www.` du rapport pages GSC sous 2 semaines ;
CTR de `/fr/` sur « calcul trj / calculateur trj » ; apparition d'une source autre que « Other »
dans Play ; part Myanmar + Brésil des installs et désinstallations J+1 après la nouvelle fiche.

## 8. Pistes écartées

- Onboarding, notifications, tutoriel : inutiles tant que la cible est fausse ; un calculateur
  n'a pas de déclencheur naturel de notification.
- Nouveaux formats AdMob : gain nul à 2–3 sessions/jour, expérience dégradée.
- Exclure Myanmar / Brésil de la distribution : trop brutal, la fiche Play règle la source.
- Chasser « calculo rtp » : intention slots, position 40.
- Renommer l'app : « RTP » est la marque et le domaine ; on complète le vocabulaire.
- Toucher au Consent Mode pour gonfler GA4 : collecte conforme ; Search Console fait foi.
- Générateur de site statique : à reposer seulement si la production de contenu devient
  régulière après l'étape 5.
- En‑têtes de cache `.htaccess` : zone interdite, gain marginal.
- Déploiement Git Infomaniak (dépôt nu + clone) : contredit la décision du 18/10 (GitHub =
  sauvegarde), enverrait app et journal sur le serveur, push interdit à Claude.

## 9. À recouper par Julien

- Texte actuel de la fiche Play (pour l'étape 4).
- Rapport « Pages » et inspection d'URL de `/fr/` dans Search Console (canonical retenu).
- Statut de la demande AdSense (script chargé sur `/app/` avec emplacement vide).
