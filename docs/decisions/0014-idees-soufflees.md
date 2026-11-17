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
  l'application, éventuellement « Moi »), l'idée, à qui on l'a soufflée (une personne de
  l'application ou un nom libre, dont le nom est toujours gardé), prix et lien facultatifs,
  « reçue », notes.
- Gérée **dans la fiche de la personne** :
  - « Idées soufflées pour X · Noël AAAA » (et à qui) ;
  - « Ce que j'ai soufflé à X · Noël AAAA » (pour qui).
- **Anti-doublon** : même idée (accents et majuscules ignorés), même personne, même Noël, mais
  soufflée à quelqu'un d'autre → « Aussi soufflée à … » en bordeaux discret, et un
  avertissement dans la fenêtre de saisie (sans bloquer).
- Hors budget (ce ne sont pas nos dépenses). Recherche : groupe « Idées soufflées ».
- Base Room v11, format de sauvegarde v9.
