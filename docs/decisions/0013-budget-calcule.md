# ADR 0013 — Budget calculé et autres dépenses

- Date : 2026-11-16
- Statut : accepté

## Contexte

Le cahier des charges veut un budget complet, mais sans double saisie (CLAUDE.md) et sans
pression (ADR 0006). L'utilisatrice veut un onglet Budget, des « autres dépenses » (sapin…)
et que les repas comptent comme dépensés une fois leur date passée.

## Décision

- **Onglet Budget** (« Les comptes ») dans le menu.
- Le budget d'un Noël est **calculé** (`computeYearBudget`) à partir des modules :
  - Cadeaux : prévu = prix ; dépensé = commandés ou au-delà ; budget = somme des enveloppes.
  - Calendriers : cases + fournitures (comme sur l'écran du calendrier) ; budget = budgets des calendriers.
  - Repas : coût estimé ; dépensé à partir du jour du repas.
  - Autres dépenses : nouvelle table `expense` (titre, montant, catégorie, payé, notes) ;
    dépensé quand c'est coché « payé ».
- **Budget global** facultatif par année (`plannedBudgetCents` de l'édition, déjà présent) :
  marge ou dépassement affichés en douceur (texte bordeaux discret), jamais d'alerte.
- **Graphiques** : barre empilée de la répartition du prévu, avec une légende chiffrée (vue
  tableau) ; colonnes du coût prévu par année dès deux Noëls. Couleurs des postes vérifiées
  par le validateur de palette (daltonisme, contraste) sur le fond ivoire et sur le fond nuit.
- Base Room v10, format de sauvegarde v8.

## Conséquences

- Les budgets par catégorie de cadeaux du cahier des charges ne sont pas repris : enveloppes
  par personne et budgets de calendrier suffisent pour l'instant.

## Révision (v0.9.0, retour utilisatrice)

« Prévu » affichait le coût total noté (déjà payé compris) et faisait doublon avec « Dépensé ».
Désormais, partout (Les comptes, accueil, en-tête Cadeaux) :

- **Prévu** = ce qu'on a prévu de dépenser : le budget global s'il existe, sinon la somme des
  enveloppes et des budgets de calendrier. Sans aucun budget, la case n'apparaît pas.
- **Dépensé** = le réel (inchangé). Les barres comparent dépensé / prévu ; un dépassement ne
  change que la couleur.
- Un poste sans budget n'affiche que son dépensé. Le coût total des cadeaux reste visible dans
  l'onglet Cadeaux (« Total des cadeaux », « 42,90 € de cadeaux · prévu 60,00 € »).
- Le graphique des années montre le dépensé de chaque Noël.

