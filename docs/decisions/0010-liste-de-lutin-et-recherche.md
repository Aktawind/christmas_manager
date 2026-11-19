# ADR 0010 — Ma liste de lutin, suggestions et recherche

- Date : 2026-11-15
- Statut : accepté

## Décision

- Table `lutin_task` (annuelle) : titre, description, état (TODO, IN_PROGRESS, DONE, CANCELLED),
  thème (`TaskModule`), notes, élément lié facultatif (`LinkType` + id), `suggestion_key`.
  Aucune date ni priorité (ADR 0006).
- **Suggestions** calculées à la volée (`currentSuggestions`) depuis les cadeaux et les
  calendriers du Noël affiché : livraison à vérifier (cadeau commandé), cadeau à emballer,
  fourniture à acheter, surprises à trouver, cases à préparer. Elles sont **proposées** :
  l'utilisatrice les ajoute (tâche avec `suggestion_key`) ou les supprime (table
  `dismissed_suggestion`, elles ne reviennent pas). Désactivables par type (DataStore).
- **Coche automatique** : `LutinAutoSync` (démarré par l'application) observe l'état des
  suggestions et coche / décoche les tâches correspondantes. Les tâches annulées ne bougent pas.
- **Recherche globale** (`searchAll`) : insensible aux accents et à la casse, tous les mots
  doivent correspondre ; résultats regroupés par type ; cases identiques d'un calendrier
  regroupées. Accessible par une loupe dans le menu (décision utilisatrice).
- Accueil : « Le chemin vers Noël » (`christmasPath`) — étapes datées par nature uniquement.
- Base v6 (migration 5 → 6 testée), sauvegarde v5.

## Révision (2026-11-18, retour utilisatrice)

- La liste devenait longue : **Ma liste** est découpée en **onglets par thème** (Cadeaux,
  Calendriers de l'Avent, Cuisine, Repas de fête, Maison & déco, Autre). Chaque onglet montre ses
  suggestions et ses choses à faire ; son compteur additionne tâches ouvertes et suggestions.
  L'onglet affiché au départ est le premier qui a quelque chose à faire. Une nouvelle chose à
  faire prend le thème de l'onglet.

## Révision (2026-11-19, retour utilisatrice) — Ma liste supprimée

- L'onglet **Ma liste** est supprimé : chaque onglet (cadeaux, calendriers, cuisine, traditions)
  montre déjà ce qu'il reste à faire. Les réglages « Suggestions du lutin » disparaissent aussi.
- La **recherche** n'est plus un onglet : une loupe en haut à droite de **Mes Noëls** l'ouvre.
- Les demandes d'idées gardent leur état « fait » dans une tâche interne (non affichée) ; elles se
  consultent dans le nouvel onglet **Cadeaux → Listes** et dans les fiches des personnes.
