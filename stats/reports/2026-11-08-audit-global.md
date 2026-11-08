# Audit global et perspectives : 08/11/2026

Relevé `stats/data/2026-11-08/` : AdMob, AdSense, GA4, Search Console et Play du 01/01 au 07/11/2026
(`--all`), plus `inspect` (49 URL) et `listing` (fiche Play en ligne). Bilan convenu le 25/10 pour mesurer
les changements d'octobre : lot CTR et sitemap (24/10), fiche Play et refonte du site à 45 pages (25/10),
app 2.2.0 (29/10), thème clair et mode sombre (05/11). Rapport précédent : `2026-10-23-analyse-globale.md`.

## 1. Sources et limites

| Source | Période utile | Retard | Réserve |
|---|---|---|---|
| Play (CSV du bucket) | 10/05 → 03/11 | 4 jours | Source de trafic, pays de la fiche et UTM agrégés en « Other » sous le seuil de Google |
| AdMob | 06/05 → 07/11 | 1 jour | Une bannière ; les requêtes servent de proxy d'ouverture de l'app |
| AdSense | état du compte | aucun | Aucune donnée de rapport : site jamais activé |
| Search Console | 02/06 → 07/11 | 1 à 2 jours | Requêtes rares anonymisées : les totaux viennent des séries par jour ou par page |
| GA4 | 06/2026 → 07/11 | 1 jour | Minoré par le Consent Mode ; juin et une partie de l'été sont des robots |
| URL Inspection | 08/11 | aucun | 45 URL du sitemap + 4 variantes ; comparé au relevé du 24/10 |

La fenêtre « après » va du 25/10 au 07/11 : 14 jours, dont une anomalie le 27/10 (section 4). Tous les
volumes sont faibles : les écarts sont des ordres de grandeur, pas des tendances établies.

## 2. App Android (Play Console)

### 2.1 Séries mensuelles

| Mois | Installations | Désinstallations | Actifs en fin de mois | Visiteurs fiche | Acquisitions | Conversion |
|---|---|---|---|---|---|---|
| Mai | 16 | 14 | 3 | 68 | 15 | 22 % |
| Juin | 14 | 13 | 4 | 77 | 13 | 17 % |
| Juillet | 9 | 11 | 1 | 38 | 8 | 21 % |
| Août | 21 | 17 | 8 | 49 | 22 | 45 % |
| Septembre | 22 | 22 | 7 | 71 | 17 | 24 % |
| Octobre | 18 | 16 | 10 | 86 | 17 | 20 % |
| Novembre (1 au 3) | 6 | 2 | **15** | 6 | 3 | |

Cumul : 106 installations, 95 désinstallations, 15 appareils actifs le 03/11 (7 le 22/10, plafond
précédent 12 le 08/09). Note 5,0 ; vitals toujours vides (sous le seuil) ; 0 problème signalé.

### 2.2 Avant / après la nouvelle fiche (publiée le 25/10)

| Indicateur | 30 jours avant (26/09 → 25/10) | Après (26/10 → 03/11, 9 jours) |
|---|---|---|
| Installations | 17 | 9 |
| Désinstallations | 15 (88 %) | 4 (44 %) |
| Dont Myanmar | 7 installs / 5 désinstalls | **0 install** / 1 désinstall |
| Pays des installs | MM 7, MX 2, puis BD, BR, PT, US, PY, IN, BE, ZA | BR 3, GR 2, BW, VN, PH, TW |
| Langues système | | 3 non renseignées, en_US 2, pt_BR 2, vi_VN 1, zh_TW 1 |
| Appareils actifs | 11 → 11 | 9 → 15 |
| Visiteurs fiche / acquisitions | 46 / 7 (15 %) | 23 / 6 (26 %) |

Le Myanmar, qui pesait un tiers des installations depuis mai (33 sur 106), n'en apporte plus aucune depuis
la fiche FR / PT-BR centrée « paris sportifs ». C'est le résultat attendu, obtenu sur 9 jours : à confirmer
sur 30. Le Brésil continue (3 installs, 0 désinstall, 2 en pt_BR).

Nuance au diagnostic du 23/10 : l'unique utilisateur qui note l'app est au Myanmar (5 étoiles le 04/09 en
2.1.0, avis renouvelé le 05/11 en 2.2.0, appareil « ginkgo »), et les 16 requêtes AdMob du 07/11 viennent du
Myanmar. Il existe donc au moins un utilisateur réel et fidèle là-bas ; la confusion « RTP casino » explique
les désinstallations à J+1, pas la totalité du pays.

### 2.3 Adoption de la 2.2.0

Code de version en production : **12** (CSV Play et `build.gradle.kts`), et non 11 comme l'écrivent
`CLAUDE.md` § 4 et le journal (le commit `e278023` passe de 10 à 12). Le 03/11 : 11 actifs en code 12,
4 restés en code 10. 8 installations directes en 2.2.0 depuis le 29/10.

### 2.4 Fiche Play

Lecture API du 08/11 : les 5 langues sont identiques aux fichiers `play/listings/` (titres, descriptions
courtes et longues), puce « Détection des surebets » publiée dans les 5 langues. Source de trafic, terme de
recherche, UTM : toujours « Other » à 100 % (395 visiteurs) ; le recoupement avec les UTM du site reste
impossible à ce volume.

## 3. Monétisation

### 3.1 AdMob

| Indicateur | Cumul (06/05 → 07/11) | Octobre | Novembre (7 jours) |
|---|---|---|---|
| Revenus | 1,62 € | 0,09 € | 0,23 € |
| Requêtes | 1 164 | 74 | 75 |
| Impressions | 667 | 55 | 55 |
| eCPM | 2,43 € | | |

Pays des revenus : Myanmar 1,04 €, Brésil 0,34 €, France 0,06 €. Les requêtes, proxy d'ouverture de l'app,
passent de 2,6 par jour (1 au 25/10) à 6,4 par jour (26/10 → 07/11) : cohérent avec 15 actifs au lieu de
9. Pas un levier financier : rien ne change l'ordre de grandeur.

### 3.2 AdSense

Compte `READY` (créé le 29/04), site `rtpcalculator.com` en `NEEDS_ATTENTION`, client en `GETTING_READY`,
0 bloc, 0 ligne de rapport, alerte « onboarding incomplet : informations de paiement et connexion du site ».
Inchangé depuis le 29/10. Le script chargé sur les 5 calculateurs reste inerte.

## 4. Site

### 4.1 Search Console, vue mensuelle

| Mois | Impressions | Clics | CTR | Position |
|---|---|---|---|---|
| Juin | 20 | 0 | | 13,5 |
| Juillet | 102 | 0 | | 26,0 |
| Août | 204 | 6 | 2,9 % | 12,6 |
| Septembre | 184 | 3 | 1,6 % | 12,5 |
| Octobre | 962 | 18 | 1,9 % | 13,4 |
| Novembre (7 jours) | 395 | 12 | 3,0 % | 8,6 |

Par semaine (lundi) : 26 à 62 impressions par semaine de septembre à mi-octobre, puis **298 la semaine du
19/10, 624 celle du 26/10 (dont l'anomalie du 27/10), 344 celle du 02/11**. La hausse commence le 19/10,
**cinq jours avant le lot CTR** : elle suit le recrawl massif du 18/10 (quinze pages ont ce jour comme
dernier crawl au 24/10) et ne peut pas être attribuée aux changements d'octobre. Ce qui leur est
attribuable se lit dans le CTR et les positions, ci-dessous.

**Anomalie du 27/10** : 276 impressions, dont 209 sur le lexique FR en position 24, depuis des ordinateurs
aux États-Unis (82), au Brésil (27), au Mexique (14), au Canada (13), en Chine (9) : des requêtes
anonymisées, non francophones, sur une page française. Profil d'un outil automatisé qui interroge Google
(suivi de positions ou agent IA), pas de lecteurs. Le lexique revient à 6 à 12 impressions par jour dès le
28/10. Le pic du 20 au 22/10 noté le 23/10 est différent : 143 impressions réparties sur 50 pays, surtout
francophones, en positions 6 à 10 ; c'est le début de la hausse générale, pas une anomalie.

### 4.2 Avant / après le 24/10 (lot CTR) et le 25/10 (refonte)

Fenêtres : 28 jours avant (26/09 → 23/10) et 14 jours après (25/10 → 07/11). Le 27/10 retiré de la fenêtre
« après » donne 18 clics / 723 impressions, CTR 2,5 %.

| Page | Avant : clics / impr. / pos. | Après : clics / impr. / pos. | Lecture |
|---|---|---|---|
| `/` (accueil EN) | 1 / 65 / 7,3 (CTR 1,5 %) | **11 / 151 / 5,6 (CTR 7,3 %)** | Effet net du lot CTR : titre et extrait EN convertissent |
| `/fr/` (https) | 0 / 24 / 9,0 | 4 / 101 / 8,4 (CTR 4,0 %) | Positif ; la variante `http://…/fr/` (60 impr. avant) a disparu depuis le 21/10 |
| `/fr/app/` | 0 / 0 | 0 / 53 / 9,2 | Indexée le 24/10 : nouvelle présence, 0 clic |
| `/app/` | 0 / 0 | 1 / 33 / 6,8 | Indexée le 24/10 |
| Lexique FR | 1 / 56 / 8,3 | 0 / 336 / 18,6 (0 / 127 / 12 hors 27/10) | Impressions sans clic, requêtes anonymisées |
| Article TRJ fusionné FR | 0 / 17 / 9,0 | 1 / 53 / 9,5 | Pas recrawlé depuis le 18/10 : Google voit l'ancienne page |
| Formats de cotes FR | 0 / 14 / 8,4 | 0 / 43 / 15,0 | Pas recrawlé depuis le 18/10 |
| `/guides/rtp-sports-betting/` (EN, **supprimée le 25/10**) | 1 / 8 / 6,3 | 1 / 40 / 6,5 | Toujours servie par Google |

| Requête | Avant | Après | Lecture |
|---|---|---|---|
| rtp calculator | 0 / 4 / pos. 7,0 | **4 / 55 / pos. 5,1** | Le terme EN de la marque monte en page 1 haute et convertit |
| calcul trj | 0 / 32 / 9,7 | 1 / 29 / 8,3 | Stable en volume, position gagnée, CTR toujours faible |
| calculateur trj | 1 / 12 / 8,4 | 0 / 5 / 5,2 | Volume en baisse, position meilleure |
| marge bookmaker (BE surtout) | 0 / 7 / 10,9 | 0 / 21 / 11,7 | En hausse, 0 clic, cible de l'article fusionné non recrawlé |
| calculo rtp (PT / BR) | 0 / 6 / 15,7 | 0 / 14 / 5,5 | Position 5 et 0 clic : intention casino confirmée, ne pas chasser |
| rtp sport gokken (BE) | 0 / 0 | 0 / 15 / 8,5 | Néerlandais, nouveau, atterrit sur `/` |
| cote fractionnaire (BE) | 0 / 3 / 13,3 | 0 / 15 / 13,7 | Guide formats de cotes FR |

Pays depuis le 25/10 : France 167 impressions (5 clics), États-Unis 150 (1), Belgique 88 (0), Brésil 85 (2),
Allemagne 67 (1), Canada 39 (2), Royaume-Uni 35 (1), Portugal 35 (0). Mobile 435 impressions, CTR 2,8 %,
position 8,3 ; desktop 560, CTR 2,0 %, position 16,0 (le desktop porte l'anomalie du 27/10).

**Canonicalisation** : réglée d'elle-même. `http://rtpcalculator.com/fr/` vue pour la dernière fois le
21/10, `http://…/es/` le 17/10, `https://www.…/` le 29/08, `http://…/` le 30/06. Les 4 variantes sont
« Page avec redirection » à l'inspection, canonical Google = `https://`. Rien à faire.

**Anciennes URL (301 du 25/10)** : Google les sert encore. `/fr/guides/comparer-bookmakers-trj/` a reçu
1 clic le 06/11, `/guides/rtp-sports-betting/` 40 impressions depuis le 25/10, les variantes DE et PT
« comparer » jusqu'au 02 et 03/11. Normal à 14 jours, mais le transfert vers l'article fusionné n'a pas
commencé : l'article lui-même n'a pas été recrawlé (4.3).

### 4.3 Indexation (URL Inspection, 24/10 → 08/11)

| État | 24/10 (55 pages) | 08/11 (45 pages) |
|---|---|---|
| Envoyée et indexée | 45 | 37 |
| Explorée, non indexée | 3 | 3 (`/fr/privacy/`, `/es/privacy/`, **`/es/guides/lexico-apuestas-deportivas/`**, nouvelle) |
| Détectée, non indexée | 1 | 1 (`/pt/sobre/`) |
| Inconnue de Google | 3 | 4 (`/pt/app/`, `/pt/guides/apostas-multiplas-rtp/`, `/pt/guides/lexico-apostas-esportivas/`, `/de/privacy/`) |

Gains : `/app/`, `/fr/app/` et `/es/` sont indexés (demandes du 24/10, crawl le jour même). Perte : le lexique
ES est sorti de l'index. Sitemap : 45 URL, téléchargé le 04/11, 0 erreur.

Crawl : 25 pages sur 45 revues depuis le 24/10, 18 depuis la refonte du 25/10. **Non recrawlées depuis le
18/10** : l'article TRJ fusionné FR, `formats-de-cotes` FR, `/de/app/`, `/privacy/`, lexique DE, à-propos DE
et EN (22/08). Les textes de la refonte et les dates « mis à jour » ne sont donc pas encore pris en compte
sur les pages FR les plus importantes après l'accueil. Les 3 pages PT jamais vues le sont toujours : le
maillage interne vers `/pt/app/` existe pourtant (menu). Demandes d'indexation à faire par Julien (section 8).

### 4.4 GA4

| Mois | Sessions | Dont organiques | Dont directes | Lecture |
|---|---|---|---|---|
| Juin | 164 | 4 | 158 | Robots (133 « US desktop » à 0 s) |
| Juillet | 11 | 0 | 11 | |
| Août | 7 | 2 | 5 | |
| Septembre | 2 | 1 | 1 | |
| Octobre | 15 | 7 | 6 | + 2 via ChatGPT (`/fr/`, 16 et 28/10) |
| Novembre (7 jours) | 9 | **9** | 0 | Le trafic direct de robots a cessé |

Depuis le 25/10 : 15 sessions humaines (France 7, Guinée 3, Brésil 2, Allemagne, Croatie, Indonésie, NZ,
Algérie), durées de 6 à 71 s, 9 sur mobile. GA4 compte 9 sessions organiques en novembre pour 12 clics
Search Console : la perte due au Consent Mode est plus faible qu'au printemps.

Clics sortants vers Play : 5 en tout, dont **3 depuis les liens UTM du 24/10** (Croatie depuis `/` le
26/10, France depuis `/app/` le 28/10, Brésil depuis `/pt/` le 03/11). Le marquage fonctionne ; Play
l'agrège en « Other ». Événements : `page_view`, `scroll`, `click` seulement. **Aucun événement propre au
calculateur** (calcul, ajout d'issue, partage) : l'usage du calculateur web n'est pas mesuré.

Navigateurs, hors robots : Chrome Windows 113, Chrome Linux 26, Chrome Android 24, **iOS 14** (Chrome 7,
Safari 7), Edge 11, Firefox 13, Mac 6. Un visiteur mobile sur trois est sous iOS, sans app disponible.

Signal faible mais net : une requête Search Console du 21/10 est un prompt d'agent IA mot pour mot
(« context: location: portugal … question: como avaliar a margem (overround) de uma casa de apostas… »,
position 4), et le canal « AI Assistant » apparaît en octobre. Les assistants interrogent Google et trouvent
les guides : le contenu structuré (définitions, formules) est le bon format.

## 5. Bilan des actions d'octobre

| Action | Date | Indicateur attendu | Observé | Verdict |
|---|---|---|---|---|
| Lot CTR (titres, extraits, boutons héros, UTM, og:image) | 24/10 | CTR de `/` et `/fr/` | `/` : 1,5 % → 7,3 %, « rtp calculator » pos. 7 → 5 avec 4 clics ; `/fr/` : 0 → 4 clics | **Effet visible** sur EN, encourageant sur FR |
| Sitemap généré, demandes d'indexation | 24/10 | Indexation des calculateurs, fin des variantes http / www | `/app/`, `/fr/app/`, `/es/` indexés ; variantes disparues | **Effet visible** ; `/pt/app/` toujours inconnu |
| Fiche Play FR / PT-BR « paris sportifs » | 25/10 | Moins d'installs Myanmar, moins de désinstalls J+1 | Myanmar 7 → 0 installs ; désinstalls 88 % → 44 % ; actifs 9 → 15 | **Effet visible**, 9 jours seulement |
| Refonte du site (45 pages, article fusionné, 301) | 25/10 | Transfert des anciennes URL, position de « marge bookmaker » | Anciennes URL encore servies ; article fusionné non recrawlé ; « marge bookmaker » 7 → 21 impr., 0 clic | **Trop tôt** : Google n'a pas lu la nouvelle version |
| App 2.2.0 (surebet, signature, placeholder) | 29/10 | Adoption, avis | 11 actifs sur 15 en code 12 ; avis 5 étoiles renouvelé en 2.2.0 | **Effet visible** sur l'adoption, rien de mesurable sur l'usage |
| Thème clair, mode sombre, couleur | 05/11 | Aucun indicateur de données | 2 jours de données | **Non mesurable** ; test devices par Julien toujours en attente |
| Guide combinés : H2 « poule aux œufs d'or » | 29/10 | Position du guide | 1 impression FR depuis le 25/10, page non recrawlée (27/10) | **Trop tôt**, volume nul |

## 6. Diagnostic actualisé

Confirmé depuis le 23/10 :
- Le site est l'actif qui progresse : impressions × 8 entre la semaine du 12/10 et celle du 02/11, position
  moyenne 8,6 en novembre. La hausse est antérieure aux changements et vient du recrawl du 18/10 ; les
  changements ont surtout amélioré la conversion de l'accueil EN.
- L'intention casino reste hors de portée : « calculo rtp » en position 5 fait 0 clic sur 14 impressions.
- La monétisation est hors sujet à ce volume ; AdSense n'a jamais été actif.

Infirmé ou nuancé :
- « Le Myanmar, c'est la confusion casino » : vrai pour les désinstallations à J+1, mais l'utilisateur le plus
  fidèle de l'app est là-bas. La fiche a coupé le flux de curieux sans toucher cet usage.
- « Dette SEO http / www » : résolue sans intervention.
- « Rétention nulle » : 15 actifs pour 106 installs (14 %) ; c'était 7 % le 22/10.

Nouveau :
- Google n'a pas relu les pages FR centrales depuis le 18/10 : l'effet de la refonte est en attente d'un
  crawl, pas d'une action de contenu.
- La Belgique est le deuxième marché francophone (113 impressions, 0 clic), avec une requête néerlandaise
  (« rtp sport gokken ») qui monte sur l'accueil EN.
- L'usage du calculateur web est invisible (aucun événement GA4), alors que c'est maintenant le canal qui
  grandit.

## 7. Perspectives, par ordre recommandé

| Ordre | Piste | Effort | Impact | Recommandation |
|---|---|---|---|---|
| 1 | **Demandes d'indexation** : article TRJ fusionné FR, formats de cotes FR, `/pt/app/`, 2 guides PT, `/pt/sobre/`, lexique ES, puis les 2 anciennes URL FR | nul (clics de Julien, quota ≈ 10/jour) | élevé | Faire cette semaine : débloque la mesure de la refonte |
| 2 | **Suivi des 301** : ajouter les 10 anciennes URL à `config.inspect.extraUrls` pour suivre leur sortie de l'index au prochain relevé | faible | moyen | Faire |
| 3 | **Mesure du calculateur web** : événements GA4 `calc_result`, `outcome_add`, `share` (gtag, soumis au Consent Mode, aucune donnée personnelle) | faible | élevé pour piloter | Faire : seul moyen de savoir si les visiteurs calculent |
| 4 | **Étape 7, repositionnement EN** : limiter au guide `/guides/calculate-bookmaker-margin/` (pos. 5,9, CTR 7 % après) : titre et extrait sur « bookmaker margin / overround / vig ». **Ne pas toucher l'accueil EN**, qui vient de gagner « rtp calculator » en position 5 | faible | moyen | Décider au bilan de décembre, une fois l'article FR recrawlé |
| 5 | **Contenu FR** : rien de nouveau avant que les 4 guides soient recrawlés ; ensuite, « marge bookmaker » (21 impressions BE / FR, 0 clic) est la requête à viser avec l'article fusionné, pas avec une page de plus | nul maintenant | | Attendre |
| 6 | **Belgique néerlandophone** : surveiller « rtp sport gokken » ; une 6e langue NL (9 pages) ne se justifie qu'au-delà de ≈ 50 impressions par mois sur des requêtes NL | moyen | incertain | Surveiller seulement |
| 7 | **iOS** : un tiers des mobiles du site ; le calculateur web est la réponse. Un manifeste PWA (« Ajouter à l'écran d'accueil », icône, hors ligne) coûte une journée et aucune publication | faible | moyen | À étudier après le point 3 |
| 8 | **AdSense** : redemander un examen quand le site dépasse 100 clics Search Console par mois sur 2 mois (novembre part sur ≈ 50). Retirer alors la bannière RGPD autonome (double bandeau) | faible | faible | Pas avant 2027 au rythme actuel |
| 9 | **Analytics in-app** : reposer la question à 100 appareils actifs (15 aujourd'hui) | | | Non, décision du 25/10 maintenue |
| 10 | **Dette** : `material-icons-extended` (35 Mo, build debug), Kotlin 2 / Compose, « Sécurité des données » Play vers `/fr/privacy/`, images `og-*.png` sombres sur un site clair, axe `nested-interactive` des cartes d'historique | faible à moyen | faible | Au fil de l'eau |
| 11 | **Documentation** : `CLAUDE.md` § 4 et journal indiquent versionCode 11 ; la production est en 12 | nul | | Corriger (validation de Julien pour `CLAUDE.md`) |

Écartées, reconduites du 23/10 sans nouveau chiffre : onboarding et notifications, nouveaux formats AdMob,
exclusion de pays, renommage de l'app, retouche du Consent Mode, générateur de site statique, cache
`.htaccess`, déploiement Git Infomaniak. Nouvelle piste écartée : chasser « calculo rtp » malgré sa position 5
(0 clic sur 14 impressions : l'intention n'est pas la nôtre).

## 8. À recouper par Julien dans les consoles

- Search Console : extrait réellement affiché pour `/fr/` sur « calcul trj » (meta description de 195
  caractères) ; rapport Pages : les 10 anciennes URL doivent passer en « Page avec redirection ».
- Search Console : faire les demandes d'indexation du point 1 (ordre : article TRJ FR, formats de cotes FR,
  `/pt/app/`, `/pt/guides/apostas-multiplas-rtp/`, `/pt/guides/lexico-apostas-esportivas/`, `/pt/sobre/`,
  lexique ES, `/fr/guides/trj-pari-sportif/`, `/fr/guides/comparer-bookmakers-trj/`).
- Play Console : « Portée et appareils » et sources d'acquisition (l'API ne donne que « Other ») ; vérifier
  le versionCode affiché pour la 2.2.0 (12 attendu).
- Devices : test du thème clair et du mode sombre sur S9 et S23+, toujours en attente depuis le 05/11.

## 9. Prochain bilan

Première semaine de décembre 2026 (`snapshot --all`, `inspect`, comparer à `data/2026-11-08/`) :
- Play : part Myanmar et taux de désinstallation sur 30 jours pleins ; actifs (15 le 03/11).
- Search Console : clics mensuels de novembre (12 au 07/11), CTR de `/fr/`, position de « marge bookmaker »,
  impressions des anciennes URL (attendu : 0), position du guide combinés.
- Inspection : crawl des 4 guides FR après la refonte, indexation des pages PT.
- GA4 : événements du calculateur si le point 3 est fait.
