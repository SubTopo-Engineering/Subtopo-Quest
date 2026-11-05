# SubTopo-Geo : développé par Fathi Nasraoui

Application de cartographie et de topographie GPS hors ligne (X, Y, Z ; UTM ; ELD79 / LGD2006 ; GPX, KML, KMZ, DXF, CSV).

- `app/` : projet Android (WebView + application web embarquée). Voir **CONSTRUIRE_APK.md**.
- `core-geo/` : module Kotlin pur (UTM, ED50, géodésie), base d'une future version native. Non inclus dans le build de l'APK.
- `reference-python/` : implémentation de référence et tests (19/19) utilisés pour valider les formules.
- `PARAMETRES_LIBYE.md` : paramètres géodésiques libyens et sources.
