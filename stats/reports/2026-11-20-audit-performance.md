# Audit de performance, évolution et projections : 20/11/2026

Relevé `stats/data/2026-11-20/` (AdMob, AdSense, GA4, Search Console et Play du 01/01 au 19/11/2026, `inspect`
sur 59 URL, `listing`), séries produites par `npm run series`, mesures techniques du 20/11. Rapports
précédents : `2026-10-23-analyse-globale.md`, `2026-11-08-audit-global.md`. Le bilan de la première semaine de
décembre est maintenu, en contrôle court (section 10).

## 1. Synthèse

**Réserve** : les volumes sont très faibles (18 appareils actifs, 50 clics Search Console par mois) et le
dernier audit date de 12 jours. Les écarts sont des ordres de grandeur, les projections des fourchettes.

Cinq constats :

1. **Le site a changé de régime le 19/10 et s'y maintient** : 1 900 impressions et 50 clics par mois, contre
   184 et 5 avant. Aucun pic en novembre, 50 à 95 impressions par jour.
2. **Le clic dépend de la position, pas du volume** : 8,0 % en positions 1 à 3, 4,8 % en 4 à 6, 1,3 % en 7 à 10.
   Le plus gros paquet d'impressions est en 7 à 10.
3. **Le parc de l'app a doublé en trois semaines** (10 appareils actifs fin octobre, 18 le 16/11), porté par le
   Brésil. Le Myanmar, lui, n'a pas bougé : la conclusion du 08/11 est infirmée.
4. **Le site est techniquement rapide, sauf les calculateurs** : le script AdSense, pour un compte jamais
   activé, y double le temps de chargement sur mobile.
5. **La monétisation reste hors sujet** dans tous les scénarios : 1,93 € cumulés, aucun seuil de paiement
   atteignable à 12 mois.

Trois chiffres : **34 clics** Search Console en 19 jours de novembre (18 sur tout octobre) ; **18 appareils
actifs** (7 le 22/10) ; **4,3 s** de chargement des calculateurs sur mobile bridé, **2,0 s** sans AdSense.

| Actif | Verdict | Tendance |
|---|---|---|
| Site, visibilité | Bon, en progression | Nouveau palier depuis le 19/10, tenu 4 semaines |
| Site, technique | Très bon, un défaut (AdSense sur les calculateurs) | Stable |
| Site, usage du calculateur | Inconnu : aucun événement reçu | À vérifier |
| App, acquisition | Faible mais en hausse | Novembre = meilleur mois |
| App, rétention | Faible : 84 % de désinstallations en novembre | Légère amélioration (94 % avant) |
| App, technique | Bon : 3,4 Mo, note 5,0, aucun problème signalé | Stable |
| Monétisation | Négligeable | Sans perspective à 12 mois |

## 2. Sources, périodes, limites

| Source | Période utile | Réserve |
|---|---|---|
| Play (CSV du bucket) | 10/05 → 16/11 | Fiche (visiteurs, acquisitions) arrêtée au 07/11 : retard de Google, pas des zéros. Sources de trafic agrégées en « Other » |
| AdMob | 06/05 → 19/11 | Une bannière ; les requêtes servent de proxy d'ouverture de l'app |
| AdSense | état du compte | Aucune ligne de rapport : site jamais activé |
| Search Console | 02/06 → 19/11 | Requêtes rares anonymisées : totaux depuis les séries par jour ou par page. 27/10 retiré (anomalie, 276 impressions d'outils automatisés) |
| GA4 | 06/2026 → 19/11 | Minoré par le Consent Mode (41 % des clics Search Console en novembre) ; juin = robots |
| URL Inspection | 24/10, 08/11, 20/11 | 45 pages du sitemap + 14 URL suivies (4 variantes, 10 anciennes pages en 301) |
| Mesures de vitesse | 20/11 | Laboratoire, pas de données de terrain (CrUX vide sous le seuil de Google) |

## 3. App Android

### 3.1 Acquisition et rétention

| Mois | Installs | Désinstalls | Désinst. / inst. | Actifs fin de mois | Visiteurs fiche | Acquisitions | Conversion |
|---|---|---|---|---|---|---|---|
| Mai | 16 | 14 | 88 % | 3 | 68 | 15 | 22 % |
| Juin | 14 | 13 | 93 % | 4 | 77 | 13 | 17 % |
| Juillet | 9 | 11 | 122 % | 1 | 38 | 8 | 21 % |
| Août | 21 | 17 | 81 % | 8 | 49 | 22 | 45 % |
| Septembre | 22 | 22 | 100 % | 7 | 71 | 17 | 24 % |
| Octobre | 18 | 16 | 89 % | 10 | 86 | 17 | 20 % |
| Novembre (16 jours) | 25 | 21 | 84 % | 18 | 21 (au 07/11) | 12 (au 07/11) | 57 % |

Cumul : 125 installations, 114 désinstallations, 18 appareils actifs le 16/11 (pic à 20 le 15/11). Novembre
est déjà le meilleur mois en installations ; la seule semaine du 02/11 en compte 15, avec une conversion de
la fiche de 63 % (12 acquisitions pour 19 visiteurs), jamais vue auparavant.

### 3.2 Pays

| Pays | Mai | Juin | Juil. | Août | Sept. | Oct. | Nov. | Total installs | Total désinstalls | Solde |
|---|---|---|---|---|---|---|---|---|---|---|
| Myanmar | 5 | 7 | 3 | 7 | 6 | 5 | 5 | 38 | 39 | -1 |
| Brésil | 3 | 0 | 3 | 5 | 4 | 2 | 5 | 22 | 17 | +5 |
| France | 3 | 0 | 0 | 2 | 0 | 0 | 1 | 6 | 5 | +1 |
| 37 autres pays | | | | | | | | 59 | 53 | +6 |

- **Myanmar** : 5 installations et 5 désinstallations en novembre, comme chaque mois depuis mai. La nouvelle
  fiche du 25/10 n'a pas coupé ce flux. Le « 0 install sur 9 jours » du 08/11 était un creux, pas un effet.
  Ce pays pèse 30 % des installations pour un solde nul, mais 1,32 € des 1,93 € de revenus AdMob.
- **Brésil** : seul pays dont le solde est nettement positif. Parc actif le 16/11 : Brésil 5, France 2,
  Myanmar 2, Taïwan 2, puis 7 pays à 1. Le titre PT-BR « Calculadora RTP - Apostas » du 25/10 est la seule
  action de la fiche dont l'effet se lit : 1 installation dans les 26 jours avant, 6 dans les 23 jours après.

### 3.3 Usage, qualité, monétisation

| Mois | Requêtes AdMob | Par jour | Revenus | Cumul |
|---|---|---|---|---|
| Mai | 285 | 9,2 | 0,17 € | 0,17 € |
| Juin | 294 | 9,8 | 0,01 € | 0,18 € |
| Juillet | 49 | 1,6 | 0,23 € | 0,42 € |
| Août | 175 | 5,6 | 0,19 € | 0,61 € |
| Septembre | 212 | 7,1 | 0,69 € | 1,30 € |
| Octobre | 74 | 2,4 | 0,09 € | 1,39 € |
| Novembre (19 jours) | 154 | 5,1 (8,1 sur les jours écoulés) | 0,54 € | 1,93 € |

L'usage remonte en novembre mais reste dans la fourchette de mai, juin et septembre : le doublement du parc
ne se traduit pas encore par un doublement des ouvertures. Note 5,0 (un seul noteur, au Myanmar, avis
renouvelé en 2.2.0) ; vitals vides ; 0 problème signalé. 15 appareils sur 18 sont en 2.2.0 (code 12).

### 3.4 Technique

| Élément | Valeur |
|---|---|
| APK release universel (2.2.0, non signé) | 3,4 Mo |
| dont code après R8 | 2,5 Mo |
| dont table des ressources | 0,56 Mo |
| AAB envoyé à Play | 6,8 Mo, surtout des métadonnées non livrées |
| APK debug | 20,9 Mo (bibliothèque d'icônes étendue, dette connue) |
| Android du parc actif | 11 à 17 ; 15 appareils sur 18 en Android 14 ou plus |

L'app est légère, rien d'utile à gagner sur le poids. Aucun appareil actif sous Android 10 ou moins : le S9
teste un segment absent du parc actuel, il garde son rôle de garde-fou mémoire. Démarrage à froid : non
mesuré (aucun device branché).

## 4. Site

### 4.1 Visibilité (Search Console, hors 27/10)

| Mois | Impressions | Clics | CTR | Position |
|---|---|---|---|---|
| Juin | 20 | 0 | | 13,5 |
| Juillet | 102 | 0 | | 26,0 |
| Août | 204 | 6 | 2,9 % | 12,6 |
| Septembre | 184 | 3 | 1,6 % | 12,5 |
| Octobre | 686 | 13 | 1,9 % | 10,2 |
| Novembre (19 jours) | 1 224 | 34 | 2,8 % | 12,1 |

| Semaine du | Impressions | Clics | Position |
|---|---|---|---|
| 05/10 | 40 | 1 | 12,5 |
| 12/10 | 39 | 2 | 14,7 |
| 19/10 | 298 | 4 | 9,7 |
| 26/10 | 348 | 8 | 9,9 |
| 02/11 | 404 | 11 | 8,7 |
| 09/11 | 513 | 11 | 13,7 |
| 16/11 (4 jours) | 256 | 10 | 14,6 |

La position moyenne se dégrade en apparence depuis le 09/11 : c'est le lexique ES, réindexé, qui entre en
position 30 à 34 avec 121 impressions. Les pages existantes ne reculent pas.

| Page | Octobre : clics / impr. / pos. | Novembre : clics / impr. / pos. |
|---|---|---|
| `/` (accueil EN) | 5 / 123 / 6,7 | **16 / 244 / 5,3** |
| Lexique FR | 1 / 132 / 9,6 | 0 / 157 / 9,2 |
| `/fr/` | 0 / 55 / 9,9 | 4 / 154 / 7,8 |
| Lexique ES | | 1 / 121 / 34,1 |
| Guide « marge » FR | 1 / 40 / 8,7 | 0 / 78 / 11,7 |
| Formats de cotes FR | 0 / 28 / 8,6 | 0 / 58 / 16,9 |
| `/app/` | 0 / 16 / 9,2 | 3 / 51 / 6,2 |
| `/fr/app/` | 0 / 24 / 10,3 | 0 / 43 / 8,1 |
| `/pt/` | 1 / 23 / 7,0 | 4 / 32 / 4,5 |
| `/guides/rtp-sports-betting/` (supprimée le 25/10) | 2 / 36 / 5,6 | 1 / 42 / 6,8 |

L'accueil EN porte la moitié des clics du site. Pays en novembre : France 214 impressions (3 clics),
Belgique 135 (1), Brésil 111 (5), États-Unis 91 (2), Allemagne 79 (1). Mobile 701 impressions et 20 clics,
ordinateur 516 et 14.

**Taux de clic par position, depuis le 25/10 (pages × jours)** :

| Position | Impressions | Clics | CTR |
|---|---|---|---|
| 1 à 3 | 88 | 7 | 8,0 % |
| 4 à 6 | 455 | 22 | 4,8 % |
| 7 à 10 | 635 | 8 | 1,3 % |
| 11 à 20 | 309 | 2 | 0,6 % |
| 21 et plus | 248 | 1 | 0,4 % |

C'est le résultat le plus utile de l'audit : passer de 7 à 10 vers 4 à 6 multiplie le clic par 3,7. Les pages
FR (lexique, `/fr/`, `/fr/app/`, guide « marge ») sont précisément dans cette tranche.

### 4.2 Indexation

| État (45 pages du sitemap) | 24/10 | 08/11 | 20/11 |
|---|---|---|---|
| Envoyée et indexée | 35 | 37 | 40 |
| Explorée, non indexée | 3 | 3 | 4 |
| Détectée, non indexée | 3 | 1 | 0 |
| Inconnue de Google | 4 | 4 | 1 |

Gagnées depuis le 08/11 : les pages PT, `/de/app/`, le lexique ES. Perdues : les guides ES « marge » et
« formats de cotes », passés en « explorée, non indexée » (crawl du 13 et du 15/10, donc avant la refonte).
Restent hors index : `/fr/privacy/`, `/es/privacy/`, `/de/privacy/` (jamais vue).

**Les redirections du 25/10 ne sont pas vues** : 7 anciennes URL sur 10 sont toujours « envoyées et
indexées », Google ne les a pas recrawlées depuis le 13 au 24/10. 3 sont passées en « page avec
redirection ». Les 4 variantes http et www sont réglées. 18 pages sur 45 n'ont pas été recrawlées depuis la
refonte (dernier passage avant le 26/10).

### 4.3 Audience et usage (GA4)

| Mois | Sessions | Organiques | Directes | Assistant IA |
|---|---|---|---|---|
| Août | 7 | 2 | 5 | |
| Septembre | 2 | 1 | 1 | |
| Octobre | 15 | 7 | 6 | 2 |
| Novembre (19 jours) | 15 | 14 | 1 | |

GA4 voit 14 sessions organiques pour 34 clics Search Console (41 %). Clics vers Play : 5 en tout, dont 3 par
les liens UTM, aucun depuis le 03/11.

**Aucun événement `rtp_save`, `rtp_add_outcome` ni `rtp_share`** depuis leur déploiement du 08/11. GA4 ne
compte que 4 pages vues des calculateurs en novembre : impossible de dire si le marquage est muet ou si
personne n'a enregistré de calcul. L'usage du calculateur web reste inconnu.

### 4.4 Technique (production, 20/11)

Méthode : 6 pages, 3 passes à froid, première visite sans consentement. Mobile bridé = 4G lente
(1,6 Mbit/s, 150 ms) et processeur ralenti 4 fois.

| Page | Affichage principal, mobile | Fin de chargement, mobile | Blocage du fil principal, mobile | Affichage principal, ordinateur | Requêtes | Poids |
|---|---|---|---|---|---|---|
| `/` | 1,2 s | 2,0 s | 550 ms | 0,33 s | 9 | 244 Ko |
| `/fr/` | 1,2 s | 1,9 s | 514 ms | 0,35 s | 9 | 244 Ko |
| `/fr/guides/` | 1,0 s | 2,1 s | 364 ms | 0,28 s | 7 | 231 Ko |
| Guide « marge » FR | 1,2 s | 2,0 s | 515 ms | 0,33 s | 9 | 245 Ko |
| `/fr/app/` | 1,0 s | 4,4 s | 872 ms | 0,35 s | 22 | 584 Ko |
| `/pt/app/` | 1,0 s | 4,3 s | 878 ms | 0,35 s | 22 | 584 Ko |

| Origine | Poids | Pages |
|---|---|---|
| Fichiers du site | 16 à 27 Ko | toutes |
| Google Fonts | 47 Ko | toutes |
| Google Analytics | 179 Ko | toutes |
| Script AdSense | 330 Ko, 10 requêtes | les 5 calculateurs |

Affichage principal à 1,0 à 1,2 s sur mobile bridé (seuil « bon » de Google : 2,5 s), aucun décalage de mise
en page, serveur en 70 à 100 ms, gzip et HTTP/2 actifs, aucune ressource en erreur. Les tiers font 93 à 95 %
des octets. **Seul défaut : le script AdSense**, inerte puisque le compte n'est pas activé, qui double le
chargement et le blocage sur les pages où l'on saisit. Pas d'en-tête de cache (ETag seul) : effet limité aux
visites répétées, piste écartée le 23/10, non rouverte.

## 5. Monétisation

AdMob : 1,93 € en six mois et demi, 0,24 € par mois jusqu'au 25/10, 0,68 € depuis. AdSense : compte `READY`,
site en `NEEDS_ATTENTION`, aucun bloc, inchangé. Aucun scénario n'approche un seuil de paiement (section 7).
Le seul enjeu réel de la monétisation aujourd'hui est son coût : le script AdSense ralentit les calculateurs.

## 6. Évolution : ce qui est attribuable

| Date | Action ou événement | Rupture observée | Attribution |
|---|---|---|---|
| 18/10 | Recrawl massif par Google | Impressions × 7 dès le 19/10 (39 → 298 par semaine), tenu depuis | Google, pas une action du projet |
| 24/10 | Lot CTR (titres, extraits) | Accueil EN : 5 → 16 clics, position 6,7 → 5,3 ; `/fr/` 0 → 4 clics | Attribuable, confirmé sur 4 semaines |
| 24/10 | Sitemap, demandes d'indexation | 35 → 40 pages indexées | Attribuable |
| 25/10 | Fiche Play FR / PT-BR | Brésil 1 → 6 installations ; conversion de la fiche 21 % → 41 % (au 07/11) | Probable pour le Brésil ; **nul pour le Myanmar** |
| 25/10 | Refonte à 45 pages, 301 | 7 anciennes URL sur 10 encore indexées | Non mesurable, Google n'a pas relu |
| 29/10 | App 2.2.0 | 15 / 18 en 2.2.0 ; 15 installations la semaine du 02/11 | Adoption oui ; le pic d'installations coïncide, cause inconnue |
| 05/11 | Thème clair et mode sombre | Aucune | Non mesurable |
| 08/11 | Événements GA4 du calculateur | Aucun événement reçu | À vérifier |
| 11/11 | Favicon en fichiers | Aucune encore | Contrôle en décembre |

Deux ruptures seulement expliquent la trajectoire : le recrawl du 18/10 pour le site, la semaine du 02/11
pour l'app. Aucune des deux n'est une action directe du projet ; les actions d'octobre ont amélioré le
rendement de ce trafic (clics de l'accueil EN, installations brésiliennes), pas son volume.

## 7. Projections

### 7.1 Méthode et réserve

| Indicateur | Régime ancien | Régime récent |
|---|---|---|
| Impressions par mois | 184 (01/08 → 18/10) | 1 900 (depuis le 25/10) |
| Clics par mois | 4,6 | 50 |
| Installations par mois | 18 (mai → 25/10) | 39 (depuis le 26/10) |
| Solde net par mois | +1,1 | +7 (16 jours) |
| AdMob par mois | 0,24 € | 0,68 € |

**Contrôle rétrospectif échoué** : le rythme des 4 semaines closes fin octobre annonçait 440 impressions et
18 installations par mois ; novembre en fait 1 950 et 47. Un prolongement à plat ne voit pas les ruptures.
Le scénario tendanciel ne vaut donc que sans nouveau changement de régime, et quatre semaines ne permettent
pas d'estimer une croissance : les taux du scénario favorable sont des choix, pas des mesures.

| Scénario | Site | App |
|---|---|---|
| Prudent | La moitié du gain d'octobre retombe, puis plat | Rythme de mai à octobre, parc revenu au palier de septembre |
| Tendanciel | Rythme depuis le 25/10, à plat | 30 installations par mois, solde net de +1,1 par mois |
| Favorable | Impressions +10 % par mois sur 3 mois puis +5 % ; CTR de 2,7 % à 4 % | Rythme de novembre, solde net de +4 par mois |

### 7.2 Scénarios à 3, 6 et 12 mois (fin février, fin mai, fin novembre 2027)

| Indicateur, par mois | Aujourd'hui | Prudent | Tendanciel | Favorable : 3 / 6 / 12 mois |
|---|---|---|---|---|
| Impressions Search Console | 1 900 | 1 050 | 1 900 | 2 500 / 2 900 / 3 900 |
| Clics Search Console | 50 | 26 (15 à 37) | 50 (35 à 65) | 76 / 103 / 157 |
| Sessions organiques GA4 | 22 | 10 | 20 | 30 / 41 / 63 |
| Pages indexées sur 45 | 40 | 40 | 42 | 45 |
| Installations | 39 | 18 (10 à 26) | 30 (19 à 41) | 45 (32 à 58) |
| Désinstallations / installations | 84 % | 95 % | 90 % | 80 % |

| Indicateur, à l'horizon | Aujourd'hui | Prudent : 3 / 6 / 12 mois | Tendanciel : 3 / 6 / 12 mois | Favorable : 3 / 6 / 12 mois |
|---|---|---|---|---|
| Appareils actifs | 18 | 10 / 10 / 10 | 21 / 25 / 31 | 30 / 42 / 66 |
| AdMob cumulé | 1,93 € | 2,70 € / 3,40 € / 4,90 € | 3,40 € / 4,90 € / 7,90 € | 5,50 € / 9,10 € / 16 € |

Non projetable : les enregistrements de calculs sur le site, aucun événement n'ayant été reçu.

### 7.3 Jalons

| Seuil fixé | Prudent | Tendanciel | Favorable |
|---|---|---|---|
| 100 clics Search Console par mois sur 2 mois (AdSense) | non | non | vers juin 2027 |
| 100 appareils actifs (analytics in-app) | non | non | non, vers mi-2028 à ce rythme |
| 50 impressions par mois sur des requêtes néerlandaises (6e langue) | non | non, 21 aujourd'hui | limite, fin 2027 |
| Seuil de paiement AdMob (70 €, à vérifier dans la console) | hors d'atteinte | hors d'atteinte | hors d'atteinte |

Le site peut franchir son seuil dans le meilleur cas ; l'app ne franchit aucun des siens à 12 mois ; la
monétisation est hors sujet dans les trois scénarios. Le levier qui déplace le plus un scénario vers le
suivant est la position des pages FR (section 4.1), pas le volume d'impressions.

## 8. Écarts avec l'audit du 08/11

| Statut | Constat du 08/11 | Au 20/11 |
|---|---|---|
| Confirmé | Le site est l'actif qui progresse | Palier tenu 4 semaines, 34 clics en 19 jours |
| Confirmé | Le lot CTR a amélioré l'accueil EN | 16 clics en novembre, position 5,3 |
| Confirmé | Monétisation hors sujet | 1,93 €, aucun seuil atteignable |
| **Infirmé** | Myanmar : 0 installation depuis la nouvelle fiche | 5 installations en novembre, comme chaque mois |
| **Infirmé** | Désinstallations ramenées de 88 % à 44 % | 84 % sur novembre : les 9 jours étaient trop courts |
| Nuancé | Position moyenne 8,6 | 12,1, à cause de l'entrée du lexique ES en position 30 |
| Nuancé | Événements du calculateur « à contrôler sous 48 h » | Aucun reçu en 12 jours, cause non établie |
| Nouveau | | Brésil premier pays du parc actif (5 sur 18) |
| Nouveau | | CTR par tranche de position : le gain est en 7 à 10 |
| Nouveau | | Script AdSense : chargement des calculateurs doublé sur mobile |
| Nouveau | | Guides ES « marge » et « formats » sortis de l'index |
| Nouveau | | 7 anciennes URL sur 10 toujours indexées |

Leçon de méthode : une fenêtre de 9 jours a produit deux conclusions fausses. Aucun effet ne sera plus
déclaré sur moins de 28 jours.

## 9. Pistes, par ordre recommandé

| Ordre | Piste | Effort | Impact | Recommandation |
|---|---|---|---|---|
| 1 | **Retirer le script AdSense des 5 calculateurs** tant que le compte n'est pas approuvé (le remettre le jour d'une approbation) | faible : 5 pages, sitemap, déploiement | chargement mobile 4,3 s → 2,0 s sur les pages de saisie | Faire, sur demande de déploiement de Julien |
| 2 | **Vérifier les événements `rtp_*`** : un calcul enregistré sur `/fr/app/` après consentement, lecture en Temps réel GA4 | nul (Julien, 5 minutes) | lève l'inconnue sur l'usage | Faire cette semaine |
| 3 | **Demandes d'indexation** : les 7 anciennes URL encore indexées (pour que Google voie les 301), les guides ES « marge » et « formats », `/de/privacy/` | nul (clics de Julien, 10 par jour) | débloque la mesure de la refonte | Faire |
| 4 | **Pages FR en positions 7 à 10** (lexique, `/fr/`, `/fr/app/`, guide « marge ») : titres et extraits, sans nouvelle page | faible | le plus fort levier de clics (CTR × 3,7 en 4 à 6) | Décider au bilan de décembre, une fois les guides recrawlés |
| 5 | **Étape 7, guide EN « bookmaker margin »** : titre et extrait seuls | faible | moyen | Inchangé : décision en décembre |
| 6 | **Brésil** : seul marché où l'app retient ; `/pt/` en position 4,5 | nul | | Surveiller, pas d'action avant 28 jours de recul |
| 7 | **PWA iOS** du calculateur web | une journée | moyen | Après le point 2 |
| 8 | **Dette** : `material-icons-extended`, Kotlin 2 / Compose, images `og-*.png` sombres, axe `nested-interactive` | faible à moyen | faible | Au fil de l'eau |

Seuils inchangés : AdSense à 100 clics par mois sur 2 mois, analytics in-app à 100 appareils actifs, 6e
langue NL à 50 impressions par mois. Pistes écartées non rouvertes, faute de chiffre nouveau : exclusion de
pays (le Myanmar apporte 68 % des revenus AdMob et le seul avis), nouveaux formats AdMob, cache `.htaccess`,
chasse à « calculo rtp », réécriture de l'accueil EN, nouvelle page FR.

## 10. Bilan de décembre (première semaine)

`snapshot --all`, `inspect`, puis `npm run series -- --pivot=2026-10-25 --window=40`. À relire :

- Search Console : clics de novembre complet (attendu 50 à 55), tenue du palier de 1 900 impressions, positions
  des 4 pages FR en 7 à 10, impressions des anciennes URL.
- Inspection : anciennes URL passées en « page avec redirection », guides ES réindexés, crawl des 18 pages non
  relues depuis la refonte.
- Play : installations et désinstallations de novembre complet, parc actif (18 le 16/11), part du Brésil,
  données de la fiche après le 07/11.
- GA4 : événements `rtp_*` après la vérification du point 2.
- Favicon : affichage dans les résultats Google (déployé le 11/11).
- Projections : situer novembre et début décembre par rapport aux trois scénarios.
