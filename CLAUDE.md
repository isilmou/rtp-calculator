# RTP Calculator — Instructions projet

## 1. Contexte

RTP Calculator : calcul du TRJ (taux de retour au joueur) des paris sportifs.
Développeur solo : Julien. Deux livrables sous cette racine.

| Dossier | Contenu | Stack |
|---|---|---|
| `RTP Calculator - App` | Application Android publiée sur Google Play | Kotlin, Jetpack Compose, Gradle, Android Studio |
| `RTP Calculator - Website` | Site rtpcalculator.com | Site statique multilingue, hébergement Infomaniak |
| `stats/` | Collecte des statistiques Google (AdMob, GA4, Search Console, Play), lecture seule | Node 24, `googleapis` |
| `deploy/` | Déploiement du site vers Infomaniak (règle 8, § 6, § 8) | Node 24, `curl` |
| `tools/` | Outillage du dépôt : génération du sitemap (§ 6), extraction des textes du site vers Word et réinjection (`copy/`) | Node 24 |

Les noms de dossiers contiennent des espaces : toujours encadrer les chemins de guillemets.

## 2. Règles de travail — non négociables

1. **Une étape à la fois.** Proposer, attendre ma validation explicite, exécuter, s'arrêter.
   Jamais plusieurs modifications structurantes enchaînées sans validation intermédiaire.
2. **Diagnostic avant tactique.** Expliquer le « pourquoi » avant le « comment ».
   Évaluation honnête plutôt que rassurante : si une piste est mauvaise, le dire.
3. **Français = langue maîtresse.** Toute copie est rédigée en français, validée par moi,
   puis propagée vers EN / ES / DE / PT-BR. Ne jamais modifier une traduction si la source
   FR n'est pas à jour.
4. **Aucun commit ni push Git automatique.** Je commite et je pousse moi-même, après
   validation sur device. Dépôt distant : GitHub privé `isilmou/rtp-calculator`, branche
   `main`, sauvegarde des sources uniquement (pas de CI, pas d'hébergement du site).
5. **Aucune publication automatique.** Le build signé et l'envoi Play Console sont manuels.
6. Réponses structurées, tableaux pour les données comparatives, pas de jargon décoratif.
7. **Périmètre confiné à `C:\RTP-Calculator\`.**
   - Le travail se limite à cette racine et à ses sous-dossiers.
   - Ne jamais lire, écrire, lister ni explorer un chemin situé au-dessus de cette racine :
     ni `C:\`, ni le profil utilisateur, ni `Program Files`, ni un autre dossier de projet.
   - Ne jamais proposer `/cd` vers un dossier extérieur, ni demander l'ajout d'un répertoire
     supplémentaire (`--add-dir`, `/add-dir`) sans mon accord explicite et motivé.
   - Si une tâche semble exiger un fichier hors périmètre : **s'arrêter et me le demander**,
     en indiquant le chemin voulu et la raison. Ne pas contourner par une commande shell.
   - Les commandes shell obéissent à la même règle : pas de `cd ..`, pas de chemin absolu
     remontant hors de la racine, pas de script qui ouvre des fichiers hors périmètre.
   - Sauvegardes Git locales (`git bundle`) dans `C:\RTP-Calculator\_backup\` (ignoré par
     Git), jamais dans le scratchpad.
8. **Déploiement du site uniquement sur demande explicite de Julien**, diff montré avant tout
   envoi. `deploy/` ne supprime jamais rien à distance et n'envoie `.htaccess` que sur drapeau.

## 3. Journal de bord — `journal.md`

Un fichier `journal.md` est tenu à la racine `C:\RTP-Calculator\`. Il sert de mémoire
inter-sessions à Claude Code, qui repart d'un contexte vide à chaque lancement.

**Lecture : lire `journal.md` en début de session, avant toute action.**

**Écriture : mettre à jour `journal.md` sans attendre qu'on le demande, dès que**
- une décision technique est prise, ou écartée — noter la raison dans les deux cas ;
- un fichier structurant est créé, renommé ou supprimé ;
- une version est publiée (numéro, date, contenu) ;
- un bug est identifié, contourné ou corrigé ;
- une piste est abandonnée — écrire pourquoi, pour ne pas la rouvrir dans six mois ;
- une session se termine.

**Format** : entrées datées. Le journal ne contient que ce qu'une future session doit savoir :
état courant, décisions et leurs raisons, pistes écartées et pourquoi, points en suspens.
Pas de narration ni de compte rendu pas à pas. Une information devenue inutile est retirée,
une information devenue fausse est corrigée sur place. Les contraintes durables remontent
dans `CLAUDE.md` et ne sont pas dupliquées ici.

**Interdit dans `journal.md`** : mots de passe, clés API privées, identifiants FTP,
contenu du keystore ou mot de passe de signature.

### Maintenance de ce fichier

`CLAUDE.md` n'est pas figé : il décrit l'état des règles et des contraintes du projet, qui
évoluent. Le tenir à jour fait partie du travail.

**Proposer une mise à jour de `CLAUDE.md` dès que**
- une règle énoncée ici devient fausse (outil remplacé, chemin modifié, contrainte levée) ;
- une contrainte durable est découverte — un piège de build, une limite d'hébergeur, un
  comportement à ne jamais reproduire ;
- une convention nouvelle est adoptée (nommage, structure de dossiers, workflow) ;
- une zone interdite doit être ajoutée ou retirée de la section 7 ;
- la même correction m'a été demandée deux fois : c'est le signe qu'elle manque ici.

**Procédure** : proposer la modification, attendre ma validation (règle 1), puis éditer.
Ne jamais modifier `CLAUDE.md` de sa propre initiative sans me le soumettre.

**Frontière entre les deux fichiers**
- `CLAUDE.md` : ce qui est **permanent** — règles, conventions, contraintes, interdits.
- `journal.md` : ce qui est **daté** — décisions, avancement, bugs, versions publiées.

Une information durable issue du journal doit remonter dans `CLAUDE.md`. Garder ce fichier
sous 200 lignes : au-delà, l'adhérence aux instructions se dégrade. Si la taille devient un
problème, condenser ou déplacer un sujet vers `.claude/rules/`.

## 4. Versioning

- patch `x.x.1` : correction de bug
- mineure `x.1.0` : nouvelle fonctionnalité ou nouvelle langue
- version en production au 19/09/2026 : **2.1.1** (versionCode 10, publiée le 19/09/2026)

## 5. Chaîne de publication Android

1. Tests sur devices physiques : Samsung Galaxy S9 **et** S23+ — obligatoire avant tout
   build de release. Le S9 est le garde-fou sur les vieilles API et la mémoire.
2. Commit Git puis `git push` (par moi).
3. AAB signé via l'assistant Android Studio.
4. Play Console. **Publication gérée activée** : après approbation, la mise en ligne exige
   un clic explicite sur « Publier les modifications ». Ne jamais considérer une release
   comme live avant ce clic.

**Tests de release** : pièges vérifiés (variante debug sans R8, signatures incompatibles,
migration de données, appareils de test AdMob) dans `.claude/rules/android-release.md`,
chargé automatiquement pour tout travail sous `RTP Calculator - App/`.

**Keystore** : `rtp-calculator.jks`, stocké en local dans `C:\RTP-Calculator-Keystore\`.
Cet emplacement est **hors du périmètre de travail défini en règle 7** — c'est délibéré.
Claude Code n'y accède jamais : la signature se fait manuellement via l'assistant Android
Studio. Ne jamais demander l'ajout de ce dossier en répertoire supplémentaire, ne jamais
copier le keystore dans `C:\RTP-Calculator\`, ne jamais le commiter, ne jamais écrire son
mot de passe dans un fichier du dépôt.

## 6. Site web — contraintes connues

- **`.htaccess` / Infomaniak** : le proxy masque `%{HTTPS}` (toujours lu `off`). Les
  conditions de redirection doivent utiliser `X-Forwarded-Proto`, et les cibles doivent
  être **absolues** — une cible relative est reconstruite en `http://` derrière le proxy.
- **Offre Starter** : FTP en clair port 21 seulement, ni TLS ni SSH. Déploiement par `deploy/`
  (curl, `manifest.json` versionné = état de la production, vérification HTTPS, texte normalisé
  en LF car `core.autocrlf=true` livre du CRLF). Secours : ZIP + extraction dans le Manager.
- **GA4** (`G-WD1V10FW80`) : la chaîne Consent Mode est critique. Sans CMP, la collecte est
  nulle — signature `gcs=G100` dans les requêtes réseau. Si AdSense est un jour approuvé,
  désactiver la bannière RGPD autonome pour éviter le conflit de double bandeau.
- SEO : le canal web attire du trafic Tier 1 (US, France, Canada, Danemark), contrairement
  à l'ASO qui apporte majoritairement du Tier 3-4. Ne pas dégrader l'existant SEO.
- **Typographie : aucun tiret long** (« — »). Titres « A - B », incises par virgules ou deux-points, « · » en pied de page.
- **`sitemap.xml` est généré**, jamais édité à la main : `node tools/sitemap.mjs` (`loc` = canonical,
  `lastmod` = dernier commit Git, hreflang = balises des pages). À relancer après toute
  modification de page, avant le déploiement ; `--check` vérifie sans écrire.

## 7. Zones interdites sans validation explicite

- Configuration de signature, `signingConfig`, keystore
- `applicationId` et nom de package
- Identifiants GA4 et AdMob (App ID `ca-app-pub-2303541955549784~...`)
- **Icône adaptative** : le VectorDrawable de premier plan utilise un scale de groupe
  calibré manuellement à `0.65` pour la zone de sécurité 72dp dans le canvas 108dp.
  Ne jamais le régénérer depuis le SVG maître.
- Suppression ou fusion de fichiers de traduction
- `app/proguard-rules.pro` : les règles `keep` du package `model` protègent l'historique Gson
  des utilisateurs (noms de champs = clés JSON). Ne jamais les retirer ni les remplacer par une
  règle globale, qui ramènerait le score d'obscurcissement à zéro.
- Toute modification du `.htaccess` de production
- `_secrets/` (client OAuth et jeton Google ; identifiants FTP `netrc` et `deploy.json`) :
  lecture interdite à Claude Code par `settings.json`, jamais commité. Seuls les scripts de
  `stats/` et `deploy/` y accèdent.

## 8. Commandes utiles

```powershell
# Depuis "C:\RTP-Calculator\RTP Calculator - App"
.\gradlew.bat assembleDebug          # build debug
.\gradlew.bat assembleRelease        # APK release — vérifier le comportement post-R8
.\gradlew.bat bundleRelease          # AAB (la signature reste faite via Android Studio)
.\gradlew.bat clean                  # en cas de cache Gradle incohérent

# Site (règle 8) : check = connexion FTP, init = relecture HTTPS + manifeste, diff = dry-run
npm --prefix "C:\RTP-Calculator\deploy" run check|init|diff|push -- --apply [--only=<chemins>] [--htaccess] [--active]

# Textes du site : Word relu par Julien (tools/copy/out/), apply = à blanc sans --apply
npm --prefix "C:\RTP-Calculator\tools" run copy:extract
npm --prefix "C:\RTP-Calculator\tools" run copy:apply -- <docx> [--apply] [--empty-ok]
```

Aucun JDK dans le PATH : préfixer Gradle de `JAVA_HOME="C:/Program Files/Android/Android Studio/jbr"`
et appeler `adb` par son chemin complet (`sdk.dir` de `local.properties` + `platform-tools/adb.exe`).
Ces deux chemins hors périmètre sont autorisés par Julien (19/09/2026) en lecture et exécution seules.

## 9. Statistiques Google

Avant toute analyse, Claude lance lui-même la collecte, sans rien demander à Julien :

```powershell
npm --prefix "C:\RTP-Calculator\stats" run snapshot                    # 28 derniers jours
npm --prefix "C:\RTP-Calculator\stats" run snapshot -- --all           # historique complet
npm --prefix "C:\RTP-Calculator\stats" run snapshot -- --source=gsc    # admob | ga4 | gsc | play
npm --prefix "C:\RTP-Calculator\stats" run snapshot -- --source=inspect # indexation par URL, hors défaut
```

Résultats non versionnés dans `stats/data/AAAA-MM-JJ/` ; identifiants non secrets dans `stats/config.json`.
- Projet Google Cloud `rtp-calculator-stats`, **sans compte de facturation** : ne jamais en
  associer un, ne jamais saisir de carte. Toutes les API utilisées sont gratuites.
- Jeton OAuth auto-renouvelé. Relancer `npm run auth` (navigateur, compte
  contact@rtpcalculator.com) seulement si l'accès est révoqué ou inutilisé 6 mois.
- Données en retard de 1 à 3 jours selon la source ; GA4 minoré par le Consent Mode ;
  vitals Play vides tant que l'app reste sous le seuil d'utilisateurs de Google.

## 10. Chantier en cours

Plan d'amélioration validé le 23/10/2026 (`stats/reports/2026-10-23-analyse-globale.md`) ;
ordre d'exécution et état courant dans `journal.md`.
