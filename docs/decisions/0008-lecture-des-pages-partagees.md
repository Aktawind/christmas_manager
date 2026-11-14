# ADR 0008 — Lecture des pages partagées (Internet facultatif)

- Date : 2026-11-14
- Statut : accepté

## Contexte

L'utilisatrice partage souvent une page de boutique vers Mon Noël. Recopier le titre, le prix
et une photo prend du temps.

## Décision

- Permission Android `INTERNET`, utilisée **uniquement** lorsqu'un lien est partagé vers
  l'application : la page est chargée comme par un navigateur (aucune autre donnée envoyée,
  aucun compte, aucun service tiers).
- Lecture des balises standard : Open Graph (`og:title`, `og:image`, `og:site_name`),
  `product:price:amount` / `og:price:amount`, `itemprop="price"`, données produit JSON-LD.
  Prix retenu seulement en euros (ou sans devise). Analyse pure et testée
  (`parseLinkPreview`, `:core:domain`), téléchargement dans `HttpLinkPreviewRepository`
  (`:core:data`, `HttpURLConnection`, délais de 8 s, 2 Mo de HTML et 10 Mo d'image max).
- L'image est importée comme une photo normale (redimensionnée, sauvegardée) ; elle est
  supprimée si l'utilisatrice annule.
- Un bouton « Passer » permet de ne pas attendre. Sans réseau, ou si le site refuse
  (Amazon bloque souvent), la fenêtre s'ouvre avec le simple texte partagé.
- L'application reste entièrement utilisable hors connexion.
