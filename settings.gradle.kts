pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "subtopo-geo"
include(":app")
// Le module :core-geo (Kotlin pur, étape 1) n'est PAS inclus dans le build de l'APK.
// Il sert de base à une future version 100 % native. Pour le tester seul : include(":core-geo")
