# RTP Calculator — Journal de bord

Mémoire inter-sessions de Claude Code. Lu en début de session. Tenu **court** : décisions,
raisons, pistes écartées, états. Pas de narration ; les contraintes durables vivent dans
`CLAUDE.md`, pas ici. Condensé le 19/09/2026 à la demande de Julien.

---

## État courant (25/10/2026)

| Élément | Valeur |
|---|---|
| Version en production | 2.1.1 / versionCode 10, publiée le 19/09/2026 (commit `f0586de`) |
| Obscurcissement DEX | Livré en 2.1.1 ; **alerte Play « 0 % » disparue, vérifié par Julien le 25/10/2026**. Clos |
| Statistiques Google | Collecte opérationnelle depuis le 23/10/2026 (`stats/`, 4 sources). Analyse initiale faite le 23/10/2026 : `stats/reports/2026-10-23-analyse-globale.md` |
| Chantier en cours | **Refonte du contenu du site livrée et déployée le 25/10/2026** (étape 8 ci-dessous : 45 pages, 4 guides, menu à 4 entrées, testée OK sur S9 et S23+ ; commit à faire par Julien). Plan d'amélioration validé le 23/10/2026 (ordre ci-dessous). Étapes 2 (lot CTR, `6e83ce9`) et 3 (sitemap généré, source `inspect`, réindexation demandée) faites le 24/10/2026. Intercalés le 24/10 : **présentation du site** (lots 1a → 6 livrés et déployés, `da63d6c`) puis **relecture de tous les textes par Julien** (Word rendu et réinjecté le 24/10 au soir, tirets longs supprimés, guide « marge » réécrit en 5 langues : **commité `2d62671`, déployé le 24/10 au soir** ; section dédiée ci-dessous) ; **étape 4 (fiche Play) : terminée, nouvelle fiche publiée le 25/10/2026** (envoi par l'API, approbation et clic « Publier » le même jour). Repère pour le bilan : installs Brésil et désinstalls J+1 à partir du 26/10. **Étapes 5 et 6 closes le 25/10/2026** sans livrable (détail ci-dessous) ; **étape 7 en attente du bilan du 06/11**. Contrôle `inspect` vers le 31/10, **bilan début novembre (point convenu « dans une semaine ou deux », 25/10)** |
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
   HTTPS, diff à blanc vide ensuite ; `manifest.json` à jour), commit `6e83ce9`.
   Piège rencontré : la relecture HTTPS (`fetch` Node) échoue parfois avec « fetch failed » alors
   que le fichier est bien en ligne ; `verifyPublished` traite désormais l'exception comme une
   tentative ratée (3 essais) au lieu d'interrompre le déploiement. Après une interruption, le
   manifeste ne contient que les fichiers vérifiés : relancer `push` réenvoie le reste, sans risque.
3. **Sitemap, inspection, réindexation** — en cours.
   - 3a fait le 24/10/2026, **local, non déployé** : `tools/sitemap.mjs` (Node sans dépendance,
     `--check` pour vérifier sans écrire) régénère `sitemap.xml` depuis les 55 `index.html` :
     `loc` = balise canonical (contrôlée contre le chemin), `lastmod` = date du dernier commit
     Git du fichier (aujourd'hui si modifié non commité), `xhtml:link` = hreflang du `<head>`,
     réciprocité des groupes vérifiée (55 pages, 385 liens, 0 erreur). Sitemap passé de 58 à
     609 lignes, LF, mêmes 55 URL. **Ne plus éditer `sitemap.xml` à la main** : relancer le
     script après chaque modification de page, avant le déploiement.
   - 3b fait le 24/10/2026 : source `inspect` (`stats/fetch/inspect.mjs`, API URL Inspection,
     scope `webmasters.readonly` suffisant, pas de ré-autorisation). Hors collecte par défaut :
     `npm run snapshot -- --source=inspect` (59 appels ≈ 100 s ; quota 2 000/jour). URL = `<loc>`
     du sitemap local + `config.inspect.extraUrls` (4 variantes http/www). Résultat de
     référence `stats/data/2026-10-24/inspect.json`, **avant** déploiement du nouveau sitemap :
     45/55 pages indexées ; non indexées : `/app/`, `/fr/app/`, `/pt/app/` (les 3 calculateurs !
     `/fr/app/` et `/pt/app/` jamais vues par Google), `/es/` (explorée le 18/10, non indexée),
     `/fr/privacy/`, `/es/privacy/`, `/de/privacy/`, `/pt/sobre/`, 2 guides PT. Variantes
     `http://` et `www.` : redirection ou canonical `https://` correctement reconnus par Google,
     rien à corriger, le rapport Pages est en retard. Aucune page recrawlée depuis le lot CTR
     du 24/10 (dernier crawl de `/fr/` : 18/10). Mobile usability / rich results : non renseignés
     par l'API (`VERDICT_UNSPECIFIED`), champ `sitemap` incomplet (20 sur 55), à ne pas interpréter.
     Conséquence pour 3c : demander l'indexation de `/fr/app/` et `/app/` en plus de `/`, `/fr/`, `/es/`.
   - 3c fait le 24/10/2026 : `sitemap.xml` déployé seul (vérifié en HTTPS, diff à blanc vide,
     `manifest.json` à jour, 64 fichiers en production). **Aucune API de réindexation** pour un
     site classique (Indexing API réservée aux offres d'emploi et directs ; ping sitemap supprimé
     en 2023) : clics de Julien dans Search Console. Jeton `stats/` maintenu en lecture seule.
     Piège : Julien avait resoumis le sitemap **avant** l'envoi (ancienne version relue) ;
     resoumis après, puis demandes d'indexation faites le 24/10 pour `/fr/app/`, `/app/`, `/es/`,
     `/fr/` (déjà indexée selon la console), `/` (quota ≈ 10/jour). **Contrôle vers le 31/10/2026** :
     `npm run snapshot -- --source=inspect` puis comparer avec `data/2026-10-24/inspect.json`
     (recrawl des 5 URL, indexation des calculateurs, disparition des variantes http/www du
     rapport Pages).
   - 3d fait le 24/10/2026 : `CLAUDE.md` § 1 (`tools/`), § 6 (sitemap généré), § 9 (`inspect`).
     **Étape 3 terminée et commitée (`6b662e7`).** `CLAUDE.md` condensé le 24/10/2026 : les pièges
     de release du § 5 sont dans `.claude/rules/android-release.md` (chargé pour `RTP Calculator - App/**`) ; 199 lignes.
4. **Fiche Play** : lever l'ambiguïté casino dès le titre, 5 langues. **En cours (25/10/2026).**
   - Texte actuel lu par l'API Android Publisher : source `listing` (`stats/fetch/listing.mjs`,
     hors collecte par défaut, `npm run snapshot -- --source=listing`). L'API ne lit une fiche
     qu'à travers un brouillon (`edits.insert` → `listings.list` → `edits.delete` en `finally`,
     jamais `commit`) : rien n'est modifié dans la console. Jeton existant suffisant (portée
     `androidpublisher` déjà accordée pour les avis). Résultat : `stats/data/2026-10-25/listing.json`.
   - **Constat qui nuance le rapport du 23/10** : la fiche EN (langue par défaut, donc celle vue
     au Myanmar, 32 installs en `en_US`) dit déjà « RTP Calculator for sports bets » (30/30) et
     « sports bet » en description courte ; DE et ES aussi. Seuls les titres **FR « Calculateur de
     TRJ » (18/30)** et **PT-BR « Calculadora de RTP » (18/30)** ne nomment pas les paris sportifs ;
     PT-BR est le vrai trou (Brésil, 16 installs, « RTP » = machines à sous). Le titre EN n'est donc
     pas le levier contre le Myanmar : regarder icône / captures (premier écran vu) et accepter que
     des installs « RTP » casino persistent. Descriptions longues : tirets longs (FR, EN, PT), puces
     « • » en FR contre « - » ailleurs, ligne « Design entièrement repensé » datée (note de version),
     `contactWebsite` en `http://`.
   - **Plan validé le 25/10/2026** (`~/.claude/plans/ne-lis-pas-pour-recursive-elephant.md`), textes
     construits le même jour : `RTP Calculator - App/play/listings/<langue>/{title,short,full}.txt`
     (15 fichiers, LF, source de vérité de la fiche, convention proche de fastlane). Titres : FR
     « Calculateur TRJ paris sportifs » (30/30, contient « calculateur trj » et « trj paris sportifs »),
     PT-BR « Calculadora RTP - Apostas » (25/30, modèle ES) ; EN / DE / ES inchangés. Descriptions
     courtes inchangées. Descriptions longues (5) : tirets longs et demi-cadratins remplacés par
     deux-points ou virgules, apostrophes droites, puces « • » partout (FR en avait, les autres « - »),
     ligne « Design entièrement repensé » neutralisée, « value-bet » → « value bet », ligne « Aussi
     disponible sur navigateur, sans installation : rtpcalculator.com » avant l'appel final. FR seul :
     phrase ANJ 85 % dans « Qu'est-ce que le TRJ ? » (décision de Julien : pas de variante ailleurs).
     Longueurs : 2 592 à 3 143 / 4 000, 15 puces et 5 rubriques dans chaque langue. Script de
     construction en scratchpad (remplacements à occurrence unique, contrôles avant écriture).
   - **Écarté (Julien, 25/10)** : phrase « pas un outil casino / machines à sous » dans les
     descriptions. Lecture déjà claire, et ces mots-clés feraient remonter la fiche sur les
     recherches casino de Play. Ne pas rouvrir. Icône et captures : non lues pour l'instant.
   - **Envoi par l'API tenté le 25/10/2026** : `stats/listing-push.mjs` (`npm run listing:push`, à blanc
     par défaut, `--apply` pour écrire ; garde-fous longueurs / typographie / langues). Objectif : des
     « modifications non envoyées » dans la console, par `edits.commit` avec `changesNotSentForReview`.
     **Refusé par Google** (400 : « Changes are sent for review automatically. The query parameter
     changesNotSentForReview must not be set ») : ce drapeau n'existe que pour les apps où l'envoi
     automatique est désactivé. Brouillon supprimé par le `finally`, fiche en ligne vérifiée inchangée
     (lecture à blanc : mêmes 8 écarts). Seule voie API restante : `commit` sans drapeau = envoi en
     examen immédiat, publication toujours manuelle (publication gérée). `changesInReviewBehavior` =
     `ERROR_IF_IN_REVIEW` (le défaut annulerait des changements en examen).
   - **Envoyé en examen le 25/10/2026** (voie choisie par Julien) : `listing:push --apply` sans le drapeau,
     8 champs écrits (titres FR et PT-BR, 5 descriptions longues, `contactWebsite` en `https://`),
     commit accepté. Relecture à blanc ensuite : « Aucun écart », la fiche lue par l'API est identique
     aux fichiers `play/listings/`. Première écriture du jeton `stats/` dans la Play Console.
     `stats/data/2026-10-25/listing.json` = version en examen ; `listing-avant.json` = fiche d'avant
     (reconstituée). Piège : la collecte `listing` écrase le JSON du jour, archiver avant un envoi.
   - **Fiche publiée le 25/10/2026** (approbation Google et clic « Publier » le jour même). Étape 4
     terminée. Commits `064a1b7` (outillage, fiche, CLAUDE.md) et `f122155` (journal), poussés.
5. ~~**Guides FR**~~ : **close le 25/10/2026 sans nouvelle page.**
   - « Comment calculer le TRJ d'un pari sportif » : **écarté, cannibalisation**. `calculer-marge-bookmaker`
     porte ce titre depuis le 24/10 (formule, 2 exemples), `trj-pari-sportif` contient aussi la
     méthode, et « calcul trj » (70 impressions, pos. 9 à 10, 0 clic) comme « calculateur trj »
     atterrissent sur `/fr/` (Search Console 16 mois, `stats/data/2026-10-25/gsc.json`). Une 3e
     page diluerait le signal. Mesurer l'effet du lot CTR et du guide « marge » au bilan du 06/11.
   - « TRJ et plafond légal de 85 % (ANJ) » : **écarté par Julien**. Aurait été la première page FR
     seule ; `tools/sitemap.mjs` l'accepterait avec `hreflang="fr"` et `x-default` vers elle-même.
   - Si l'idée d'un guide revient : probabilité implicite d'une cote, surebet / arbitrage, value
     bet, à vérifier contre l'existant d'abord. Le lexique est le guide FR le plus vu (64 impressions,
     pos. 8,2).
6. ~~**App 2.2.0**~~ : **abandonnée pour l'instant (Julien, 25/10/2026).** Production inchangée :
   2.1.1 / code 10.
   - **Pas d'analytics in-app** (Firebase) : décidé. 7 appareils actifs, promesse « 100 % hors
     ligne » de la fiche, déclaration Sécurité des données et politique de confidentialité à
     modifier sinon. À réexaminer si l'app dépasse un seuil d'utilisateurs actifs.
   - **Marge non affichée** : abandonné, ne pas rouvrir. Elle avait été retirée du formulaire lors
     de la refonte graphique (`BetAnalysisCard.kt`, « input screen as quiet as possible ») ; Julien
     confirme ce choix.
   - **Surebet et URL du site dans l'image partagée** : principe accepté, **pour une prochaine mise à
     jour**. Proposition prête : « TRJ 102.1% · Surebet +2.1% » dans `ui/RtpPill.kt` (composant
     partagé par formulaire, historique et image ; fond inchangé) ; signature de l'image
     « Calculateur de TRJ · rtpcalculator.com » à la place de « Disponible sur le Play Store »
     (les deux ne tiennent pas sur la ligne de 10 sp), chaîne `share_signature_site` non traduisible ;
     version 2.2.0 / code 11 ; notes de version à versionner dans `play/release-notes/<version>/`.
     `RtpResult` n'est pas persisté : ni ProGuard ni migration.
7. **Repositionnement EN** (bookmaker margin / overround), puis `npm run report`. **En attente
   (Julien, 25/10/2026)** : ne pas commencer avant le bilan du 06/11, qui mesurera d'abord l'effet du
   lot CTR et de la fiche Play. Question à trancher à ce moment : la « marge dans le calculateur
   web » prévue par le rapport, alors que la marge a été refusée dans l'app (écran calme) ; l'étape
   peut se limiter aux titres et textes EN.

8. ~~**Refonte du contenu du site (audit du 25/10/2026)**~~ : **livrée, déployée et testée OK sur S9 et
   S23+ le 25/10/2026** (plan validé par Julien le 25/10, `~/.claude/plans/j-aimerais-un-audit-du-linear-candle.md`,
   détail dans la section dédiée ci-dessous). Reste : indexation Search Console et commit par Julien,
   bilan fin novembre 2026.
   **Décision : tout faire maintenant, sans attendre le bilan du 06/11**, qui mesurera donc l'ensemble
   des changements d'octobre sans isoler le lot CTR.

Hors plan, toujours valables :
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

## Refonte du contenu du site (25/10/2026)

Demande de Julien : contenu redondant, lexique hors sujet, menu mobile sur deux lignes, centrage des
titres à étudier. Audit par un **agent neutre de tout contexte** (prompt de Julien + chemin du site
seulement), croisé avec Search Console 16 mois (`stats/data/2026-10-25/gsc.json`).

Constats retenus : `trj-pari-sportif` et `calculer-marge-bookmaker` sont un même article coupé en deux
(une phrase identique mot pour mot, mêmes exemples) ; formule écrite 5 fois sur le site, ANJ 85 % 3 fois,
liste des 6 guides affichée 4 fois ; lexique : 14 termes employés sur le site, 16 jamais (dutching,
Kelly, CLV…), mais page guide FR la plus vue (64 impressions, 1 clic) ; menu : 6 entrées dont 3 ancres
vers l'accueil, ≈ 545 px de texte pour 320 px ; aucun titre centré.

**Décisions (Julien, 25/10/2026)** :
- Fusion des deux articles TRJ sur la famille d'URL **`calculer-marge-bookmaker`** (seule positionnée en
  FR : 18 impressions pos. 8,9 ; `trj-pari-sportif` : 0 impression FR) ; 301 des 5 URL abandonnées dans
  `.htaccess` (première règle de page, validation explicite). Revient sur la décision du 24/10 (deux angles).
- Guide « comparer les bookmakers plutôt que les promotions » : **angle abandonné** (parieurs
  multi-comptes, le choix d'un opérateur n'est pas la question ; 1 impression FR en 16 mois). Réécrit
  sous l'angle « pour un pari donné, chez lequel de mes opérateurs le marché est le moins cher »,
  URL conservée. Écarté : suppression avec 301.
- Lexique réduit de 30 à 14 entrées (TRJ, marge, probabilité implicite, cote, mise, bookmaker, combiné,
  1X2, double chance, over/under, handicap, score exact, surebet, cote boostée), URL conservée. Perte
  de longue traîne acceptée. Écarté : suppression de la page.
- Menu mobile : **4 entrées** (Calculateur · Guides · Application · À propos), Confidentialité et
  Contact au pied de page, après mesure à 360 / 375 px. Écarté : hamburger (déjà le 24/10), défilement
  horizontal. Revient sur le choix « deux lignes » du 24/10.
- Centrage : zones vitrine (sections de l'accueil, héros empilé, têtes de l'index et du calculateur)
  centrées ; articles, À propos, confidentialité à gauche. CSS seul.
- Autres : bouton Play sous le formulaire du calculateur, encadré de formules retiré de l'À propos,
  confidentialité dédoublonnée, titres de guides sous 60 caractères, « À lire ensuite » à 2 liens,
  menu déroulant Guides supprimé, 5 cartes à l'accueil. Cible : 10 URL par langue au lieu de 11.

**Étape 1 validée par Julien le 25/10/2026, avec corrections du plan initial** :
- Titres FR : article TRJ fusionné « Le TRJ d'un pari sportif : définition et calcul » ; combinés
  « Paris combinés : une mine d'or pour les bookmakers » ; formats de cotes **inchangé** (77 caractères,
  choix de Julien) ; lexique : H1 « Lexique des paris sportifs » inchangé.
- **Lexique : les 30 termes sont conservés** (revient sur la réduction à 14). Deux définitions à enrichir :
  dutching (permet d'équilibrer ses gains en cas de surebet), surebet (peut parfois s'obtenir grâce à
  des promotions ou des cotes boostées). Le `<title>` « 30 termes » reste donc exact.
- **Guide « comparer » supprimé** (5 langues, « peu d'utilité »), 301 vers l'article TRJ fusionné, qui
  absorbe l'usage « comparer ses opérateurs sur un même match ». Écarté : réécriture multi-comptes.
  Cible : **9 URL par langue, 4 guides** (TRJ, combinés, formats, lexique). 10 règles 301 au total.

**Étape 2 faite le 25/10/2026 (local, non commité, non déployé)** : article fusionné écrit sur
`calculer-marge-bookmaker` et ses 4 équivalents (texte FR relu et amendé par Julien : intro, phrase
sur la conversion automatique des formats, usages du TRJ ramenés à 3 phrases, lien combinés retiré du
dernier paragraphe ; `title`, metas, `headline`, `dateModified` 2026-10-25, 9 H2, 17 `strong`
équilibrés dans chaque langue). Traductions assemblées à partir des paragraphes déjà traduits des deux
articles sources, seules les phrases nouvelles ont été traduites. Au passage (demande de Julien) :
« , sans inscription » et équivalents retirés de la phrase du bloc `.cta` (7 pages × 5 langues = 35
fichiers) ; les mentions de l'accueil et des metas sont conservées. Scripts : `scratchpad/apply-step2.mjs`,
`apply-step2b.mjs`. Les anciennes pages `trj-pari-sportif` et `comparer-bookmakers-trj` existent
encore et les liens internes pointent toujours vers elles : étape 3.

**Étape 3 faite le 25/10/2026 (local, non commité, non déployé)** : 10 dossiers supprimés
(`trj-pari-sportif`, `comparer-bookmakers-trj` et équivalents EN/ES/DE/PT), **45 pages** restantes,
0 référence résiduelle aux 10 URL. 35 fichiers modifiés par `scratchpad/apply-step3.mjs` : menu
déroulant Guides des 5 accueils remplacé par un lien simple (identique aux autres pages) ; cartes des
5 accueils et des 5 index : 6 → 4, carte TRJ fusionné = nouveau titre + résumé de l'ancienne carte
« C'est quoi » ; nouveau titre des combinés appliqué (`title`, `og:title`, H1, `headline`, 5 langues :
« Paris combinés : une mine d'or pour les bookmakers », « Accumulators: a gold mine for bookmakers »,
« Apuestas combinadas: una mina de oro… », « Kombiwetten: eine Goldgrube für Buchmacher », « Apostas
múltiplas: uma mina de ouro… ») ; « À lire ensuite » à 2 liens (TRJ → combinés + formats ; combinés →
TRJ + formats ; formats → TRJ + combinés ; lexique → TRJ + formats) ; « Pour aller plus loin » du
calculateur → TRJ fusionné · formats · combinés ; lexique : phrase « Voir le guide comparer… » retirée
de l'entrée Bonus, ancres « c'est quoi le TRJ » → titre fusionné. `sitemap.mjs --check` : 45 pages,
315 hreflang, réciprocité OK, à régénérer (étape 9). **Bug trouvé** : `pt/guides/apostas-multiplas-rtp`
l. 107 contient une fin de phrase en espagnol (« y, si lo deseas, comprobar por ti mismo… ») : à corriger
(étape 4 ou 5). Les 10 pages supprimées restent en ligne jusqu'au déploiement ; `deploy/` ne supprime
jamais à distance : les 301 (étape 9) s'appliqueront avant la lecture du fichier, suppression distante
manuelle par Julien dans le Manager Infomaniak si souhaitée.

**Étape 4 faite le 25/10/2026 (local)** : lexiques des 5 langues (`scratchpad/apply-step4.mjs`) :
dutching (« la technique qui permet d'équilibrer ses gains en cas de surebet ») et surebet (« reste rare
mais peut s'obtenir grâce aux promotions ou cotes boostées proposées par les bookmakers », phrase de
Julien) enrichis ; ancres « Voir … » vers l'article TRJ (4 par langue) et vers les combinés prennent
les nouveaux titres ; « Mis à jour le 25 octobre 2026 » et `dateModified`. Bug PT des combinés corrigé
(fin de phrase traduite en portugais). Les 30 termes et le `<title>` « 30 termes » sont conservés.

**Étape 5 faite le 25/10/2026 (local)**, 15 fichiers (`scratchpad/apply-step5.mjs`) : calculateur
(5) : bouton Play retiré de `.page-head`, replacé en fin de `.seo-text` après « Pour aller plus loin » ;
exemple du texte explicatif 2,10 / 3,40 / 3,60 → 2,00 / 3,50 / 3,80 (104,9 %, TRJ 95,3 %, marge 4,7 %),
phrase ANJ conservée. À propos (5) : encadré `.ex` des formules + paragraphe « exemples publiés » →
un paragraphe avec lien vers l'article TRJ ; `<title>` / `og:title` / descriptions « Calculateur de
TRJ, un outil indépendant » (EN inchangé : « RTP Calculator » est le nom) ; « Chaque guide indique sa
date de publication et de mise à jour ». Confidentialité (5) : § 1 « application Android et site web
(rtpcalculator.com) », suppression § 1 et § 2 par renvoi aux sections 2 et 3, « Dernière mise à jour :
25 octobre 2026 », ancre `#data-deletion` et section 5 inchangées.

**Étape 6 faite le 25/10/2026 (local)**, 5 accueils (`scratchpad/apply-step6.mjs`) : « Sans
inscription » retiré de la lede du héros (reste dans la section Application) ; lede de la section
Guides raccourcie (« Différents articles pour mieux apprécier l'importance du TRJ pour les parieurs »,
validé par Julien cette fois) ; lien « En savoir plus sur ce site → » vers l'À propos à la fin de la
note « Pour qui ? » ; lien Confidentialité ajouté au pied des accueils (libellé repris du pied du
calculateur de chaque langue).

**Étape 7 faite le 25/10/2026 (local)**, 45 pages (`scratchpad/apply-step7.mjs`). **Mesure préalable
dans Chromium** (`scratchpad/measure-menu.mjs`, Playwright de `tools/`, DOM modifié en mémoire) : menu
actuel à 6 entrées = 2 lignes à 360 et 375 px dans les 5 langues (texte + écarts 381 à 502 px pour
320 à 335 px disponibles), en-tête 106 px ; 4 entrées = 1 ligne partout (max FR 285 px), en-tête 81 px ;
3 entrées = 1 ligne (max 203 px). Variante 4 entrées appliquée : « Calculateur · Guides · Application ·
À propos » (EN « Calculator · Guides · App · About », ES « Calculadora · Guías · App · Acerca de », DE
« Rechner · Ratgeber · App · Über uns », PT « Calculadora · Guias · App · Sobre »), attributs `class="on"`
conservés, Confidentialité et Contact sortis du menu (présents au pied de chaque page), `aria-label`
traduits (« Navigation principale » / « Langue » et équivalents). Constat annexe : à 360 px en ES et
PT, la ligne marque + sélecteur de langue passe elle-même sur deux rangées (en-tête 132 px au lieu de
106 ; 107 au lieu de 81 après l'étape 7) : « Calculadora de RTP » en insécables + 5 boutons de 30 px
dépassent 320 px. À traiter à l'étape 8 (CSS).

**Étape 8 faite le 25/10/2026 (local)**, `assets/site.css` seul (`scratchpad/apply-step8.mjs`, 274 lignes) :
règles `.nav-dd` retirées (balisage disparu à l'étape 3), boutons de langue 30 → 28 px sous 560 px
(ES / PT : première ligne 314 px à 360 px, en-tête 81 px dans les 5 langues, mesuré), centrage des
zones vitrine : accueil sections `#apps` et `#guides` (kicker, H2, lede, « Tous les guides », lede et
note en marges auto), héros empilé ≤ 820 px (texte, lede, boutons, carte démo et légende), index des
guides (H1, `.dek`), tête du calculateur (`.page-head`, `.sub`). Articles, À propos, confidentialité
inchangés (0 px de différence sur l'article à 768 et 1280). Planches : `tools/shots/2026-10-25-1531-compare`
(app, guides, article, ES) et `…-1534-compare` (accueil FR, « avant » obtenu par `git stash` de
`site.css`). Piège rencontré : `node shots.mjs --pages=/fr/` sous Git Bash transforme `/fr/` en
`C:/Program Files/Git/fr/` ; exporter `MSYS_NO_PATHCONV=1` (déjà noté le 24/10 pour `/`).

**Étape 9 faite le 25/10/2026 (local)** : `.htaccess` (**validé explicitement par Julien**, première
règle de page du fichier) : 5 `RewriteRule` (une par langue, deux anciens slugs chacune, `/?(index\.html)?$`)
vers l'article TRJ de la langue, cibles absolues `https://`, insérées entre le bloc HTTPS/www et la
règle `index.html` (`scratchpad/apply-step9.mjs`). `sitemap.xml` régénéré : 45 URL, 315 hreflang,
lastmod 2026-10-25. axe : 0 violation sur les 8 pages de référence × 2 largeurs
(`tools/shots/2026-10-25-1536-a11y`).

**Étape 10 : déployé le 25/10/2026** à la demande de Julien : `push --apply` (47 fichiers : 45 pages,
`site.css`, `sitemap.xml`, tous vérifiés en HTTPS) puis `push --apply --htaccess` (1 fichier). Contrôles
en production : les 10 anciennes URL répondent 301 vers l'article TRJ de leur langue (cible absolue
`https://`), les cibles 200, menu à 4 entrées en ligne, sitemap 45 URL sans ancienne URL, H1 fusionné
visible. `http://` sur une ancienne URL = 2 sauts (https même chemin, puis 301 de page) : attendu.
`diff` à blanc ensuite : « Rien à déployer » ; le manifeste garde les 10 anciennes pages comme
« absentes en local » tant que Julien ne les a pas supprimées dans le Manager Infomaniak (`deploy/`
ne supprime jamais à distance). Quirk de `deploy/` : après un envoi `--htaccess`, le `diff` sans ce
drapeau liste `.htaccess` parmi les « absents en local » (il n'est pas lu sans le drapeau) : sans gravité.
**Testé OK sur S9 et S23+ par Julien le 25/10/2026.** Reste à faire par Julien : demandes
d'indexation Search Console (5 articles TRJ, `/fr/`, `/fr/guides/`, `/fr/app/`, lexique FR, `/`, puis
les 2 anciennes URL FR pour accélérer la prise en compte des 301), resoumission du sitemap,
suppression distante facultative. **Commit `77103c7`** (60 fichiers, à la demande de Julien), poussé le 25/10. **Sitemap resoumis le
25/10/2026 ; demandes d'indexation faites, les 10 URL étaient déjà connues de Google** (le recrawl est
tout de même mis en file). `CLAUDE.md` mis à jour le 25/10 (§ 6 structure éditoriale et centrage, § 8 captures
et `MSYS_NO_PATHCONV`, § 9 et § 10 condensés pour rester à 200 lignes), validé par Julien. Bilan GSC 4 à 6 semaines
après (fin novembre 2026) : anciennes URL sorties de l'index, clics FR, position de « marge bookmaker »
et « calcul trj ».

---

## Partage en image depuis le calculateur web (24/10/2026)

Constat de Julien (Firefox PC, Chrome S23) : « Partager » envoie du texte. **Pas une régression** :
`shareA()` des 5 `*/app/index.html` n'a jamais produit qu'une ligne de texte (`navigator.share({text})`,
sinon copie), identique depuis le premier commit du site (`6a11dd7`). Le site promettait pourtant l'image
à 6 endroits (intro des 5 calculateurs, accueils FR/DE/PT) ; seule l'app Android le fait
(`ShareableCard.kt` / `ShareImage.kt`, 1080 px).

- **Choix** : rendu Canvas 2D maison dans `assets/share.js` (partagé par les 5 pages, comme `site.css` /
  `cmp.js`), carte à 360 px dessinée à l'échelle 3 (1080 px), même structure que la carte d'historique et
  que l'image de l'app (barre TRJ, méta + pastille, événement, blocs par 3, filet, logo + « RTP Calculator ·
  rtpcalculator.com »). Écarté : html2canvas (dépendance, rendu infidèle des polices) ; capture DOM native
  (inexistante). Polices Archivo demandées par `document.fonts.load()` avant le dessin (sinon police de repli).
- **Canaux, dans l'ordre** : `navigator.share({files})` si `canShare` l'accepte (Chrome Android, Chrome/Edge
  Windows, Safari) → `clipboard.write` image/png (Firefox ≥ 127, Chrome bureau) → téléchargement `<a download>`.
  L'ancien partage texte reste en `shareText()`, utilisé seulement si `share.js` n'est pas chargé.
- La page construit un modèle de vue déjà localisé (`meta`, `eventName`, `rtp{text,color}`, `outcomes`) :
  `share.js` ne connaît ni les langues ni les formats de cotes. Chaînes ajoutées à `T` : `copiedImg`,
  `downloaded`, `shareFail`, `shareFile` (préfixe du fichier : `trj` en FR, `rtp` ailleurs).
- **État (24/10/2026 au soir)** : rendu et chaînes FR validés par Julien ; `assets/share.js` + les 5 pages patchés
  (script `patch-share.mjs` du scratchpad, 3 ancrages à occurrence unique par page ; entre langues, seules les
  chaînes et le drapeau décimal diffèrent). Rendu vérifié en Chromium headless (FR et DE, 5 cas chacun).
  `sitemap.xml` inchangé (lastmod déjà au 24/10). **Déployé le 24/10/2026 au soir** à la demande de Julien (6 fichiers vérifiés en HTTPS, diff à blanc vide ensuite,
  `manifest.json` à jour). Commit `7ab11e3`, poussé. **Testé OK par Julien le 24/10/2026** : Firefox PC (presse-papiers) et Chrome S23
  (feuille de partage). S9 non testé, non bloquant (canal download en dernier recours).

---
## Relecture des textes du site par Julien (24/10/2026)

Demande de Julien : relire lui-même **tous** les textes du site dans un Word, puis me le rendre
pour mise en ligne. Outil créé dans `tools/copy/` (non commité) :

- `npm --prefix tools run copy:extract` → `tools/copy/out/site-textes-<date>.docx` +
  `snapshot.json` (référence, **à conserver jusqu'au retour du Word** ; jeton commun aux deux,
  vérifié par `apply`). Extraction du 24/10/2026 : **1 723 textes distincts, 3 045 emplacements**,
  55 pages + bandeau cookies (`assets/cmp.js`) + chaînes JS du calculateur (`var T` des 5 `app/`).
  Ordre : FR, EN, ES, DE, PT ; par page : `<title>`, metas, og, JSON-LD `headline`, feuilles HTML
  (h1…td, boutons, liens de menu, attributs `aria-label` / `placeholder`).
- `npm --prefix tools run copy:apply -- <docx> [--apply] [--empty-ok]` : relecture à blanc par
  défaut, tout ou rien (garde : jeton, et chaque emplacement relu dans la source doit être
  identique à l'extraction, sinon réextraire). Accepte aussi un `.docx` décompressé (tests).
- Conception : analyseur HTML maison à offsets conservés (`lib.mjs`), aucune réécriture hors des
  textes modifiés. **Dédoublonnage par langue** : un même texte (menus, pieds, boutons, titre
  repris en `<title>` / `og:title` / H1 / `headline`) = une seule ligne Word (« ×N »), modification
  propagée à tous ses emplacements. Mise en forme : gras / italique / liens = runs Word
  (les `href` d'origine sont toujours conservés) ; autres balises inline (`span.hl`, `dd-car`) =
  run sur fond gris `E6E6E6`, remis dans leur balise d'origine par ordre. Apostrophes courbes et
  guillemets anglais de Word redressés, espace insécable Word devant `: ; ? !` retirée (signalés
  dans le compte rendu), cellule vidée = non appliquée sans `--empty-ok`.
- Testé : aller-retour sans modification = 0 changement ; 9 modifications simulées (menu ×11
  pages, gras stylé, `span.hl`, texte de lien, chaîne JS avec guillemets, headline JSON-LD,
  apostrophes courbes = inchangé) réinjectées correctement, puis `git checkout`.
- Écarté : Playwright/Chromium pour l'analyse (DOM normalisé, impossible de réécrire la source à
  l'identique) ; `cheerio`/`parse5` (même problème) ; tableau multilingue côte à côte (illisible).
- Dépendance ajoutée : `docx` 9.5.1 dans `tools/`. Pas de LibreOffice ni pandoc sur la machine :
  rendu Word non vérifié ici, Julien ouvrira le fichier.
- `CLAUDE.md` mis à jour le 24/10/2026 (validé par Julien) : § 1 `tools/`, § 8 commandes `copy:*`.

**Retour du Word (24/10/2026, soir) : fait, commité `2d62671`, sitemap inchangé (lastmod déjà au 24/10), déployé le 24/10 au soir (57 fichiers, tous vérifiés en HTTPS, `diff` ensuite vide).** Plan validé
(`~/.claude/plans/proud-kindling-riddle.md`), exécuté en 4 étapes validées une à une :

1. `copy:apply --apply` : 13 textes FR relus par Julien, 39 emplacements, 8 fichiers (garde OK).
   Coquille « dans table de correspondance » corrigée en « dans une table » avec son accord.
2. **Plus aucun tiret long sur le site** (demande de Julien : « trop associés à un texte écrit
   par IA »). Règles tirées de ses propres corrections : titres de pages « A - B » ; incises par
   virgules, deux-points ou phrase coupée ; pied de page « © 2026 RTP · SIREN » ; intitulés de la
   confidentialité avec deux-points ; valeurs vides du calculateur (`TRJ –`, `|| '–'`) en
   demi-cadratin, texte de partage `join(' · ')`. FR : 64 remplacements, 12 fichiers.
3. Guide « Comment calculer la marge d'un bookmaker » → **« Comment calculer le TRJ d'un pari
   sportif et la marge des bookmakers ? »** (titre de Julien, version du Word). Réécrit (angle
   méthode, la marge comme complément du TRJ ; le guide `trj-pari-sportif` garde l'angle
   définition). **URL conservée** (`calculer-marge-bookmaker` et équivalents) : un renommage
   imposerait des 301 dans le `.htaccess` (zone interdite), 25 hreflang et une perte de
   positionnement ; décidé avec Julien. `dateModified` 2026-10-24, date affichée « Mis à jour ».
   Titre du comparatif : « … plutôt que par les promotions qu'ils proposent » (Julien).
4. Propagation EN/ES/DE/PT par scripts à comptage exact (103 / 102 / 125 / 102 remplacements,
   11 fichiers par langue) : équivalents des 13 corrections, tirets, guide traduit, cartes.
   DE comptait ~20 tirets propres à sa traduction, traités de même ; son `<title>` du guide est
   aligné sur le H1 comme ailleurs. Titres sans « ? » en EN/DE, « ¿ ? » en ES, « ? » en PT.

Contrôles : 57 fichiers modifiés (+588/−531), 0 « — », `&mdash;` ou « – » hors JS ; balises
équilibrées sur les 5 guides (19 p, 7 h2, 14 strong, 27 a, 8 div chacun) ; réextraction de
contrôle : 55/60 pages au même nombre d'emplacements, les 5 guides passent de 43 à 52 ;
captures locales (`tools/shots/2026-10-24-2105-relecture`, `…-2107-relecture-fr`) relues.
Scripts de remplacement en scratchpad (non versionnés). **`snapshot.json` du 24/10 est périmé** :
réextraire avant toute nouvelle relecture Word. Chaîne commit → sitemap → déploiement faite le 24/10 ;
`git push` à faire par Julien.
Règle typographique « aucun tiret long » ajoutée à `CLAUDE.md` § 6 le 24/10/2026 (validée par Julien).

---

## Présentation du site : audit et chantier (24/10/2026)

Rapport : `stats/reports/2026-10-24-audit-presentation-site.md` (constats mesurés, décisions,
lots). À retenir :

- **Défauts confirmés par mesure** (copie locale = production) : gouttière du héros perdue
  (`.hero{padding:72px 0 84px}` écrase `.wrap`, H1 à x = 0 sur mobile, 24 px à gauche des H2 sur
  desktop) ; section confidentialité de l'accueil centrée (H2 à x = 315 contre 134) ; défilement
  horizontal de l'accueil à 375/768 px (+ 24 px, cause probable `.demo-wrap::before`) et du guide
  « formats de cotes » (tableau, 422 px) ; menu mobile avec 3 entrées hors écran sans indice ;
  en-tête mobile sur 3 rangées (112–123 px) ; boutons longs sur 2 lignes alignés à gauche ;
  axe : 44 contrastes en défaut, tous dus à `--text-3:#666` (3,44:1), 18 liens repérables par la
  couleur seule, graisse 400 non chargée. Infirmé : troncature des placeholders date/heure à 375 px.
- **Décisions de Julien** : feuille de style partagée `assets/site.css` + `assets/cmp.js`
  (écarté : inline propagé par script) ; menu mobile sur deux lignes (écarté : hamburger) ;
  boutons en casse de phrase, graisse 600.
- **Lot 1a fait le 24/10/2026, local, non déployé, non commité** : `assets/site.css` (32 Ko,
  11 règles communes puis 5 sections préfixées `body.p-home|p-app|p-guides|p-article|p-privacy`,
  `:root` → `body.p-x`, `html` → `html:has(body.p-x)`) et `assets/cmp.js` (variante PT du script,
  qui contient toutes les langues) ; les 55 pages reçoivent `<link rel="stylesheet">`,
  `<body class="p-x">` et `<script src="/assets/cmp.js">` (pas sur `/privacy/`, sans GA4). HTML
  total 1 439 → 670 Ko. Script d'extraction : scratchpad (refus si un bloc n'est pas trouvé
  exactement une fois ; 0 erreur). **Preuve** : 162 captures avant/après identiques au pixel
  près sur 54 pages × 3 largeurs ; `/` (racine EN, non capturée « avant » : Git Bash transforme
  `/` en chemin Windows, utiliser `MSYS_NO_PATHCONV=1`) identique à la production ; texte des
  pages identique hors blocs extraits. Deux écarts de 0,07–0,09 % (`/privacy/`, `/de/privacy/`
  à 768 px) étaient du bruit de rendu d'une capture isolée : recaptures et production identiques.
  `npm --prefix deploy run diff` : 57 fichiers à envoyer (55 HTML + 2 assets), 9 inchangés.
  Après déploiement, relancer `node tools/sitemap.mjs` (lastmod des 55 pages).
- **Lot 1b fait le 24/10/2026, local, non déployé, non commité.** `site.css` réécrit (21 Ko,
  267 lignes) : jetons (`--gutter` 24/20 px, `--max-wide` 1060, `--max-read` 720, `--max-app` 760,
  `--text-3:#8A8A8A`, `--border-strong:#6A6A6A`), base, en-tête collant partout, **un seul système
  de boutons** (`.btn`, `.play-btn`, `.btn-save`, `.cta a` : 44 px mini, 15 px casse de phrase
  graisse 600, centrés, pleine largeur ≤ 560 px), pied unique ; puis sections par type (guides,
  index des guides et à-propos fusionnés). Corrections : graisse 400 chargée (lien Google Fonts
  des 55 pages), liens soulignés dans le texte, aucune taille < 11 px, halo du héros limité
  (`inset:-48px 0`) → plus de défilement horizontal de l'accueil, bouton « supprimer » désactivé
  en `rgba(255,255,255,.3)` (était `#4DFFFFFF`, ARGB Android → **cyan**), avertissement 18+ ajouté
  aux pieds du calculateur et de la confidentialité (10 pages, texte copié de l'accueil de la
  langue), boutons du bandeau cookies alignés (`cmp.js`). **axe : 0 violation** de contraste et
  de lien (44 + 18 avant) ; reste `heading-order` sur `/privacy/` (lot 4).
  **Régression attrapée par les mesures** : `main{padding:36px 0 10px}` (guides, à-propos,
  confidentialité) l'emportait, une fois préfixé, sur la gouttière de `.wrap` porté par le même
  `<main>` → contenu collé au bord. Corrigé en padding vertical seul. **Piège durable** (même
  mécanisme que le héros, A0) : ne jamais écrire `padding:X 0 Y` sur un élément qui porte aussi
  `.wrap` ; candidat `CLAUDE.md` § 6. Planches 1a → 1b : `tools/shots/2026-10-24-1350-compare/`
  (différences attendues : boutons, graisse, pieds, couleurs). Scripts du lot : scratchpad
  (`build-1b.mjs` reconstruit `site.css` depuis la version 1a avec garde-fous ; `apply-1b.mjs`
  polices + pieds + cmp.js).
- **Lot 2 fait le 24/10/2026, local, non déployé, non commité** (CSS seul, section `.p-home`
  de `site.css`) : gouttière du héros rétablie (`padding-top/bottom` au lieu de `padding:72px 0
  84px`, desktop et mobile), section confidentialité ramenée sur la grille des autres sections
  (`.legal .max{max-width:var(--max)}`, lecture limitée par `.legal p{max-width:76ch}`), légende
  de la carte démo alignée à gauche sous 820 px. Mesure : H1 et les 3 H2 à la même abscisse aux
  3 largeurs (20 / 24 / 134 px), sur `/`, `/fr/`, `/de/`. Les boutons héros empilés pleine
  largeur venaient déjà du lot 1b. Kickers et H2 raccourcis = copie → lot 6.
  Planches 1b → 2 : `tools/shots/2026-10-24-1354-compare/`.
- **Lot 3 fait le 24/10/2026, local, non déployé, non commité** (bloc mobile de l'en-tête dans
  `site.css`) : menu sur deux lignes à retour automatique (plus de défilement masqué, 0 entrée
  hors écran sur FR/DE/EN à 360 et 375 px), logo compacté (13 px, sans interlettrage) et
  sélecteur de langue 30×30 px pour tenir sur la ligne du logo à 360 px. **Décision** : en-tête
  **non collant sous 820 px** (il mesure 106–108 px, soit 14 % d'un écran de 740 px ; collant,
  il aurait mangé l'écran à chaque défilement). Réversible en retirant
  `header.site{position:static}` du bloc mobile. Hauteur mesurée : 106–108 px à 360/375,
  77 px à 768 (avant : 112–123 px avec 3 entrées cachées).
- **Lot 5 fait le 24/10/2026, local, non déployé, non commité** : les 10 tableaux (2 guides × 5
  langues : formats de cotes, paris combinés) enveloppés d'un `<div class="tbl-wrap">` à
  `overflow-x:auto` avec ombres de bord (dégradés `background-attachment: local/scroll`),
  cellules en `nowrap`. Mesure : plus aucun défilement horizontal de page à 360/375 px sur les
  4 pages testées (FR ×2, DE, EN) ; c'était le dernier du site.
- **Lot 4 fait le 24/10/2026, local, non déployé, non commité** : calculateur — actions
  d'historique et bouton de tri à 44 px, segments EU/UK/US et bouton supprimer à la hauteur du
  champ (38 px, supprimer élargi à 44 px), « Ajouter une issue » à 38 px avec bordure visible,
  H2 « Historique » et H2 du texte SEO à 19 px, placeholders date/heure avec points de suspension
  sous 340 px. Confidentialité (5 pages) : les 8 sections numérotées de la politique passent de
  `<h3>` à `<h2 class="sec">` au rendu identique (ordre des titres h1 → h2 → h3 ; les 3
  sous-sections de « Suppression des données » restent en h3). Au passage, les 10 conteneurs de
  tableau du lot 5 reçoivent `tabindex="0"` (axe `scrollable-region-focusable`). **axe : 0
  violation** sur les 8 pages de référence × 2 largeurs.
- **Lot 6 fait le 24/10/2026, local, non déployé, non commité.** Copie FR validée par Julien
  (24/10) puis propagée EN/ES/DE/PT-BR par script à comptage exact (95 remplacements) :
  boutons « Ouvrir le calculateur » (accueil + 35 pages guides) et « Télécharger l'application »
  (accueil, 30 articles, 5 calculateurs ; les pieds de page ES/PT « Descargar la versión Android »
  / « Baixar a versão Android » sont des liens de pied, pas des boutons : inchangés) ; accueil :
  kicker « Application » + H2 « Sur navigateur ou sur Android », lede « Versions navigateur et
  Android gratuites &amp; sans inscription. », kicker « Guides » + H2 « Comprendre le TRJ et les
  cotes ». **Julien a refusé** de toucher la lede de la section guides : conservée. Traductions
  des H2 : « In your browser or on Android » / « Understanding RTP and odds », « En el navegador
  o en Android » / « Entender el RTP y las cuotas », « Im Browser oder auf Android » / « RTP und
  Quoten verstehen », « No navegador ou no Android » / « Entender o RTP e as odds ».
  Réserve notée : les deux H2 d'accueil avaient un léger poids SEO ; à surveiller au prochain
  bilan Search Console.
- **Chantier présentation terminé et déployé le 24/10/2026** (lots 1a → 6), à la demande de
  Julien : commits `48765d1` (outillage + audit) et `da63d6c` (site), puis `push --apply` :
  57 fichiers (55 HTML + 2 assets) envoyés et vérifiés en HTTPS, diff à blanc vide ensuite,
  `manifest.json` à jour (66 fichiers en production). Sitemap inchangé (lastmod déjà au 24/10).
  Captures de production comparées à la copie locale après envoi : 8 sur 8 identiques au pixel
  près (`/`, `/fr/`, `/fr/app/`, `/fr/guides/formats-de-cotes/` à 375 et 1280 px).
  **Testé OK sur S9 et S23+ par Julien le 25/10/2026.** Reste : `CLAUDE.md` § 1, § 6, § 8 (proposé, en attente) ;
  surveiller au prochain bilan Search Console l'effet des deux H2 d'accueil modifiés.
  (corrections transversales) → 2 accueil → 3 menu → 5 guides → 4 calculateur → 6 copie (règle 3).
  Chaque lot : diff + planche avant/après + validation, déploiement sur demande.
- **Outillage `tools/`** (Playwright, axe-core, serveur local, comparaison d'images) : détail § 4
  du rapport. Chromium confiné dans `tools/.browsers/` (719 Mo, ignoré par Git). Les captures
  bloquent GA4/AdSense et pré-refusent le consentement : aucune session comptée en production.
  Référence avant travaux : `tools/shots/2026-10-24-1224-local/`, `…-1225-prod/`, `…-1226-a11y/`.
  Commandes : `npm --prefix tools run shots [-- --url=https://rtpcalculator.com --pages=… --widths=… --label=…]`,
  `npm --prefix tools run shots -- --compare <A> <B>`, `npm --prefix tools run a11y`.
  **`CLAUDE.md` à mettre à jour** (proposé à Julien, en attente) : § 1 `tools/` (sitemap + captures +
  axe), § 8 commandes `tools/`.

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
