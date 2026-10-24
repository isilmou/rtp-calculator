# RTP Calculator — Journal de bord

Mémoire inter-sessions de Claude Code. Lu en début de session. Tenu **court** : décisions,
raisons, pistes écartées, états. Pas de narration ; les contraintes durables vivent dans
`CLAUDE.md`, pas ici. Condensé le 19/09/2026 à la demande de Julien.

---

## État courant (23/10/2026)

| Élément | Valeur |
|---|---|
| Version en production | 2.1.1 / versionCode 10, publiée le 19/09/2026 (commit `f0586de`) |
| Chantier prioritaire | Obscurcissement DEX : livré. Reste à vérifier la disparition de l'alerte Play sous 48 h |
| Statistiques Google | Collecte opérationnelle depuis le 23/10/2026 (`stats/`, 4 sources). Analyse initiale faite le 23/10/2026 : `stats/reports/2026-10-23-analyse-globale.md` |
| Chantier en cours | Plan d'amélioration validé le 23/10/2026 (ordre ci-dessous). Étape 2 (lot CTR du site) déployée le 24/10/2026, commit à faire ; prochaine : étape 3 (sitemap, inspection, réindexation) |
| Devices de test | S9 (Android 10) et S23+ (Android 16), builds hors Play Store, déclarés test AdMob |
| Dépôt distant | GitHub privé `isilmou/rtp-calculator`, branche `main`, créé le 18/10/2026. Sauvegarde des sources uniquement : pas de CI, pas d'hébergement du site. Push manuel par Julien après chaque commit |

### À faire

Ordre validé par Julien le 23/10/2026 (détail et justification dans le rapport) :

0. ~~**Outil de déploiement du site**~~ — opérationnel le 24/10/2026 : `check` OK en mode
   passif (pas de gel), envoi à blanc de `robots.txt` vérifié en HTTPS en 2,5 s, diff vide.
   Compte FTP : celui de l'hébergement (Starter refuse la création d'un compte supplémentaire),
   racine FTP = docroot du site (`remoteRoot` vide). `CLAUDE.md` mis à jour (règle 8, § 6, § 7, § 8).
1. ~~Consigner l'analyse~~ — fait le 23/10/2026.
2. **Site, lot CTR** — en cours. Fait le 24/10/2026 sur `/fr/` seulement (local, non déployé) :
   title « Calcul du TRJ d'un pari sportif — Calculateur gratuit en ligne », meta description
   et `og:*` réécrits (le calculateur web passe avant l'app Android : 57 % des impressions
   sont desktop), H1 « Calculez le TRJ de vos paris sportifs », surtitre, lede, deux boutons
   héros (`.hero-ctas` : « Calculer un TRJ » → `/fr/app/`, « Télécharger sur Android » → Play).
   `referrer` UTM (`utm_source=rtpcalculator.com`, `utm_medium=web`, `utm_campaign=home-fr`,
   encodé) sur les 3 liens Play de `/fr/` ; 16 autres liens Play dans les pages FR restent sans UTM.
   `og:image` : `img/og-fr.png` (1200×630, 31 Ko) généré par un script `sharp` du scratchpad
   (SVG composé : logo du site, H1, surtitre, URL, pastille TRJ ; police Archivo téléchargée
   depuis google/fonts, chargée via `FONTCONFIG_FILE`), balises `og:image*` + `twitter:card`
   sur `/fr/`. Le site n'avait aucune image bitmap. Outillage non versionné : à refaire si besoin.
   Politique de confidentialité et suppression des données retirées de l'accueil (texte identique
   à `/fr/privacy/`, 65 lignes) : remplacées par un résumé de 3 phrases + lien, ancres `#privacy`
   et `#data-deletion` conservées (menu, et au cas où la Play Console les référence).
   hreflang `pt-BR` ajouté à côté de `pt` (même URL `/pt/…`) dans les 55 pages du site, par
   script ; le sitemap n'a pas de hreflang (étape 3).
   `/fr/app/` : note de bas de page remplacée par une section `.seo-text` (≈ 230 mots : calcul du
   TRJ avec exemple 2,10 / 3,40 / 3,60 → 95,4 %, lecture du résultat, plafond ANJ 85 %, formats de
   cotes, 3 liens guides) ; UTM `app-fr` sur les 3 liens Play de la page.
   **Lot FR terminé le 24/10/2026.** Propagation EN (validée), puis ES / DE / PT-BR le 24/10 :
   script `propagate.mjs` + un JSON par langue (scratchpad, non versionné), qui refuse d'écrire
   si un texte d'origine n'est pas trouvé exactement une fois ; 5 images `img/og-<lang>.png`.
   Adaptation hors FR : la phrase sur le plafond ANJ est présentée comme exemple (« certains
   régulateurs… en France, l'ANJ… »). Au passage, le H2 de la politique sur `/pt/` disait
   « Política de privacidad » (espagnol) : corrigé par le dédoublonnage.
   UTM `guides-<lang>` / `about-<lang>` posés par script sur les 70 liens restants : 100 liens
   Play, 20 par langue, `hl` et campagne cohérents avec le dossier (vérifié). Piège : `path.join`
   supprime `./`, lire la langue via `path.relative`.
   **Étape 2 déployée le 24/10/2026** (60 fichiers : 5 images + 55 HTML, tous vérifiés en
   HTTPS, diff à blanc vide ensuite ; `manifest.json` à jour). Reste : commit par Julien.
   Piège rencontré : la relecture HTTPS (`fetch` Node) échoue parfois avec « fetch failed » alors
   que le fichier est bien en ligne ; `verifyPublished` traite désormais l'exception comme une
   tentative ratée (3 essais) au lieu d'interrompre le déploiement. Après une interruption, le
   manifeste ne contient que les fichiers vérifiés : relancer `push` réenvoie le reste, sans risque.
3. **Sitemap** `lastmod` + hreflang ; source `inspect` dans `stats/` ; réindexation `/`, `/fr/`, `/es/`.
4. **Fiche Play** : lever l'ambiguïté casino dès le titre, 5 langues. Julien doit coller le
   texte actuel de la fiche.
5. **Guides FR** : « Comment calculer le TRJ d'un pari sportif » et « TRJ et plafond légal de
   85 % en France (ANJ) », puis traductions.
6. **App 2.2.0** : afficher marge et surebet, URL du site dans l'image partagée ; analytics
   in-app (Firebase) = décision séparée à prendre avant.
7. **Repositionnement EN** (bookmaker margin / overround), puis `npm run report`.

Hors plan, toujours valables :
- **Vérifier** dans la Play Console que l'alerte « Obscurcissement 0 % » a disparu sur la
  2.1.1. Si elle persiste, relire le score par catégorie avant de toucher aux règles.
- Retirer `material-icons-extended` (AAR 35 Mo, seules `AccessTime` et `ContentCopy` sont hors
  jeu de base). Gain sur le build debug uniquement.
- **Kotlin 2 / Compose récent** : chantier à part.
- Revoir vers le 06/11/2026 le pic Search Console du 20–22/10 (45–55 impressions/jour).
- Julien : recouper AdMob 1,36 € et Play 95 installs avec les consoles ; statut AdSense.
- Meta description des 5 accueils : ≈ 195 caractères (choix de Julien, 24/10). Google tronque
  vers 155 : vérifier l'extrait réellement affiché lors du prochain bilan Search Console.
- Play Console, « Sécurité des données » : si les URL de politique / suppression pointent vers
  `/fr/#privacy`, les faire pointer vers `/fr/privacy/` (les ancres restent valides).

Méthode retenue pour éditer le site (24/10/2026) : scripts Node dans le scratchpad qui refusent
d'écrire si le texte d'origine n'est pas trouvé exactement une fois, puis `git diff` montré.
Éviter `node -e` ou un heredoc Bash pour un script contenant backticks ou apostrophes : écrire
un fichier `.mjs`. Le scratchpad est propre à la session : les outils `propagate.mjs`,
`gen.mjs` (og:image, sharp + Archivo) et les JSON par langue ont disparu avec elle ; la
recette est décrite ci-dessus, à versionner dans `tools/` si l'on doit regénérer les images.

---

## Analyse des statistiques et plan d'amélioration (23/10/2026)

Rapport complet : `stats/reports/2026-10-23-analyse-globale.md` (versionné). À retenir :

- **Diagnostic** : la rétention nulle de l'app (95 installs / 91 désinstalls, 7 actifs) vient
  d'abord d'une confusion d'intention — Myanmar 32 et Brésil 16 installs, marchés où « RTP » =
  machines à sous. Désinstallation à J ou J+1. Seules les installs FR (« TRJ ») ont l'usage
  attendu. En anglais le vocabulaire paris sportifs est *vig / overround / bookmaker margin*.
- **Site** : impressions GSC ×2 depuis juillet, cluster FR « calcul trj / calculateur trj » en
  positions 8–10, mais CTR 2 %. Google montre encore `http://…/fr/` et `www.` comme canoniques
  (reliquat d'avant le correctif `.htaccess` du 04/07, le fichier actuel est correct).
- **GA4** : pic de juin = robots (crawlers IA) ; ≈ 10 sessions organiques réelles en 4 mois,
  dont 1 via ChatGPT. Search Console fait foi pour le trafic.
- **AdMob** : 1,36 €, bannière seule ; pas un levier à ce volume.
- **Écarté, ne pas rouvrir sans nouveau chiffre** : onboarding/notifications (cible fausse
  d'abord), nouveaux formats AdMob, exclusion de pays, renommage de l'app, retouche du Consent
  Mode, générateur de site statique (à reposer si production de contenu régulière), cache
  `.htaccess`, déploiement Git Infomaniak (contredit « GitHub = sauvegarde », push interdit).
- **Déploiement du site par Claude** : l'hébergement est en offre **Starter** → FTP en clair
  port 21 seulement, pas de SSH ni TLS (FAQ Infomaniak 446). SFTP/clé écarté de fait ; Julien a
  choisi le FTP scripté plutôt que l'hébergement payant ou une migration vers un hébergeur
  statique à déploiement Git. Outil `deploy/` (Node sans dépendance, curl comme moteur,
  identifiants via `--netrc-file` dans `_secrets/`, `deploy/manifest.json` versionné = état
  connu de la production, vérification HTTPS après envoi, aucune suppression distante,
  `.htaccess` protégé). `init` du 23/10/2026 : 59 fichiers identiques à la production.
  **Piège** : `core.autocrlf=true` livre la copie de travail en CRLF alors que l'index Git et la
  production sont en LF ; le script normalise les fichiers texte en LF avant hachage et envoi.
  Mise en place (24/10) : Starter n'accepte qu'un seul compte FTP ; le Bloc-notes ajoute `.txt`
  aux fichiers sans extension (`netrc.txt`) ; le préfixe `!` de Claude Code lance Git Bash
  (`mv`, pas `ren`). Secrets jamais lus par Claude : diagnostic par noms et tailles seulement.

---

## Statistiques Google (23/10/2026)

Objectif : Claude lit lui-même toutes les statistiques, sans action de Julien à chaque analyse.
Boîte à outils Node `stats/` (détail des commandes dans `CLAUDE.md` § 9).

- **API officielles + OAuth du compte Julien** (`contact@rtpcalculator.com`, Workspace), tout en
  lecture seule. Écarté : compte de service (refusé par l'API AdMob) ; serveurs MCP (GA officiel
  en Python, absent ici ; rien d'officiel pour AdMob ni Play) ; exports CSV manuels et lecture
  des consoles via navigateur (action humaine à chaque analyse).
- **Projet Google Cloud `rtp-calculator-stats` sans compte de facturation** : coût nul garanti,
  un service payant serait refusé et non facturé. Écran OAuth « Externe », publié **en
  production** : en mode Test le jeton expire après 7 jours. Le bouton Publier reste grisé tant
  que page d'accueil, règles de confidentialité et domaine autorisé ne sont pas renseignés.
- **Play Console** : la page « Accès à l'API » (association d'un projet Cloud) n'existe plus en
  2026 ; les droits du compte suffisent. Bucket des rapports : `pubsite_prod_<id développeur>`
  (**sans** `_rev_`, contrairement à ce que je supposais). Vitals (plantages, ANR) : l'API répond
  mais renvoie 0 ligne, app sous le seuil d'utilisateurs quotidiens de Google.
- Identifiants découverts automatiquement et mémorisés dans `stats/config.json` : propriété GA4
  `540143495`, Search Console `sc-domain:rtpcalculator.com`.
- Pièges de code rencontrés : `start "" "<url>"` sous Windows ne doit pas échapper les `&`
  (URL déformée, erreur 400 Google) ; noms des jeux de métriques Play en camelCase
  (`crashRateMetricSet`).
- **Premiers chiffres bruts** (06/05 → 22/10/2026) : 95 installations, 91 désinstallations,
  7 appareils actifs, 1,36 € AdMob, note 5,0 (1 avis sans texte) ; site : 190 sessions GA4,
  15 clics / 760 impressions Search Console. Analysés le 23/10/2026 (section ci-dessus) :
  l'écart GA4 / Search Console s'explique par le Consent Mode, pas par une collecte cassée.

---

## Dépôt GitHub (18/10/2026)

- **Un seul dépôt** pour l'app et le site : ils étaient déjà suivis ensemble, avec CLAUDE.md et
  journal.md partagés à la racine. Écarté : deux dépôts (aucun besoin de visibilité ni de
  workflow distincts ; aurait imposé un `subtree split` et un choix d'emplacement pour les
  fichiers racine).
- **Privé** : le journal et CLAUDE.md n'ont pas vocation à être publics. Aucun secret n'est
  suivi (vérifié : pas de keystore, `local.properties`, mot de passe ni identifiant FTP ;
  AdMob/GA4 déjà publics dans l'APK et le site).
- **Historique réécrit avant le premier push** (`git filter-branch`, outil intégré) pour
  retirer `node_modules/` et `app/release/` (DLL 19 Mo, APK et AAB 14 Mo) présents dans les
  commits de mai et juillet. Piège rencontré : ces deux commits avaient l'app à la racine du
  dépôt, le dossier `RTP Calculator - App/` n'existe que depuis la 2.1.1 ; le filtre doit viser
  les deux jeux de chemins. `.git` : 49 Mo → 0,5 Mo. Les SHA ont changé, messages et dates
  conservés. Le bundle de sauvegarde de l'historique d'origine a été supprimé le 18/10/2026
  après vérification du push ; les sources 2.1.1 et 07/09/2026 (site) restent sur Drive.
- **`master` → `main`** (convention GitHub). `init.defaultBranch` global non modifié.
- **Authentification** : Git Credential Manager (HTTPS, jeton dans le gestionnaire Windows).
  Pas de `gh`, pas de clé SSH. `.claude/settings.json` interdit `git push` à Claude Code :
  tout push passe par Julien (`! git push`).
- Ajouts `.gitignore` racine : `*.zip` (archives d'upload du site, conservées sur Drive) et
  `_backup/`.

---

## Contenu de la 2.1.1 (19/09/2026)

**Obscurcissement R8.** Cause de l'alerte : `isMinifyEnabled = false` en release, R8 n'avait
jamais tourné. Activé avec `isShrinkResources`. Règles `keep` ciblées dans
`app/proguard-rules.pro` : champs + constructeur vide de `BetAnalysis` / `Outcome`, enum
`OddsFormat` (clés JSON Gson de l'historique), `Signature` + `TypeToken`, `LineNumberTable`.
Résultat : 4 classes non renommées sur 99, APK release 3,4 Mo, `mapping.txt` embarqué dans
l'AAB. Migration prouvée sur les 8 analyses réelles du S23+ (build non-debuggable installé
par-dessus, relecture OK) et statiquement (17 clés JSON = champs conservés). Chemin de la
demande d'avis Play exercé sous R8 sans erreur (déclenché au 5ᵉ lancement exact).

**Saisie des cotes.** Règle finale validée : une cote est un nombre **≥ 1**.
- À la frappe : EU refuse « 0 » ou « . » en première position ; UK/US refusent un « 0 »
  initial ; zéros de tête redondants retirés dès qu'un chiffre suit (« 0800 » → « 800 »).
- À la sortie du champ EU et à la sauvegarde : « 1 » devient « 1.00 », un point seul est
  vidé (`dropInvalidDecimalInput`). Décimales jamais touchées (« 2.180 » reste).
- `calculateRtp`, `hasMeaningfulContent`, pilule TRJ : seuil ≥ 1.
- Aucun message d'aide, aucune couleur d'erreur : la frappe refusée ne s'affiche pas.
- Écarté : message « Remplissez toutes les cotes… » (existait en mai 2026, retiré à la
  refonte 1.0.3, rétabli puis re-supprimé le 19/09 : commenter une saisie invalide est moins
  cohérent que l'empêcher). Écarté : bloquer le bouton Enregistrer (conflit avec la
  sauvegarde automatique à la sortie, conçue pour garder un brouillon).
- Données anciennes non migrées : nettoyées à leur prochaine sauvegarde.

**Splash unique.** Le logo vu sur Android 12+ venait du splash système ; le composable
maison (voile 500 ms, nom seul) était redondant et sans logo sur Android 10. Remplacé par
`androidx.core:core-splashscreen` : thème `Theme.RTPCalculator.Starting`, icône référencée
(jamais régénérée), `installSplashScreen()` maintenu tant que `historyLoaded` est faux.

**AdMob.** Les builds de test release génèrent de vraies statistiques : le SDK ne
reconnaissait aucun des deux téléphones (enregistrement console inopérant). Corrigé par
`setTestDeviceIds` dans `MainActivity` (empreintes S9 `489C9D693BD433E3908C6AE730885490`,
S23+ `15E99F91C172FCDBD7E1C9C53B318BDF`, vérifiées par le log « sent from a test device »).

**Nettoyage.** `Formatters.kt`, `convertToDecimalOdds`, `toDecimalOddsOrNull`,
`fieldColors()` supprimés ; 4 chaînes inutilisées retirées des 5 langues ; dépendance
`ui-tooling-preview` retirée ; parsing de l'heure unifié dans `DateUtils.parseTimeOrNull` ;
`node_modules/` ignoré par Git ; fichiers `*_TRANSLATIONS.txt` supprimés.
Audit : l'app est déjà légère ; écartés volontairement Gson (migration pour quelques Ko) et
le suivi de scroll de `HistoryScreen` (heuristiques durement acquises, ne pas toucher).

**Tests** : `OddsConversionTest` 30 + `HistoryRetroCompatTest` 3, 0 échec. Toujours vérifier
qu'un test ajouté apparaît **par son nom** dans le rapport XML (un `@Test` perdu passe inaperçu).

---

## Décisions d'environnement (19/09/2026)

- **Google Drive abandonné comme répertoire de travail** (hydratation partielle des fichiers,
  corruption Git possible, bruit de synchro). Sources dans `C:\RTP-Calculator\`. Drive garde
  les AAB signés, les ZIP du site et les documents maîtres. Le keystore y était aussi : en
  streaming la signature échouait (`block not properly padded`), d'où son stockage local dans
  `C:\RTP-Calculator-Keystore\` (CLAUDE.md § 5).
- **Claude Code lancé directement dans le dossier source** ; ce journal remplace la
  continuité du fil de conversation. Outillage icône : `generate_icons.mjs` + `logo/` + npm.
