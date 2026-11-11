# Roadmap — Mon Noël

Légende des statuts : ⬜ À faire · 🚧 En cours · 🧪 À tester · ✅ Validé · ⏸️ Reporté

> **Contrainte calendaire.** Nous sommes le 11 novembre 2026. Pour que l'application
> serve dès ce Noël, l'ordre ci-dessous fait passer en premier ce qui est utile
> **avant le 1er décembre** : sauvegarde, cadeaux, puis calendriers de l'Avent.
> Le cahier des charges place « Recherche » au lot 1 : elle arrive en v0.4, quand il
> y a assez de contenu à chercher, puis s'étend à chaque nouveau module.

## Vue d'ensemble des versions

| Version | Nom                         | Contenu principal                                                     | Statut |
| ------- | --------------------------- | --------------------------------------------------------------------- | ------ |
| v0.1.0  | Les fondations              | Socle technique, CI/APK, thème, navigation, années, accueil, export/import, sauvegarde auto | ✅ |
| v0.2.0  | La hotte                    | Personnes, idées, cadeaux prévus, statuts rapides, photos, ajout rapide, budget cadeaux | ⬜ |
| v0.3.0  | Les calendriers de l'Avent  | Calendriers, 24 cases en grille, glisser-déposer, fournitures, modèles, duplication | ⬜ |
| v0.4.0  | Ma liste de lutin           | Tâches sans dates ni priorités, automatisations, accueil complet, recherche globale | ⬜ |
| v0.5.0  | Le fournil                  | Bibliothèque de recettes, productions pâtissières                     | ⬜ |
| v0.6.0  | La table de fête            | Repas, menus par catégories, liens recettes                           | ⬜ |
| v0.7.0  | Les comptes du Père Noël    | Budget complet : catégories, dépassements, graphiques, comparaison    | ⬜ |
| v0.8.0  | Les traditions              | Catalogue de traditions, suivi annuel, listes d'inspiration           | ⬜ |
| v0.9.0  | L'atelier des lutins        | Activités & DIY, projets créatifs, décoration                         | ⬜ |
| v0.10.0 | La boîte à souvenirs        | Journal, archives, bilans annuels, duplication d'une année, export PDF | ⬜ |
| v1.0.0  | Noël magique                | Finitions, animations, accessibilité, performances, robustesse        | ⬜ |

Chaque version : un jalon GitHub, une checklist `docs/test-checklists/vX.Y.Z.md`,
des notes de version `docs/release-notes/vX.Y.Z.md`.

---

## v0.1.0 — Les fondations

| ID    | Fonctionnalité                                                                 | Statut |
| ----- | ------------------------------------------------------------------------------ | ------ |
| F-001 | Projet Android multi-modules (Kotlin, Compose, Hilt, Room, convention plugins) | ✅ |
| F-002 | CI : vérifications + tests à chaque push sur `develop`, APK de test signé       | ✅ |
| F-003 | CI : APK signé publié dans les Releases à chaque tag `v*`, avec notes de version | ✅ |
| F-004 | Création automatique des jalons GitHub                                          | ✅ |
| F-005 | Identité graphique « Mon Noël » : icône, palette, typographie, thème clair/sombre/système | ✅ |
| F-006 | Navigation adaptative (rail latéral sur tablette, barre du bas sur téléphone)   | ✅ |
| F-007 | Premier lancement : accueil de bienvenue, création du Noël 2026                 | ✅ |
| F-008 | Mes Noëls : créer, choisir l'année affichée, archiver / désarchiver, supprimer  | ✅ |
| F-009 | Accueil v1 : année, compte à rebours, message du jour, aperçu des modules à venir | ✅ |
| F-010 | Export complet des données (.zip) vers un emplacement choisi                    | ✅ |
| F-011 | Import d'une sauvegarde (vérification + sauvegarde de sécurité préalable)       | ✅ |
| F-012 | Sauvegarde automatique quotidienne dans un dossier choisi, 10 dernières gardées | ✅ |
| F-013 | Gestion centralisée des erreurs (messages clairs, état de la dernière sauvegarde) | ✅ |
| F-014 | Réglages : thème, sauvegarde, à propos (version)                                | ✅ |

## v0.2.0 — La hotte 🎁

| ID    | Fonctionnalité                                                                    | Statut |
| ----- | --------------------------------------------------------------------------------- | ------ |
| G-001 | Fiches personnes : nom, photo, goûts, liste de souhaits, notes                    | ⬜ |
| G-002 | Historique des cadeaux reçus par personne (toutes années)                         | ⬜ |
| G-003 | Catalogue d'idées indépendant des années (titre, description, photo, prix, lien, boutique, notes, catégorie) | ⬜ |
| G-004 | Une idée liée à plusieurs personnes / plusieurs années                            | ⬜ |
| G-005 | Cadeaux prévus : instance d'une idée pour une personne et une année (snapshot)    | ⬜ |
| G-006 | Statuts À acheter → Commandé → Reçu → À emballer → Emballé → Offert, changement en un geste | ⬜ |
| G-007 | Vues : par destinataire, par statut, par catégorie, achetés, à emballer, non reçus | ⬜ |
| G-008 | Budget cadeaux : total et par personne (prévu / dépensé)                           | ⬜ |
| G-009 | Photos : galerie, appareil photo, redimensionnement, photo principale             | ⬜ |
| G-010 | Ajout rapide d'une idée depuis l'accueil (en quelques secondes)                    | ⬜ |
| G-011 | Partage Android → « Ajouter comme idée » (lien d'une boutique en ligne)            | ⬜ |

## v0.3.0 — Les calendriers de l'Avent

| ID    | Fonctionnalité                                                                 | Statut |
| ----- | ------------------------------------------------------------------------------ | ------ |
| A-001 | Calendriers multiples : nom, destinataire, description, couverture, thème, budget, notes | ⬜ |
| A-002 | Grille des 24 cases avec état visuel de préparation                            | ⬜ |
| A-003 | Fiche case : type de surprise, photo, prix, fournisseur, lien, statut, notes   | ⬜ |
| A-004 | Réorganisation des cases par glisser-déposer                                    | ⬜ |
| A-005 | Filtre « cases à préparer »                                                     | ⬜ |
| A-006 | Fournitures nécessaires par calendrier                                          | ⬜ |
| A-007 | Un achat partagé entre plusieurs cases (prix réparti)                           | ⬜ |
| A-008 | Suivi de réalisation et budget par calendrier                                   | ⬜ |
| A-009 | Dupliquer un calendrier d'une année à l'autre                                   | ⬜ |
| A-010 | Modèles personnels : enregistrer / créer depuis un modèle, réutiliser des cases | ⬜ |

## v0.4.0 — Ma liste de lutin 📝

Pas de dates ni de priorités (ADR 0006).

| ID    | Fonctionnalité                                                                    | Statut |
| ----- | --------------------------------------------------------------------------------- | ------ |
| P-001 | Tâches : titre, description, statut, module, éléments liés, notes                 | ⬜ |
| P-002 | Liste regroupée par module, cases à cocher, filtre par statut                     | ⬜ |
| P-003 | Automatisations : cadeau commandé → vérifier la livraison ; case non prête → préparer | ⬜ |
| P-004 | Les automatisations proposent, l'utilisateur accepte / modifie / ignore ; activables une à une | ⬜ |
| P-005 | Accueil complet : avancement des préparatifs, « Le chemin vers Noël », tâches à faire | ⬜ |
| P-006 | Recherche globale v1 : personnes, idées, cadeaux, calendriers, tâches, résultats groupés | ⬜ |
| ~~—~~ | ~~Vues mensuelle / hebdomadaire, priorités, rappels~~ — supprimés (ADR 0006)       | — |

## v0.5.0 — Le fournil 🧁

| ID    | Fonctionnalité                                                                 | Statut |
| ----- | ------------------------------------------------------------------------------ | ------ |
| R-001 | Bibliothèque de recettes : nom, photo, description, catégorie, notes (ingrédients et étapes en texte libre) | ⬜ |
| R-002 | Catégories initiales (bûches, sablés, mendiants…) + catégories personnalisées  | ⬜ |
| R-003 | Productions : recette, année, quantité, destination, statut, photo, notes      | ⬜ |
| R-004 | Production planifiée → tâche de fabrication automatique                        | ⬜ |
| R-005 | Recherche étendue aux recettes et productions                                   | ⬜ |

## v0.6.0 — La table de fête 🍽️

| ID    | Fonctionnalité                                                                 | Statut |
| ----- | ------------------------------------------------------------------------------ | ------ |
| M-001 | Repas : type (réveillon, déjeuner du 25, brunch…), date, lieu, convives        | ⬜ |
| M-002 | Menu par catégories (apéritif → boissons)                                      | ⬜ |
| M-003 | Plat lié à une recette existante ou saisi librement                            | ⬜ |
| M-004 | Repas créé → propositions de tâches de préparation                              | ⬜ |
| M-005 | Budget par repas                                                                | ⬜ |

## v0.7.0 — Les comptes du Père Noël 💰

| ID    | Fonctionnalité                                                                 | Statut |
| ----- | ------------------------------------------------------------------------------ | ------ |
| B-001 | Budget global annuel et budgets par catégorie / personne / calendrier / repas  | ⬜ |
| B-002 | Dépenses libres (décoration, divers) rattachées à un module                    | ⬜ |
| B-003 | Prévu / dépensé / reste, alertes de dépassement                                | ⬜ |
| B-004 | Répartition des dépenses (graphiques)                                          | ⬜ |
| B-005 | Comparaison entre années et graphique d'évolution                              | ⬜ |

## v0.8.0 — Les traditions ❤️

| ID    | Fonctionnalité                                                                 | Statut |
| ----- | ------------------------------------------------------------------------------ | ------ |
| T-001 | Catalogue : nom, description, photo, catégorie, période, personnes, matériel, notes | ⬜ |
| T-002 | Traditions d'exemple pré-remplies (lutin farceur, sapin, films…)               | ⬜ |
| T-003 | Sélection annuelle, marquage « réalisée », photos et souvenirs, favorites      | ⬜ |
| T-004 | Listes d'inspiration : films, dessins animés, livres, musiques, sorties, activités, recettes à découvrir | ⬜ |
| T-005 | Recherche étendue                                                               | ⬜ |

## v0.9.0 — L'atelier des lutins ✂️

| ID    | Fonctionnalité                                                                 | Statut |
| ----- | ------------------------------------------------------------------------------ | ------ |
| D-001 | Bibliothèque d'activités : titre, description, catégorie, photo, tutoriel, coût | ⬜ |
| D-002 | Projets créatifs avec fournitures                                              | ⬜ |
| D-003 | Suivi : idées, sélectionnés, en cours, terminés, photos, coûts                  | ⬜ |
| D-004 | Décoration : inspirations (photos, liens, commentaires)                         | ⬜ |
| D-005 | Recherche étendue                                                               | ⬜ |

## v0.10.0 — La boîte à souvenirs 📖

| ID    | Fonctionnalité                                                                 | Statut |
| ----- | ------------------------------------------------------------------------------ | ------ |
| S-001 | Journal de Noël : texte libre, anecdotes, moments marquants, photos            | ⬜ |
| S-002 | Archives : menus, cadeaux offerts, calendriers réalisés, traditions d'une année | ⬜ |
| S-003 | Bilan annuel automatique                                                        | ⬜ |
| S-004 | Assistant « Nouvelle année » : dupliquer des éléments de l'année passée         | ⬜ |
| S-005 | Comparaison des préparatifs entre années                                        | ⬜ |
| S-006 | Export PDF du bilan / carnet de l'année                                         | ⬜ |

## v1.0.0 — Noël magique ✨

| ID    | Fonctionnalité                                                                 | Statut |
| ----- | ------------------------------------------------------------------------------ | ------ |
| X-001 | Animations et touches féériques (neige légère, transitions)                    | ⬜ |
| X-002 | Audit accessibilité (TalkBack, contrastes, tailles de police)                  | ⬜ |
| X-003 | Recherche : filtres avancés                                                     | ⬜ |
| X-004 | Performances (grandes bibliothèques de photos) et tests de migration complets  | ⬜ |
| X-005 | Corrections issues des retours                                                  | ⬜ |
