# ADR 0012 — Repas de fête et menus

- Date : 2026-11-15
- Statut : accepté

## Contexte

Après les recettes sans détail (ADR 0011), l'utilisatrice veut organiser les repas de fête :
qui vient, où, quand, et ce qu'on mange, toujours sans ingrédients ni étapes.

## Décision

- **Repas** (lié à une année) : type (Réveillon du 24, Déjeuner du 25, Brunch, Goûter,
  Repas entre amis, Autre), nom, date facultative (date naturelle), lieu, notes et
  **coût estimé facultatif par repas** (choix de l'utilisatrice : pas de prix par plat).
- **Convives** : personnes déjà présentes dans l'application (table `meal_person`) **et**
  invités en texte libre (une ligne par invité), sans créer de fiche.
- **Plats** rangés par moment : apéritif, entrée, plat, fromage, dessert, boissons.
  Un plat est une recette de la bibliothèque (nom et photo copiés, historique figé) ou un
  nom libre ; « qui apporte ? » en texte libre.
- **À commander** : un plat peut être marqué « à commander » ; le lutin propose alors
  « Commander … », qui se coche quand le plat est marqué « commandé ». Un repas sans plat
  donne la suggestion « Préparer le menu ». Rien n'est ajouté automatiquement (ADR 0010).
- Choisir le type propose un nom et, pour le 24 et le 25, la date ; les deux restent modifiables.
- Onglet Cuisine : **Repas** · Gourmandises (les préparations de la v0.5) · Mes recettes.
- Base Room v8, format de sauvegarde v7.

## Conséquences

- Le coût estimé des repas sera repris par le budget calculé de la v0.7, sans double saisie.
- Supprimer une personne la retire des convives ; supprimer une recette laisse le plat intact.
