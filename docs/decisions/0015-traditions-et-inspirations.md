# ADR 0015 — Traditions et inspirations

- Date : 2026-11-19
- Statut : accepté

## Décision

- Nouvel onglet **Traditions** (« Nos traditions » · « Inspirations »).
- **Tradition** (réutilisable) : nom, photo, catégorie (À la maison, Sorties, Cuisine, Films &
  lectures, En famille, Autre), période (Tout l'Avent, Réveillon, Jour de Noël, Après Noël),
  avec qui (personnes), notes, favorite. Tables `tradition`, `tradition_person`.
- **Programme d'un Noël** (`tradition_year`) : une tradition « au programme » cette année, puis
  « faite » avec une photo souvenir et un petit mot. Aucune date, aucun rappel (ADR 0006).
  On garde la trace « faite en 2025, 2026 ».
- **Idées à piocher** : 22 idées proposées (lutin farceur, sapin, lettre au Père Noël…), ajoutées
  seulement si l'utilisatrice les coche ; celles déjà au catalogue (même nom) ne sont plus proposées.
- **Inspirations** (`inspiration`) : films, dessins animés, livres, musiques, sorties, activités ;
  « fait cette année » est propre à chaque Noël (`inspiration_done`, choix de l'utilisatrice).
- Partage Android : « Ranger comme : Inspiration » (titre, lien, photo de la page, type).
- Base Room v15, format de sauvegarde v10.
