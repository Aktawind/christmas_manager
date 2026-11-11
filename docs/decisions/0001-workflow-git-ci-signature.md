# ADR 0001 — Workflow Git, CI et signature des APK

- Date : 2026-11-11
- Statut : accepté

## Contexte

L'utilisateur teste sur tablette Android, sans environnement de développement. Chaque
version doit être installable directement depuis l'appareil, et les données ne doivent
jamais être perdues entre deux versions.

## Décision

- Branches : `main` (versions validées et taguées), `develop` (intégration).
- GitHub Actions :
  - `ci.yml` — sur push `develop` et pull requests : lint, tests unitaires, build de
    l'APK signé, publication en pré-version GitHub `develop-latest`
    (le tag `develop-latest` est déplacé à chaque build, l'APK est remplacé).
  - `release.yml` — sur tag `v*` : build `release` signé, publication d'une Release
    GitHub avec `docs/release-notes/<tag>.md` comme description.
  - `milestones.yml` — crée les jalons manquants listés dans `.github/milestones.json`.
- Signature : une seule clé (RSA 4096, validité 30 ans) générée au démarrage. Stockée
  uniquement dans les secrets GitHub et dans une copie de sauvegarde conservée par
  l'utilisateur. **Perdre cette clé empêcherait toute mise à jour de l'application
  installée.**
- Une seule application (« Mon Noël », `com.aktawind.monnoel`) : l'APK de test et
  l'APK validé sont signés avec la même clé et se mettent à jour l'un l'autre.
  `versionCode` = nombre de commits, toujours croissant (pas de rétrogradation refusée).
  *(Révisé le 2026-11-11 à la demande de l'utilisateur : la variante « beta » séparée
  est abandonnée.)*

## Conséquences

- L'utilisateur doit renseigner une fois les 4 secrets dans GitHub
  (Settings → Secrets and variables → Actions).
- Installer un APK de test met à jour l'application existante en gardant les données
  (faire tout de même un export avant, par prudence).
