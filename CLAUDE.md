# RTP Calculator — Instructions projet

## 1. Contexte

RTP Calculator : calcul du TRJ (taux de retour au joueur) des paris sportifs.
Développeur solo : Julien. Deux livrables sous cette racine.

| Dossier | Contenu | Stack |
|---|---|---|
| `RTP Calculator - App` | Application Android publiée sur Google Play | Kotlin, Jetpack Compose, Gradle, Android Studio |
| `RTP Calculator - Website` | Site rtpcalculator.com | Site statique multilingue, hébergement Infomaniak |

Les noms de dossiers contiennent des espaces : encadrer systématiquement les chemins
de guillemets dans les commandes PowerShell.

## 2. Règles de travail — non négociables

1. **Une étape à la fois.** Proposer, attendre ma validation explicite, exécuter, s'arrêter.
   Jamais plusieurs modifications structurantes enchaînées sans validation intermédiaire.
2. **Diagnostic avant tactique.** Expliquer le « pourquoi » avant le « comment ».
   Évaluation honnête plutôt que rassurante : si une piste est mauvaise, le dire.
3. **Français = langue maîtresse.** Toute copie est rédigée en français, validée par moi,
   puis propagée vers EN / ES / DE / PT-BR. Ne jamais modifier une traduction si la source
   FR n'est pas à jour.
4. **Aucun commit Git automatique.** Je commite moi-même, après validation sur device.
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

**Format** : entrées datées, la plus récente en haut de la section « Sessions ».
Factuel et court. On ajoute, on ne réécrit jamais l'historique : une information
devenue fausse est corrigée par une nouvelle entrée qui dit qu'elle l'est.

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
- version en production au 19/09/2026 : **2.1.0**
- version préparée le 19/09/2026, non encore publiée : **2.1.1** (versionCode 10). Mettre cette
  ligne à jour après le clic « Publier les modifications ».

## 5. Chaîne de publication Android

1. Tests sur devices physiques : Samsung Galaxy S9 **et** S23+ — obligatoire avant tout
   build de release. Le S9 est le garde-fou sur les vieilles API et la mémoire.
2. Commit Git (par moi).
3. AAB signé via l'assistant Android Studio.
4. Play Console. **Publication gérée activée** : après approbation, la mise en ligne exige
   un clic explicite sur « Publier les modifications ». Ne jamais considérer une release
   comme live avant ce clic.

**Tests de release — pièges vérifiés le 19/09/2026**
- Un « Run » Android Studio installe la variante **debug**, où R8 est inactif quel que soit le
  réglage (AGP désactive l'obscurcissement des builds debuggables). Un test d'obscurcissement
  exige un APK release signé via l'assistant, installé par `adb install -r`.
- Debug et release ont des signatures incompatibles : passer de l'un à l'autre impose une
  désinstallation, donc la perte des données locales. Sauvegarder `rtp_history.xml` avant.
- Les deux devices ne portent pas la version Play Store. Un test de migration de données se
  fait avec un build `debug { isMinifyEnabled = true; isDebuggable = false }` **temporaire**,
  jamais commité : signé en debug, donc installable par-dessus sans perte.
- **AdMob** : les appareils de test sont déclarés dans `MainActivity` via
  `setTestDeviceIds` (empreintes SDK, pas des secrets). L'enregistrement en console AdMob ne
  suffit pas. Ne jamais cliquer une annonce d'un build de test tant que logcat n'affiche pas
  « This request is sent from a test device ».

**Keystore** : `rtp-calculator.jks`, stocké en local dans `C:\RTP-Calculator-Keystore\`.
Cet emplacement est **hors du périmètre de travail défini en règle 7** — c'est délibéré.
Claude Code n'y accède jamais : la signature se fait manuellement via l'assistant Android
Studio. Ne jamais demander l'ajout de ce dossier en répertoire supplémentaire, ne jamais
copier le keystore dans `C:\RTP-Calculator\`, ne jamais le commiter, ne jamais écrire son
mot de passe dans un fichier du dépôt.

Historique : le keystore était auparavant sur Google Drive ; en streaming, la signature
échouait avec `block not properly padded`. Le stockage local supprime ce problème.

## 6. Site web — contraintes connues

- **`.htaccess` / Infomaniak** : le proxy masque `%{HTTPS}` (toujours lu `off`). Les
  conditions de redirection doivent utiliser `X-Forwarded-Proto`, et les cibles doivent
  être **absolues** — une cible relative est reconstruite en `http://` derrière le proxy.
- **FTP** : FileZilla sur le port 21 sans TLS gèle fréquemment. Contournement fiable :
  upload via navigateur puis extraction côté serveur dans Infomaniak Manager.
- **GA4** (`G-WD1V10FW80`) : la chaîne Consent Mode est critique. Sans CMP, la collecte est
  nulle — signature `gcs=G100` dans les requêtes réseau. Si AdSense est un jour approuvé,
  désactiver la bannière RGPD autonome pour éviter le conflit de double bandeau.
- SEO : le canal web attire du trafic Tier 1 (US, France, Canada, Danemark), contrairement
  à l'ASO qui apporte majoritairement du Tier 3-4. Ne pas dégrader l'existant SEO.

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

## 8. Commandes utiles

```powershell
# Depuis "C:\RTP-Calculator\RTP Calculator - App"
.\gradlew.bat assembleDebug          # build debug
.\gradlew.bat assembleRelease        # APK release — vérifier le comportement post-R8
.\gradlew.bat bundleRelease          # AAB (la signature reste faite via Android Studio)
.\gradlew.bat clean                  # en cas de cache Gradle incohérent
```

Aucun JDK dans le PATH du shell Claude Code. Préfixer les commandes Gradle de
`JAVA_HOME="C:/Program Files/Android/Android Studio/jbr"` (JDK embarqué d'Android Studio) et
appeler `adb` par son chemin complet dans le SDK (`sdk.dir` de `local.properties`,
`platform-tools/adb.exe`). Ces deux chemins sont hors périmètre : autorisation donnée par Julien
le 19/09/2026, en lecture et exécution uniquement, sans écriture.

## 9. Chantier prioritaire en cours

Obscurcissement DEX sous le seuil Google Play (échéance février 2027). Côté code, c'est fait
le 19/09/2026 (R8 actif, 2.1.1). Reste : publication de la 2.1.1 et vérification de la
disparition de l'alerte dans la Play Console. État détaillé : voir `journal.md`.
