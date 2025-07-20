pluginManagement {
    plugins {
        id("org.jetbrains.kotlin.android") version "2.1.10"
        id("org.jetbrains.kotlin.kapt") version "2.1.10"
    }
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        maven { url = uri("https://jitpack.io") } // ✅ 이미지 확대 가능
    }
}

rootProject.name = "My Music"
include(":app")
include(":benchmark")
