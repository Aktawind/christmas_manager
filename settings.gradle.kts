pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "MonNoel"

include(":app")
include(":core:model")
include(":core:common")
include(":core:domain")
include(":core:database")
include(":core:data")
include(":core:backup")
include(":core:designsystem")
include(":core:ui")
include(":feature:home")
include(":feature:years")
include(":feature:settings")
