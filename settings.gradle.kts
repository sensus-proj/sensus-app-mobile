pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("androidx.*")
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                excludeGroupByRegex("com\\.google\\.dagger.*")
            }
        }
        // Mirror oficial de Maven Central operado por Google (E-07): Central devuelve 404
        // intermitentes en los runners de GitHub. mavenCentral() queda como respaldo.
        maven {
            name = "MavenCentralGoogleMirror"
            url = uri("https://maven-central.storage-download.googleapis.com/maven2/")
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google {
            content {
                includeGroupByRegex("androidx.*")
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                excludeGroupByRegex("com\\.google\\.dagger.*")
            }
        }
        // Mirror oficial de Maven Central operado por Google (E-07): Central devuelve 404
        // intermitentes en los runners de GitHub. mavenCentral() queda como respaldo.
        maven {
            name = "MavenCentralGoogleMirror"
            url = uri("https://maven-central.storage-download.googleapis.com/maven2/")
        }
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "Sensus"
include(":app")
