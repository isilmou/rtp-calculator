# RTP Calculator — Journal de bord

Mémoire inter-sessions de Claude Code. Lu en début de session. Tenu **court** : décisions,
raisons, pistes écartées, états. Pas de narration ; les contraintes durables vivent dans
`CLAUDE.md`, pas ici. Condensé le 19/09/2026 à la demande de Julien.

---

## État courant (18/10/2026)

| Élément | Valeur |
|---|---|
| Version en production | 2.1.1 / versionCode 10, publiée le 19/09/2026 (commit `f0586de`) |
| Chantier prioritaire | Obscurcissement DEX : livré. Reste à vérifier la disparition de l'alerte Play sous 48 h |
| Devices de test | S9 (Android 10) et S23+ (Android 16), builds hors Play Store, déclarés test AdMob |
| Dépôt distant | GitHub privé `isilmou/rtp-calculator`, branche `main`, créé le 18/10/2026. Sauvegarde des sources uniquement : pas de CI, pas d'hébergement du site. Push manuel par Julien après chaque commit |

### À faire

1. **Vérifier sous 48 h** dans la Play Console que l'alerte « Obscurcissement 0 % » a disparu
   sur la 2.1.1. Si elle persiste, relire le score par catégorie avant de toucher aux règles.
2. **Option restante de l'audit** : retirer `material-icons-extended` (AAR 35 Mo, seules
   `AccessTime` et `ContentCopy` sont hors jeu de base). Gain sur le build debug uniquement.
3. **Kotlin 2 / Compose récent** : chantier à part, après la 2.1.1.

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
  conservés. Sauvegarde de l'historique d'origine : `_backup/*.bundle` (ignoré par Git).
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
  le keystore, les AAB signés, les ZIP du site et les documents maîtres.
- **Claude Code lancé directement dans le dossier source** ; ce journal remplace la
  continuité du fil de conversation. Outillage icône : `generate_icons.mjs` + `logo/` + npm.
