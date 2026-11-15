# ADR 0011 — Cuisine : recettes sans détail et préparations de l'année

- Date : 2026-11-15
- Statut : accepté

## Contexte

L'utilisatrice a déjà une application de recettes. Elle ne veut **aucun** ingrédient,
étape ou liste de courses dans Mon Noël : « juste un nom et une photo ». Les préparations
n'ont pas de lien avec les cadeaux.

## Décision

- **Recette** (réutilisable, hors année) : nom, photo, catégorie, lien facultatif vers la
  vraie recette (site ou autre application). Rien d'autre.
- **Catégories de recettes** : 10 catégories initiales (bûches, sablés, mendiants, chocolats,
  truffes, petits gâteaux, confiseries, desserts, boissons, apéritifs), modifiables dans
  Réglages → Catégories de recettes.
- **Préparation** (« production », liée à une année) : recette choisie ou nom libre,
  quantité, « pour qui / pour quoi » (texte libre avec raccourcis), avancement
  À faire → En cours → Faite, photo du résultat, notes. Aucune date.
- Historique figé (ADR 0003) : la préparation copie le nom et la photo de la recette ;
  supprimer ou modifier la recette ne change pas les années passées.
- Le lutin suggère « Préparer … » pour chaque préparation pas encore faite ; la tâche se
  coche seule quand la préparation passe à « Faite ».
- Partage Android : le partage d'une page peut être rangé comme **recette** (nom, photo de la
  page, lien) au lieu d'une idée cadeau.
- Onglet **Cuisine** (sous-onglets « Pour ce Noël » et « Mes recettes ») ; les repas de la
  v0.6 y prendront place.
- Base Room v7, format de sauvegarde v6.

## Conséquences

- Pas de liste de courses générée : si besoin, une tâche libre dans « Ma liste ».
- Les repas (v0.6) pourront référencer une recette de la bibliothèque, toujours sans détail.
