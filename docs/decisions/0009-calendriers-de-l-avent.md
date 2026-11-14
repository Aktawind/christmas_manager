# ADR 0009 — Calendriers de l'Avent

- Date : 2026-11-14
- Statut : accepté

## Décision

- `advent_calendar` (annuel) avec `box_count` (24 par défaut, 1 à 31) et personnes
  destinataires (`advent_calendar_person`). Les cases sont créées à la création ; augmenter le
  nombre ajoute des cases vides, le réduire supprime les dernières (avertissement si remplies).
- `advent_box` : numéro, type de surprise, contenu, photo, prix, fournisseur, lien, état
  (`TO_FIND` → `TO_PREPARE` → `READY`, décision utilisatrice), notes, lien facultatif vers un
  achat groupé. **Glisser-déposer = échange des numéros** de deux cases (le contenu change de
  place, les numéros restent fixes à l'écran).
- `advent_supply` : fournitures et achats groupés. Un achat relié à plusieurs cases voit son prix
  réparti à parts égales (`supplyShares`, reste au centime sur les premières cases).
- Budget du calendrier : prévu = cases + fournitures ; dépensé = cases trouvées (à préparer ou
  prêtes) + fournitures achetées. Affichage doux.
- Réutilisation : copie d'un calendrier vers un autre Noël (cases remises « À trouver »,
  fournitures à racheter, photos copiées) ; modèles personnels `advent_template` +
  `advent_template_box` (contenu sans photos ni avancement) ; « reprendre une case existante ».
- Accueil : avancement et « case du jour » du 1er décembre jusqu'à la dernière case.
- Base v4 (migration 3 → 4 testée), format de sauvegarde v3.
