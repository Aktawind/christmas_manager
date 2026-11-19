# ADR 0015 — Traditions et inspirations

- Date : 2026-11-19
- Statut : accepté

## Décision

- Nouvel onglet **Traditions** : « Au programme » · « Nos traditions » · « Inspirations »
  (retour utilisatrice : séparer le programme de l'année de la bibliothèque).
- **Tradition** (réutilisable) : nom, photo, catégorie (À la maison, Sorties, Cuisine, Films &
  lectures, En famille, Autre), période (Tout l'Avent, Réveillon, Jour de Noël, Après Noël),
  avec qui (personnes), notes, favorite. Tables `tradition`, `tradition_person`.
- **Programme d'un Noël** (`tradition_year`) : une tradition « au programme » cette année, puis
  « faite » avec une photo souvenir et un petit mot. Aucune date, aucun rappel (ADR 0006).
  On garde la trace « faite en 2025, 2026 ».
- **Idées à piocher** : une cinquantaine d'idées proposées (lutin farceur, sapin, lettre au Père Noël…), ajoutées
  seulement si l'utilisatrice les coche ; celles déjà au catalogue (même nom) ne sont plus proposées.
- **Inspirations** (`inspiration`) : films, dessins animés, livres, musiques, sorties, activités ;
  « fait cette année » est propre à chaque Noël (`inspiration_done`, choix de l'utilisatrice).
- Partage Android : « Ranger comme : Inspiration » (titre, lien, photo de la page, type).
- **Chemin vers Noël** : les dates qui comptent pour la famille (jour, mois, nom, emoji ; de
  novembre à début janvier) s'ajoutent chaque année au chemin de l'accueil (table `special_date`),
  réglées dans Réglages → Le chemin vers Noël (lien « Mes dates » sous le chemin).
- Base Room v16 (v15 : premier test), format de sauvegarde v10.
