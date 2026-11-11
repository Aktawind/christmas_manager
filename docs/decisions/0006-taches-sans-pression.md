# ADR 0006 — Des tâches sans dates ni priorités

- Date : 2026-11-11
- Statut : accepté

## Contexte

Le cahier des charges prévoyait un planning (vues mensuelle et hebdomadaire, filtres
par priorité). L'utilisateur ne veut ni dates ni priorités : Noël doit rester un
plaisir, sans pression.

## Décision

- Les vues calendrier mensuelle / hebdomadaire et le filtre par priorité sont
  **supprimés**.
- Le module devient **« Ma liste de lutin »** : des tâches avec titre, description,
  statut (À faire, En cours, Terminée, Annulée), module associé, éléments liés et notes.
  Regroupement par module, filtre par statut, cases à cocher.
- Les automatisations restent : elles **proposent** des tâches (« Vérifier la
  livraison du cadeau de Léa ») que l'utilisateur accepte, modifie ou ignore.
- L'accueil remplace « prochaines échéances » par **« Le chemin vers Noël »** : les
  rares repères qui ont une date par nature (ouverture des calendriers le 1er
  décembre, repas prévus, réveillon, Noël), présentés comme des étapes douces, jamais
  comme des retards.
- Le module garde son identifiant technique `:feature:tasks`.
