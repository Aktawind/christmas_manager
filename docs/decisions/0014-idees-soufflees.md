# ADR 0014 — Idées soufflées

- Date : 2026-11-17
- Statut : accepté

## Contexte

L'utilisatrice donne des idées de cadeaux à des proches, pour elle **et pour d'autres**
(ses parents reçoivent des idées pour elle et pour Yannick, Christine aussi…). Il faut
savoir, pour chaque personne et chaque Noël, quelles idées ont été soufflées et à qui,
pour ne pas donner la même idée deux fois. Une liste « pour moi » isolée ne convient pas.

## Décision

- **Idée soufflée** (table `whispered_idea`, liée à un Noël) : pour qui (une personne de
  l'application, l'utilisatrice comprise, sans statut particulier), l'idée, de la part de qui (une personne de
  l'application ou un nom libre, dont le nom est toujours gardé), prix et lien facultatifs,
  « reçue », notes.
- Gérée **uniquement dans la fiche de la personne à qui l'idée est destinée** : « Idées
  soufflées pour X · Noël AAAA », chacune « de la part de … ». Rien n'apparaît dans la fiche de
  la personne qui offrira (retour utilisatrice : doublon, trop compliqué).
- **Anti-doublon** : même idée (accents et majuscules ignorés), même personne, même Noël, mais
  de la part de quelqu'un d'autre → « Aussi de la part de … » en bordeaux discret, et un
  avertissement dans la fenêtre de saisie (sans bloquer).
- Hors budget (ce ne sont pas nos dépenses). Recherche : groupe « Idées soufflées ».
- Base Room v11, format de sauvegarde v9.
