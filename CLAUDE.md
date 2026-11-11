# CLAUDE.md — Mémoire du projet « Carnet de Noël »

Ce fichier est relu au début de chaque session. Il contient les conventions et les
décisions qui ne doivent pas être redemandées. Les détails de chaque décision sont
dans `docs/decisions/`, la liste des fonctionnalités dans `docs/ROADMAP.md`.

## Le projet en une phrase

Application Android native, hors connexion, qui sert de **carnet de Noël numérique** :
idées, cadeaux, calendriers de l'Avent, recettes, repas, planning, budget, traditions,
activités, décoration et souvenirs, organisés **par Noël annuel** et conservés année
après année. Ambiance chaleureuse et féérique, jamais « outil de gestion de projet ».

## Utilisatrice / utilisateur cible et appareil de test

- Appareil principal de test : **tablette Android** (paysage et portrait).
- L'interface doit aussi fonctionner sur téléphone (mises en page adaptatives).
- Langue de l'interface : **français** uniquement pour l'instant (chaînes dans
  `strings.xml`, jamais en dur, pour permettre d'autres langues plus tard).

## Langues

- Code, noms de classes, de tables et de colonnes : **anglais**.
- Interface, documentation (`docs/`), notes de version, checklists de test,
  messages de commit : **français**.

## Workflow Git et versions (voir ADR 0001)

- `main` : uniquement des versions validées par l'utilisateur, chacune taguée `vX.Y.Z`.
- `develop` : branche d'intégration ; chaque push déclenche vérifications, tests et
  publication de l'APK de test en pré-version GitHub `develop-latest`.
- Le travail d'une session peut se faire sur une branche de travail puis être
  fusionné dans `develop`.
- Une version = un jalon GitHub (`v0.1.0`, …) + une checklist de test
  `docs/test-checklists/vX.Y.Z.md` + des notes de version `docs/release-notes/vX.Y.Z.md`.
- Validation : l'utilisateur teste l'APK `develop-latest`, fait ses retours ; une fois
  validé → merge `develop` → `main`, tag `vX.Y.Z` → la CI publie l'APK signé dans
  les Releases avec les notes de version.
- Versionnage SemVer `0.x.y` jusqu'à la v1.0.0. `versionCode` = calculé depuis la
  version (`major*10000 + minor*100 + patch`) ; les builds develop utilisent le
  numéro de run CI.
- Commits : messages en français, à l'impératif, préfixés par le module
  (`cadeaux: ajoute le changement rapide de statut`).

## Signature des APK

- Une clé unique de release, générée au démarrage du projet, **jamais commitée**.
- Secrets GitHub : `SIGNING_KEYSTORE_BASE64`, `SIGNING_KEYSTORE_PASSWORD`,
  `SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD`.
- `*.jks`, `*.keystore`, `keystore.properties` sont dans `.gitignore`.

## Variantes de l'application

- `release` : `applicationId` définitif, nom « Carnet de Noël ».
- `beta` (APK `develop-latest`) : suffixe `.beta`, nom « Carnet de Noël β », signé avec
  la même clé. Les deux s'installent côte à côte ; les données passent de l'une à
  l'autre par export / import.

## Architecture (voir ADR 0002)

- Kotlin, Jetpack Compose (Material 3), MVVM + Clean Architecture, Hilt, Room,
  Navigation Compose, WorkManager, DataStore, Coil, kotlinx.serialization.
- Projet Gradle multi-modules avec convention plugins dans `build-logic/` :
  - `:app` — point d'entrée, navigation globale, injection.
  - `:core:model` — modèles métier purs (pas de dépendance Android).
  - `:core:common` — utilitaires, dispatchers, gestion d'erreurs (`AppResult`).
  - `:core:designsystem` — thème, palette, typographie, composants féériques.
  - `:core:ui` — composants d'écran partagés (états vides, cartes, photo picker…).
  - `:core:database` — Room : une seule base, entités, DAO, migrations.
  - `:core:data` — repositories (interfaces dans `:core:domain`, implémentations ici).
  - `:core:domain` — cas d'usage transverses (années, budget, recherche, tâches auto).
  - `:core:media` — import, redimensionnement et stockage des photos.
  - `:core:backup` — export / import / sauvegarde automatique.
  - `:feature:<module>` — un module par domaine (home, years, gifts, advent, recipes,
    meals, planning, budget, traditions, activities, decoration, memories, search,
    settings).
- Un module `feature` ne dépend jamais d'un autre module `feature`.
- Les ViewModels exposent un `StateFlow<UiState>` unique ; les écrans sont des
  fonctions `@Composable` sans état métier.

## Règles de données (voir ADR 0003)

- Identifiants : **UUID texte** pour toutes les entités (fusion/import sans collision).
- Notion centrale : `ChristmasEdition` (le Noël d'une année). Les éléments annuels
  (cadeaux, calendriers, productions, repas, tâches, dépenses, journal…) référencent
  une édition. Les éléments réutilisables (personnes, idées, recettes, traditions,
  activités, inspirations) n'en dépendent pas.
- **Historique figé** : un élément annuel copie (snapshot) les champs clés de
  l'élément réutilisable au moment de sa création (titre, prix, photo…). Modifier une
  recette ou une idée ne change jamais les archives.
- Montants en **centimes** (`Long`), devise EUR. Dates en `LocalDate` / `Instant`
  stockés en ISO‑8601 ou epoch.
- Le budget est **calculé** à partir des achats des autres modules : aucune double saisie.
- Une année archivée est en lecture seule (sauf journal/souvenirs).
- Toute évolution du schéma Room = une migration écrite **et testée**
  (`MigrationTestHelper`) ; jamais de `fallbackToDestructiveMigration` en release.

## Sauvegarde (voir ADR 0004)

- Format : archive `.zip` = `manifest.json` (version du format, version de l'app,
  date) + `data.json` (toutes les tables) + `media/` (photos).
- Import = remplacement complet, après confirmation et **sauvegarde de sécurité
  automatique** des données actuelles.
- Sauvegarde automatique périodique (WorkManager) dans un dossier choisi par
  l'utilisateur (Storage Access Framework), rotation des N dernières sauvegardes.
- Le format de sauvegarde est versionné ; l'import sait lire toutes les versions
  antérieures.

## Identité graphique

| Couleur            | Rôle Material 3                 | Clair     |
| ------------------ | ------------------------------- | --------- |
| Vert sapin profond | primary                         | `#1F4D3A` |
| Vert sauge         | secondary / surfaces secondaires| `#8FA98F` |
| Ivoire / crème     | background / surface            | `#FBF6EC` |
| Or champagne       | tertiary (accents magiques)     | `#C9A75C` |
| Bordeaux           | mise en valeur / error-like     | `#7A1F2B` |
| Brun noisette      | textes secondaires              | `#7B5B43` |

- Thème sombre dérivé (fond vert nuit, textes crème) ; respect du réglage système,
  avec choix manuel dans les réglages. Pas de couleurs dynamiques Material You par
  défaut (l'identité prime), option possible.
- Accessibilité : contrastes AA, tailles de police système, `contentDescription`
  sur les images, cibles tactiles ≥ 48 dp.

## Qualité et tests

- Avant chaque push : `./gradlew lint ktlintCheck testDebugUnitTest assembleBeta`.
- Tests unitaires obligatoires pour : cas d'usage, calculs de budget, export/import
  (aller-retour complet), migrations Room.
- Chaque version livre `docs/test-checklists/vX.Y.Z.md` : quoi tester, pas à pas,
  sur tablette.

## Rituel de début de session

1. Relire ce fichier, `docs/ROADMAP.md` et les ADR récents.
2. Vérifier le jalon GitHub de la version en cours et les retours de l'utilisateur.
3. Mettre à jour le statut dans `docs/ROADMAP.md` à la fin du travail.
