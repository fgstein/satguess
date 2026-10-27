# SatGuess — Spécifications V1

Application Android inspirée d'EarthGuessr : on voit une image satellite d'un lieu, on le place sur une carte du monde, on marque des points selon la distance.

## 1. Principe
Une image satellite sans aucun label (pas de noms, frontières, routes étiquetées) est affichée. Le joueur ouvre une carte du monde, pose une épingle et valide. Plus il est proche, plus il marque de points.

## 2. Déroulement d'une partie
- 5 manches.
- Chaque manche : image satellite → épingle → validation → écran de résultat → manche suivante.
- Fin de partie : récapitulatif, score total sur 25 000.

## 3. Vue satellite — deux modes, choisis au lancement
- **Move** : pan + zoom autorisés, dans des limites (dézoom max ~50 km de large, zoom max ~200 m). Bouton « Retour au départ » qui recentre sur la vue initiale.
- **No Move** : image figée, ni zoom ni déplacement.
- Dans les deux cas : aucun label.

## 4. Saisie de la réponse
- Mini-carte du monde dépliable en bas de l'écran (ou plein écran).
- Un appui pose l'épingle, qu'on peut ensuite déplacer.
- Bouton « Valider » actif uniquement quand une épingle est posée.

## 5. Score
- Distance de grand cercle (haversine) entre l'épingle et le lieu.
- Points de la manche = `round(5000 × e^(−d / 2000 km))`.
- d < 1 km → 5000 points.

## 6. Écran de résultat de manche
- Carte avec l'épingle, le vrai lieu et une ligne entre les deux.
- Distance, points gagnés, nom du lieu (+ pays).

## 7. Écran de fin de partie
- Carte avec les 5 paires épingle/lieu, score total, détail par manche.
- Seed de la partie affichée et copiable.
- Meilleur score enregistré en local.
- Boutons « Rejouer », « Partager » (ex. `SatGuess 🛰️ 18 432/25 000 — seed K7QF-29XA`), « Rejouer cette seed ».

## 8. Lieux
- Liste de plusieurs milliers de lieux, générée automatiquement depuis Wikidata (voir `tools/locations/`), embarquée dans l'app en JSON.
- Tri automatique uniquement, sans relecture manuelle. Le but du jeu est de trouver le point affiché : il n'est pas grave que la coordonnée tombe un peu à côté de l'objet réel.
- Tirage totalement libre, sans quota par catégorie.
- Catégories (V1) :

| Code | Catégorie | Vue de départ (largeur) |
|---|---|---|
| A | Grandes villes | 15 km |
| B | Aéroports | 5 km |
| C | Bases militaires | 5 km |
| D | Ports et canaux | 8 km |
| E | Sites industriels / énergie (centrales, barrages, raffineries) | 5 km |
| F | Mines | 5 km |
| G | Bases de lancement spatial | 8 km |
| H | Fleuves : deltas et embouchures | 30 km |
| I | Volcans, montagnes, cratères | 15 km |
| J | Îles et atolls | 10 km |
| K | Sites du patrimoine mondial UNESCO | 3 km |
| L | Circuits automobiles et grands stades | 3 km |
| M | Paysages remarquables (canyons, fjords, lacs, glaciers, déserts de sel) | 20 km |

- Filtres automatiques :
  1. **Célébrité** : nombre minimal d'articles Wikipédia (sitelinks), avec un seuil par catégorie.
  2. **Doublons** : deux lieux à moins de 5 km → on garde le plus connu.
  3. **Diversité** : au plus 5 % des lieux dans un même pays.
  4. **Coordonnées** : on écarte les coordonnées absentes ou invalides.
- Format d'un lieu : `id` (QID Wikidata), `lat`, `lon`, `cat`, `name_fr`, `name_en`, `country` (ISO 3166-1 alpha-2), `sitelinks`, `view_km`.
- Le fichier porte un numéro de **version** (entier), incrémenté à chaque régénération.

## 9. Mode de jeu
Un seul mode : la partie classique de 5 lieux tirés dans toute la liste. Pas de défi du jour.

## 10. Chronomètre
Désactivé par défaut. Option : 60 s ou 120 s par manche. À l'expiration, on valide l'épingle posée, ou 0 point s'il n'y en a pas.

## 11. Imagerie
- Vue satellite : Mapbox Satellite (clé API) ; secours : Sentinel-2 cloudless (EOX, CC BY 4.0).
- Carte de réponse : OpenStreetMap via MapLibre.

## 12. Stack technique
- Kotlin + Jetpack Compose, minSdk 26.
- MapLibre Native Android pour les deux cartes.
- Pas de serveur. Scores et réglages stockés en local (DataStore/Room).
- Internet requis pour charger les tuiles.

## 13. Écrans
Accueil (Jouer, Jouer avec une seed, Stats, Réglages) → Jeu → Résultat de manche → Fin de partie. Stats : meilleur score, moyenne, nombre de parties. Réglages : chrono, unités km/mi.

## 14. Langues
Français, anglais.

## 15. Hors périmètre V1
Multijoueur, comptes, classements en ligne, publicités, défi du jour.

## 16. Seed
Une partie est entièrement déterminée par sa seed : deux joueurs avec la même seed ont exactement les mêmes 5 lieux, dans le même ordre, avec la même vue de départ et le même mode.

- Format affiché : 8 caractères en base32 Crockford (sans I, L, O, U), groupés `XXXX-XXXX`. Saisie insensible à la casse et aux tirets ; `O`→`0`, `I`/`L`→`1`.
- Contenu (40 bits) :
  - 1 bit : mode (0 = Move, 1 = No Move)
  - 7 bits : version de la liste de lieux (0–127)
  - 32 bits : valeur aléatoire
- Tirage : générateur SplitMix64 (implémenté dans l'app, indépendant de la plateforme) initialisé avec les 32 bits aléatoires ; mélange de Fisher-Yates partiel sur la liste triée par `id` ; les 5 premiers éléments sont retenus.
- Une seed dont la version ne correspond pas à la liste embarquée affiche un message clair (« seed créée avec une autre version du jeu »).
- La seed est visible pendant la partie et en fin de partie. Elle est copiable et partageable. L'accueil propose « Jouer avec une seed ».
