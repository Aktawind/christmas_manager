# ADR 0003 — Modèle de données : éditions annuelles et historique figé

- Date : 2026-11-11
- Statut : proposé

## Décision

- Toutes les entités ont un identifiant **UUID texte** et des champs `createdAt` /
  `updatedAt`.
- `ChristmasEdition` (année unique, budget global, statut `ACTIVE` / `ARCHIVED`) est
  la racine des éléments annuels.
- Éléments **réutilisables** (sans année) : `Person`, `GiftIdea`, `Recipe`,
  `Tradition`, `Activity`, `Inspiration`, `AdventTemplate`.
- Éléments **annuels** (avec `editionId`) : `PlannedGift`, `AdventCalendar`
  (et ses `AdventBox`), `Production`, `Meal`, `Task`, `Expense`, `YearTradition`,
  `CreativeProject`, `JournalEntry`.
- **Snapshot** : à la création d'un élément annuel à partir d'un réutilisable, les
  champs affichés dans les archives (titre, description, prix, photo) sont copiés.
  Le lien vers l'original est conservé (`ideaId`, `recipeId`…) mais facultatif : si
  l'original est supprimé, l'archive reste intacte.
- Montants en centimes (`Long`).
- Les dépenses du budget sont une **vue calculée** sur les achats des modules
  (cadeaux, cases, fournitures, productions, repas) + des dépenses libres ; aucune
  double saisie.
- Les tâches ont des liens polymorphes : `TaskLink(taskId, targetType, targetId)`.
- Les photos sont des fichiers dans le stockage interne, référencés par `Media(id,
  relativePath, width, height)`.

## Conséquences

- Les archives d'un Noël passé ne bougent jamais, même si l'on modifie une recette.
- La duplication d'une année crée de nouveaux UUID et de nouveaux snapshots.
