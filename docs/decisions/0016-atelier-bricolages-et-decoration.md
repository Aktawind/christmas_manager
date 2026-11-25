# ADR 0016 — L'atelier : bricolages et décoration

- Date : 2026-11-22
- Statut : accepté

## Contexte

La v0.9.0 prévoyait des « activités & DIY » et de la décoration. L'utilisatrice préfère tout
garder dans l'onglet Traditions, compter les fournitures dans un nouveau poste du budget, et
s'en tenir à un simple tableau d'inspiration pour la décoration.

## Décision

- Traditions passe à **cinq onglets** (défilants) : Au programme · Nos traditions ·
  Inspirations · **Bricolages** · **Décoration**. Pas de nouvel onglet dans le menu.
- **Bricolage** (réutilisable, table `craft`) : titre, photo, catégorie (Décoration, Cadeau
  fait main, Emballage, Cartes, Avec les enfants, Autre), lien du tutoriel, notes.
  **Fournitures** (`craft_supply`) : nom, prix facultatif, ordre.
- **Avancement d'un Noël** (`craft_year`, une ligne par Noël et bricolage) : Au programme,
  En cours, Terminé (+ photo du résultat). Sans ligne = simple idée. Aucune date (ADR 0006).
  « Fait en 2025, 2026 » = années terminées.
- **Fournitures achetées** propres à chaque Noël (`craft_supply_bought`), comme
  « fait cette année » des inspirations : la liste de fournitures se réutilise l'année suivante.
- **Budget** (ADR 0013) : nouveau poste **Atelier** = prix des fournitures des bricolages au
  programme de l'année (quel que soit l'avancement) ; dépensé = fournitures cochées. Couleur
  violette (#8A5AB5 clair, #9C7BD0 sombre), vérifiée avec le reste de la palette.
- **Décoration** (`decor_idea`) : un tableau d'inspiration en grille — photo, titre facultatif,
  lien, commentaire, endroit (Sapin, Table de fête, Entrée, Fenêtres, Extérieur, Autre), ❤️ favorite.
  Pas de lien avec un Noël. Partage Android : « Ranger comme : Déco ».
- **Dépassements de budget** (retour utilisatrice) : plus aucune note sous les cartes (elles
  gardent la même hauteur) ; la barre passe simplement d'or à un orange un peu plus foncé
  (`overBudget`), partout où il y a un budget.
- **Statut « Reçu » retiré** (retour utilisatrice : identique à « À emballer ») : un cadeau
  commandé passe directement « À emballer ». Les cadeaux « Reçu » deviennent « À emballer »
  (migration Room 17 → 18, et à l'import des anciennes sauvegardes).
- **Dates de l'année** (retour utilisatrice) : une tradition au programme et une inspiration
  peuvent avoir une date facultative pour ce Noël (`tradition_year.date`, table
  `inspiration_date`). Ce ne sont pas des échéances : aucun rappel, rien en rouge (ADR 0006).
  Le programme range les traditions à faire par date, puis les faites en bas, barrées.
- **Dupliquer** une tradition, une inspiration, un bricolage (avec ses fournitures) ou une idée
  déco : copie « (copie) » avec sa photo, sans l'historique de l'année.
- Base Room v19, format de sauvegarde v11.
