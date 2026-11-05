# Construire l'APK de SubTopo-Geo

## Ce que contient ce projet
Un projet Android (Kotlin) qui embarque l'application SubTopo-Geo (`app/src/main/assets/index.html`) dans une WebView.
Tout fonctionne hors ligne : la page est dans l'APK et les données restent sur le téléphone.
L'application affiche X, Y, Z, gère les datums libyens, importe/exporte GPX, KML, KMZ, DXF, CSV, et propose « Aller à… ».

## État de validation (honnête)
| Élément | Statut |
|---|---|
| Application web (`index.html`) | **Testée** : 86 contrôles automatiques dans un vrai navigateur, calculs vérifiés contre une référence indépendante |
| Pont d'enregistrement de fichiers (JavaScript → Android) côté web | **Testé** (contenu des 5 formats vérifié) |
| Coque Kotlin (`MainActivity.kt`), Gradle, manifeste | **Écrits, NON compilés** : aucun Android SDK dans mon environnement. XML validés. |
| GPS réel, carte SD, import depuis le téléphone, bouton retour | **À tester sur votre téléphone** |

Si la compilation affiche une erreur, copiez-la-moi : je corrige.

## Méthode A : Android Studio (recommandée)
1. Installez Android Studio (gratuit) : https://developer.android.com/studio
2. Menu **File > Open**, choisissez le dossier `subtopo-geo`.
3. Attendez la synchronisation Gradle (acceptez l'installation du SDK 34 si demandé). Il faut Internet la première fois.
4. Menu **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
5. Cliquez sur **locate** : le fichier est `app/build/outputs/apk/debug/app-debug.apk`.
6. Copiez-le sur le téléphone et ouvrez-le (autorisez « installer des applis inconnues »).
   Ou branchez le téléphone (débogage USB) et cliquez sur **Run**.

## Méthode B : GitHub, uniquement depuis le navigateur (aucune installation)
1. Créez un compte sur https://github.com/signup (gratuit).
2. **New repository** : nom `subtopo-geo`, **Private**, ne cochez rien d'autre, **Create repository**.
3. Sur la page vide, cliquez **uploading an existing file**. Décompressez le zip, ouvrez le dossier `subtopo-geo`, sélectionnez **tout ce qu'il contient** (`app`, `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, `gradle`, `.github`…) et glissez-le dans la page. Ne glissez pas le dossier `subtopo-geo` lui-même : `build.gradle.kts` doit être à la racine du dépôt. Cliquez **Commit changes**.
4. Vérifiez que la racine du dépôt montre `app`, `build.gradle.kts`, `settings.gradle.kts` et `.github`.
   Si `.github` manque (dossier masqué non envoyé) : **Add file > Create new file**, tapez le nom `.github/workflows/build-apk.yml` (les `/` créent les dossiers), collez le contenu du fichier `build-apk.yml` fourni, puis **Commit**.
5. Onglet **Actions**. Si un bouton « I understand my workflows, go ahead and enable them » apparaît, cliquez dessus. Choisissez **Build APK** puis **Run workflow**.
6. Attendez 5 à 12 minutes. Une coche verte = réussi.
7. Récupérez l'APK :
   - page d'accueil du dépôt, colonne de droite **Releases** > dernière version > **SubTopo-Geo.apk** (téléchargeable directement depuis le téléphone, connecté à GitHub) ;
   - ou dans la page du run, **Artifacts** > **SubTopo-Geo-APK** (zip contenant l'APK).
8. Installez l'APK sur le téléphone (autorisez les applis inconnues).
Si une croix rouge apparaît : ouvrez l'étape en échec, copiez les ~30 dernières lignes du journal et envoyez-les-moi.

## Méthode C : ligne de commande
JDK 17, Android SDK (plateforme 34) et Gradle 8.9 installés : `gradle assembleDebug` à la racine.

## À tester sur le téléphone
1. Au premier lancement : accepter la localisation. Ouvrir « GPS réel » (onglet Traces) à l'extérieur.
2. Mode avion : l'application doit démarrer et afficher les points, traces et mesures.
3. Export : GPX, KML, KMZ, DXF, CSV. Android demande où enregistrer (mémoire ou carte SD).
4. Import d'un fichier depuis le téléphone, puis « Aller à… » avec des coordonnées X, Y.
5. Rotation de l'écran, bouton retour, langues FR / EN / AR.


## Mettre à jour l'application (nouvelle version)
1. Décompressez `mise-a-jour.zip` : il contient un dossier `app` (3 fichiers : `index.html`, `build.gradle.kts`, `debug.keystore`).
2. Sur GitHub, page d'accueil du dépôt : **Add file > Upload files**. Glissez le **dossier `app`** dans la page (il remplace les fichiers du même chemin). **Commit changes**.
3. **Actions > Build APK > Run workflow** (ne pas utiliser « Re-run », qui relance l'ancienne version). Attendez la coche verte, puis téléchargez l'artefact.
4. **Première fois seulement** : désinstallez l'ancienne version du téléphone avant d'installer la nouvelle (la signature de test change une fois). **Exportez d'abord vos points et traces** (GPX, KML, CSV...), car la désinstallation efface les données de l'application.
5. Les mises à jour suivantes s'installeront par-dessus, sans perte de données.

## Limites de cette première version
- **Hybride, pas 100 % natif** : l'enregistrement de trace s'arrête si l'écran s'éteint ou si l'application passe en arrière-plan (l'écran reste allumé tant que l'application est ouverte). Une version native avec service GPS est la suite prévue.
- **Cartes** : fonds en ligne (OpenStreetMap, OpenTopoMap, Esri) avec mémoire locale de la zone téléchargée. Pas encore de MBTiles ni de GeoTIFF. Les conditions des fournisseurs interdisent le téléchargement massif.
- **Z** : altitude brute du GPS (sans correction du géoïde).
- **Paramètres libyens** : issus d'EPSG, à valider par votre organisme (voir PARAMETRES_LIBYE.md).
- **APK de test** (signé avec la clé de débogage). Pour une diffusion, il faut une clé de signature personnelle.
