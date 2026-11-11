# ADR 0007 — Photos, cadeaux et historique figé

- Date : 2026-11-11
- Statut : accepté

## Décision

- **Photos** : importées depuis la galerie (sélecteur photo Android) ou l'appareil photo
  (fichier temporaire via `FileProvider`), tournées selon l'EXIF, réduites à 1600 px max,
  JPEG qualité 85, rangées dans `files/media/photos/<uuid>.jpg`. Les entités stockent le
  chemin relatif. Aucune permission Android n'est nécessaire.
- **Une photo n'appartient qu'à un seul élément** : quand une idée devient un cadeau, sa
  photo est **copiée**. Supprimer ou remplacer la photo d'un élément supprime le fichier sans
  risque pour les autres. Les photos ajoutées dans un formulaire abandonné sont supprimées.
- **Cadeau prévu** : copie du titre, de la description, du prix, de la boutique, du lien, de la
  catégorie et du nom du destinataire au moment de la création. Les liens vers l'idée et la
  personne sont conservés mais facultatifs (`SET NULL` à la suppression).
- **Dépensé** = cadeaux au statut Commandé ou au-delà. **Prévu** = somme des prix.
- **Enveloppe** : table `person_budget` (Noël × personne). Affichage doux, jamais en rouge.
- **Catégories** : table `gift_category`, 11 catégories par défaut (identifiants fixes
  `cat-…`), modifiables dans les Réglages ; suppression → idées/cadeaux « sans catégorie ».
- **Statut** : choisi dans un menu déroulant (décision utilisateur), avec le raccourci
  « Acheté en magasin » → À emballer.
