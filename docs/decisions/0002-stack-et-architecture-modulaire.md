# ADR 0002 — Stack technique et architecture modulaire

- Date : 2026-11-11
- Statut : proposé

## Décision

- Kotlin, Jetpack Compose + Material 3 (dont `material3-adaptive` pour tablette),
  MVVM + Clean Architecture, Hilt, Room, Navigation Compose (routes typées),
  WorkManager, DataStore, Coil, kotlinx.serialization, kotlinx.datetime.
- `minSdk 26` (Android 8), `targetSdk` = dernière API stable.
- Gradle multi-modules (`:app`, `:core:*`, `:feature:*`) avec version catalog
  (`gradle/libs.versions.toml`) et convention plugins (`build-logic/`).
- Une **seule base Room** dans `:core:database` (les relations entre modules — tâches
  liées à un cadeau, budget alimenté par les achats — sont plus simples et les
  migrations restent centralisées). Chaque feature a ses DAO.
- Les modules `feature` ne dépendent pas entre eux ; la communication passe par
  `:core:domain` (ex. automatisations de tâches, agrégation du budget, recherche).

## Pourquoi

- Une base unique évite les incohérences et les sauvegardes multi-fichiers.
- Le découpage en modules permet de livrer module par module (une version = un ou
  deux modules) sans toucher au reste.
