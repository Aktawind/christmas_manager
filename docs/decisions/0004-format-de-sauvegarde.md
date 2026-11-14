# ADR 0004 — Format de sauvegarde, export et import

- Date : 2026-11-11
- Statut : accepté

## Décision

- Fichier `.zip` nommé `mon-noel-AAAA-MM-JJ-HHmm.zip` contenant :
  - `manifest.json` : `formatVersion`, `appVersion`, `schemaVersion`, `exportedAt`,
    nombre d'éléments par table, empreinte SHA-256 de `data.json`.
  - `data.json` : toutes les tables, sérialisées avec kotlinx.serialization dans des
    DTO dédiés (indépendants des entités Room).
  - `media/` : les photos (déjà redimensionnées).
- Export et import via le Storage Access Framework (Drive, dossier Téléchargements,
  carte SD… au choix de l'utilisateur), sans compte en ligne obligatoire.
- Import :
  1. vérification du manifeste et de l'empreinte ;
  2. sauvegarde de sécurité automatique des données actuelles ;
  3. remplacement complet dans une transaction ; en cas d'échec, rien n'est modifié.
- Sauvegarde automatique : tâche WorkManager quotidienne (et après des modifications
  importantes) vers un dossier choisi, conservation des 10 dernières.
- Compatibilité : `formatVersion` incrémenté à chaque changement ; l'import convertit
  les anciennes versions.
- La sauvegarde Android (Auto Backup) est désactivée pour la base afin d'éviter des
  restaurations partielles incohérentes ; l'export est la source de vérité.

## Conséquences

- Changement de téléphone : exporter (ou utiliser le dossier de sauvegarde auto sur
  Drive), installer l'app, importer.
- Un test automatisé vérifie l'aller-retour export → import sur un jeu de données complet.

## Historique du format

| formatVersion | Version de l'app | Changement |
| ------------- | ---------------- | ---------- |
| 1 | v0.1.0 | Années |
| 2 | v0.2.0 | Personnes, souhaits, catégories, idées (+ liens personnes), cadeaux, enveloppes, photos dans `media/photos/`. Conversion 1 → 2 : ajout des catégories par défaut. |
| 3 | v0.3.0 | Calendriers de l'Avent (+ personnes), cases, fournitures, modèles et leurs cases. Conversion 2 → 3 : rien à ajouter. |
