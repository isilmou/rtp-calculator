# Audit de présentation du site rtpcalculator.com — 24/10/2026

## Contexte

Julien constate sur navigateur et sur mobile des défauts de mise en page : textes mal ajustés,
titres ou boutons qui paraissent non centrés. Audit neutre de la présentation au regard des
bonnes pratiques de clarté, préalable à toute correction (règles 1 et 2 de `CLAUDE.md`).

**Méthode.** Lecture du HTML/CSS des 6 types de page FR, puis **mesures et captures** avec
l'outillage `tools/` créé pour l'occasion (Playwright headless, voir § 4) : 8 pages
(`/fr/`, `/fr/app/`, `/fr/guides/`, `/fr/guides/formats-de-cotes/`, `/fr/a-propos/`,
`/fr/privacy/`, `/de/`, `/de/app/`) × 3 largeurs (375, 768, 1280 px), copie locale **et**
production (résultats identiques : la production est bien l'état du dépôt), plus un passage
axe-core (WCAG 2.x A/AA). Les 5 langues partagent le même CSS par type de page (empreintes
identiques), les constats valent donc pour les 55 pages.

Dossiers de référence (non versionnés) : `tools/shots/2026-10-24-1224-local/`,
`tools/shots/2026-10-24-1225-prod/`, `tools/shots/2026-10-24-1226-a11y/` (chacun avec
`report.md` / `report.json`).

Référentiels : WCAG 2.2 (1.4.3 contraste texte 4,5:1 ; 1.4.11 contraste non textuel 3:1 ;
2.5.8 cible 24 px mini), Material Design 3 (cibles 48 dp), Apple HIG (44 pt), Nielsen Norman
Group (navigation masquée, libellés de boutons verbe + objet), Lighthouse (texte ≥ 12 px).

---

## 1. Constats mesurés

Gravité : **H** visible par tout visiteur ou bloque la lecture · **M** nuit à la clarté ·
**B** finition. Colonne « Mesure » : ce que l'outil a relevé (confirme ou infirme la lecture
du code).

### A. Alignement et centrage

| # | Constat | Cause (fichier : sélecteur) | Mesure | Grav. |
|---|---|---|---|---|
| A0 | **Le bloc héros de l'accueil a perdu sa gouttière** : H1, texte et boutons collés au bord gauche sur mobile, et 24 px à gauche des autres sections sur desktop. C'est le défaut d'alignement le plus visible du site. | `.hero{padding:72px 0 84px}` écrase le `padding:0 24px` de `.wrap` (même spécificité, déclaré après) | H1 x = **0** à 375 et 768 px ; x = 110 à 1280 px contre 134 pour les H2 | **H** |
| A1 | Accueil desktop : la section « Politique de confidentialité » est une colonne de 76 ch **centrée**, alors que les autres sections sont alignées à gauche. | `.legal .max{max-width:76ch}` sur `.wrap.max` | H2 x = **315** à 1280 px contre 134 ; 59 contre 24 à 768 px | H |
| A2 | Accueil mobile : carte de démonstration à gauche, légende alignée à droite. | `.demo-caption{text-align:right}` non annulé sous 820 px | visible sur `fr-375.png` | M |
| A3 | Accueil mobile : les deux boutons héros passent sur deux lignes, alignés à gauche, de largeurs différentes. | `.hero-ctas{flex-wrap:wrap}` sans règle mobile | visible sur `fr-375.png` | M |
| A4 | Boutons à libellé long sur **deux lignes, texte aligné à gauche** dans le bouton : cartes « Application » (accueil) à 375 **et** 768 px ; CTA des guides à 375 px (centré mais sur deux lignes). Idem DE. | `.btn`, `.cta a` : `height` fixe, capitales + `letter-spacing:.05em`, libellés de 33 à 42 caractères | « Ouvrir le calculateur (version navigateur) » : 2 lignes, `start` ; « Télécharger la version Android » : 2 lignes | H |
| A5 | Hors accueil, sur mobile : logo à 16 px du bord, contenu à 20 px. | `.nav{padding:0 16px}` (≤ 560) vs `.wrap{padding:0 20px}` | 16 / 20 sur app, guides, à-propos, privacy | M |
| A6 | Desktop hors accueil : en-tête sur 1060 px, contenu sur 720 ou 760 px, pied sur 720/760. Trois largeurs. | `.nav{max-width:1060px}` vs `.wrap` 720/760 | logo x = 134, contenu x = 280 ou 300 | M |

### B. Mobile

| # | Constat | Cause | Mesure | Grav. |
|---|---|---|---|---|
| B0 | **Défilement horizontal de l'accueil** à 375 et 768 px (+ 24 px). | Cause probable : `.demo-wrap::before{inset:-48px -24px}` (halo décoratif absolu) qui déborde à droite ; à confirmer à la correction | largeur de page 399 / 792 px | H |
| B1 | Menu principal en défilement horizontal à barre masquée : trois entrées **hors écran sans indice**. | `.nav-links{overflow-x:auto;scrollbar-width:none}` | FR : « Confidentialité, À propos, Contact » hors écran à 375 px ; DE app : « Über uns, Kontakt » | H |
| B2 | En-tête mobile sur **trois rangées** (logo / sélecteur de langue seul sur sa ligne / menu) : 112 à 123 px de haut, collant sur accueil et calculateur (15 % d'un écran de 740 px). En DE, « RTP-Rechner » tient avec le sélecteur : 84 px. | logo « Calculateur de TRJ » + sélecteur 180 px > largeur disponible à 375 px | 112 px (accueil), 122–123 px (autres), 84 px (`/de/app/`) | M |
| B3 | Guide « formats de cotes » : tableau de 4 colonnes plus large que l'écran → **toute la page défile horizontalement**, le pied de page est coupé. Même structure dans `trj-paris-combines`. | `.tbl{width:100%}` sans conteneur `overflow-x:auto` | largeur de page 422 px à 375 px | H |
| B4 | Cibles tactiles sous les minima : sélecteur de langue 36×26 px, actions d'historique ≈ 22 px, bouton « Trier » ≈ 16 px. | `.lang a`, `.hact`, `.sort-btn` | lecture du code (axe ne teste pas la taille des cibles) | M |
| B5 | Placeholders « Date (facultatif) » / « Heure (facultatif) » : **tiennent à 375 px** ; tronqués sans points de suspension seulement sous ≈ 340 px. | `.dt-ph{overflow:hidden}` | infirmé à 375 px | B |

### C. Typographie et lisibilité

| # | Constat | Cause | Mesure | Grav. |
|---|---|---|---|---|
| C1 | **Graisse 400 non chargée** : le corps de texte est rendu en 500, tout le site est un cran plus lourd que prévu. | Google Fonts `Archivo:wght@500;600;700;800` | lecture du code | M |
| C2 | Contraste de `--text-3` (#666 sur #0A0A0A) = **3,44:1**, sous le seuil AA 4,5:1. Porte l'avertissement **18+ / jeu responsable**, les pieds de page, les dates, les placeholders, le message « Aucune analyse », les titres « À lire ensuite ». | `--text-3:#666666` | axe : **44 éléments** sur 16 pages, règle `color-contrast` (serious), tous à 3,44 | H |
| C3 | **Liens dans le texte repérables par la couleur seule** : jaune #FFD600 sur gris #AAA = 1,64:1 (3:1 requis) et pas de soulignement hors survol. | `a{text-decoration:none}` | axe : **18 éléments**, règle `link-in-text-block` (serious), accueil, privacy | M |
| C4 | Tailles sous 12 px : 8,5–10 px (noms de bookmakers, libellés d'issue, caret), 10,5 px (sélecteur de langue, segments EU/UK/US), 11–11,5 px (métadonnées). | carte démo, historique, `.lang a`, `.fmt button` | 7 à 8 classes concernées par page | M |
| C5 | Hiérarchie des sections d'accueil irrégulière : un seul kicker (« Mentions ») sur trois sections ; H2 de longueurs très inégales ; lede terminée par « : ». | `fr/index.html` | — | B |
| C6 | Ordre des titres : `/fr/privacy/` passe de H1 à H3. Calculateur : deux H2 à deux tailles (19 et 17 px). | `h3` directs ; `.seo-text h2` | axe : `heading-order` (moderate) ×2 | B |
| C7 | Corps de texte des cartes à 13,5–14 px en gris : correct en contraste (8,5:1) mais petit sur mobile. | `.gcard p`, `.appcard p` | — | B |

### D. Boutons et actions

| # | Constat | Cause | Grav. |
|---|---|---|---|
| D1 | **Quatre systèmes de boutons** : `.btn` 46 px / 13 px, `.play-btn` 42 px / 12 px, `.cta a` 44 px / 12,5 px, bannière cookies 42 px / 12,5 px. | accueil, app, guides, script CMP | M |
| D2 | Bouton secondaire « fantôme » : bordure #1F1F1F sur #0A0A0A = **1,2:1** (3:1 requis). « Télécharger sur Android » du héros se lit comme un texte gris. | `.btn-ghost{border:1px solid var(--divider)}` | M |
| D3 | Libellés longs et parenthésés (« Ouvrir le calculateur (version navigateur) », 42 car.) ; le contexte est déjà le titre de la carte. Cause directe de A4. **Copie : règle 3** (FR validé puis EN/ES/DE/PT-BR). | accueil, guides | M |
| D4 | Capitales + espacement sur tous les boutons : libellés 25 % plus longs, lisibilité moindre. **Tranché par Julien le 24/10 : casse de phrase, graisse 600.** | `.btn`, `.play-btn`, `.cta a`, `.btn-save` | B |

### E. Cohérence entre pages

| # | Constat | Grav. |
|---|---|---|
| E1 | **Quatre pieds de page** : accueil (3 liens + 18+), calculateur (6 liens, **sans avertissement 18+**), guides (4 liens + 18+), confidentialité (2 liens, sans 18+). | M |
| E2 | Menu déroulant « Guides » sur l'accueil seulement ; en-tête collant sur 2 types de page sur 6 ; `scroll-padding-top` sur l'accueil seulement alors que `#apps` / `#contact` sont visés depuis toutes les pages. | B |
| E3 | **Cause racine** : CSS (≈ 150 lignes) et script de consentement (170 lignes) recopiés dans les 55 pages ; trois `.wrap`, deux `.nav`, quatre `footer` ont déjà dérivé. | H (structurel) |

### Ce qui est bon et à conserver

Palette sombre cohérente avec l'app (jetons, dégradé TRJ signature) ; `focus-visible` et
`prefers-reduced-motion` ; `clamp()` sur les titres ; `tabular-nums` sur les cotes ;
`aria-label`/`role="img"` sur la carte démo ; `aria-current` sur la langue ; largeur de lecture
60–70 ch sur les guides ; `inputmode` sur les champs. Rendu desktop des guides, de l'à-propos et
de la confidentialité : propre. Aucune refonte : aligner, unifier, corriger.

---

## 2. Décisions (Julien, 24/10/2026)

1. **Feuille de style partagée** `RTP Calculator - Website/assets/site.css` + `assets/cmp.js`,
   référencées par les 55 pages à la place des blocs inline. Une correction = un fichier, dérive
   impossible, une requête mise en cache. Ce n'est pas le générateur de site écarté le 23/10.
   Écarté : CSS inline propagé par script (dérive toujours possible, 55 fichiers par retouche).
2. **Menu mobile sur deux lignes** (`flex-wrap`, sans JS). Écarté : hamburger (navigation
   masquée, JS) ; défilement + dégradé (découvrabilité moyenne).
3. **Boutons en casse de phrase**, graisse 600, sans espacement, `min-height` 44 px, centrés,
   pleine largeur sous 560 px.

---

## 3. Plan de correction par lots

Un lot = validation de Julien, `git diff` montré, planche avant / après (`tools/shots.mjs
--compare`), test S9 + S23+ pour le mobile, `node tools/sitemap.mjs` avant déploiement,
déploiement sur demande explicite. Le CSS est indépendant de la langue ; seul le lot 6 (copie)
suit la règle 3.

| Lot | Contenu | Constats | État |
|---|---|---|---|
| 0 | Outillage visuel, état de référence, cet audit, journal | — | **fait le 24/10/2026** |
| 1a | `assets/site.css` + `assets/cmp.js` **à rendu identique** : extraction des blocs inline des 55 pages par script (refus si le bloc n'est pas trouvé exactement une fois) | E3 | **fait le 24/10/2026** (local) : 162 captures avant/après identiques, racine EN identique à la production, texte des pages inchangé ; HTML −53 % |
| 1b | Corrections transversales dans `site.css` : jetons de largeur et gouttière, système de boutons unique, en-tête collant et pied uniques (18+ partout), graisse 400, `--text-3` #8A8A8A, liens soulignés dans le texte, bordure fantôme 3:1, aucune taille < 11 px, `scroll-padding-top` global, halo du héros borné (plus d'`overflow-x:clip` global : il aurait masqué le tableau des guides), bouton « supprimer » désactivé (cyan → blanc 30 %) | A4 (partiel : centré, 2 lignes jusqu'au lot 6), A5, B0, C1, C2, C3, C4, D1, D2, D4, E1, E2 | **fait le 24/10/2026** (local) : axe 0 contraste / 0 lien ; régression `main{padding}` détectée et corrigée |
| 2 | Accueil : gouttière du héros, section confidentialité alignée, légende de la carte, boutons héros empilés (déjà par le lot 1b) ; kickers reportés au lot 6 (copie) | A0, A1, A2, A3 | **fait le 24/10/2026** (local) : H1 et H2 à la même abscisse aux 3 largeurs |
| 3 | Menu mobile sur deux lignes ; sélecteur de langue compact (30×30 px) sur la ligne du logo ; en-tête non collant sous 820 px (décision, réversible) | B1, B2 | **fait le 24/10/2026** (local) : 0 entrée hors écran à 360/375 px, en-tête 106–108 px non collant |
| 5 | Guides : tableaux dans un conteneur défilant avec ombres de bord (10 pages) ; CTA unifiés par le lot 1b | B3, A4 | **fait le 24/10/2026** (local) : plus aucun défilement horizontal de page |
| 4 | Calculateur : cibles 44 px, H2 harmonisés, placeholders avec points de suspension ; ordre des titres de la page confidentialité ; conteneurs de tableau focalisables | B4, B5, C6 | **fait le 24/10/2026** (local) : axe 0 violation sur les 8 pages de référence |
| 6 | Copie (règle 3) : libellés courts « Ouvrir le calculateur » / « Télécharger l'application », kickers et H2 des sections application et guides de l'accueil, lede de la section application ; lede de la section guides conservée (choix de Julien) | D3, C5 | **fait le 24/10/2026** (local) : FR validé, 95 remplacements dans les 5 langues |

Ordre : 1a → 1b → 2 → 3 → 5 → 4 → 6.

---

## 4. Outillage `tools/` (créé le 24/10/2026)

| Fichier | Rôle |
|---|---|
| `package.json` | `npm run serve` / `shots` / `a11y` / `browsers` / `sitemap` ; dépendances : `playwright`, `pixelmatch`, `pngjs`, `@axe-core/playwright` |
| `lib/env.mjs` | Confine Playwright au dépôt : `PLAYWRIGHT_BROWSERS_PATH = tools/.browsers/` (règle 7) ; pages et largeurs par défaut |
| `serve.mjs` | Serveur statique `node:http` sans dépendance, racine = site, port 8123 (les liens sont en racine) |
| `shots.mjs` | Captures pleine page + mesures (défilement horizontal, boutons multilignes, menu hors écran, abscisse des titres, logo/contenu, texte < 12 px) → `tools/shots/<date>-<label>/` ; `--url=` pour la production ; `--compare A B` → planches avant / après / différence |
| `a11y.mjs` | axe-core par page, rapport par règle |
| `browsers.mjs` | Téléchargement de Chromium dans `tools/.browsers/` |

Mesures de précaution : GA4, AdSense et DoubleClick sont **bloqués** et le consentement est
pré-rempli « refusé » (`localStorage`) : aucune capture de production ne crée de session GA4 ni
d'impression publicitaire. Aucun fichier hors `C:\RTP-Calculator\`. Taille réelle de
`tools/.browsers/` : **719 Mo** (Chromium complet 437 Mo + headless shell 278 Mo, l'installeur
livre les deux), et non les 150 Mo annoncés dans le plan ; `node_modules` : 23 Mo. Tout est ignoré
par Git.
