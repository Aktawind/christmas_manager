import java.util.Properties

plugins {
    alias(libs.plugins.monnoel.android.application)
    alias(libs.plugins.monnoel.android.compose)
    alias(libs.plugins.monnoel.hilt)
    alias(libs.plugins.kotlin.serialization)
}

// ---- Version (CLAUDE.md) : versionCode = nombre de commits, toujours croissant.
val baseVersion: String = providers.gradleProperty("monnoel.version").get()

fun git(vararg args: String): String? = try {
    providers.exec {
        commandLine("git", *args)
        isIgnoreExitValue = true
    }.standardOutput.asText.get().trim().ifEmpty { null }
} catch (e: Exception) {
    null
}

val commitCount: Int = git("rev-list", "--count", "HEAD")?.toIntOrNull() ?: 1
val shortSha: String = git("rev-parse", "--short", "HEAD") ?: "local"
val isOfficialRelease: Boolean = providers.environmentVariable("MONNOEL_RELEASE").orNull == "true"

// ---- Signature : variables d'environnement (CI) ou keystore.properties (local, jamais commité).
val keystoreProps = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun signingValue(env: String, prop: String): String? =
    providers.environmentVariable(env).orNull ?: keystoreProps.getProperty(prop)

val keystorePath = signingValue("SIGNING_KEYSTORE_PATH", "storeFile")

android {
    namespace = "com.aktawind.monnoel"

    defaultConfig {
        applicationId = "com.aktawind.monnoel"
        versionCode = commitCount
        versionName = if (isOfficialRelease) baseVersion else "$baseVersion-dev.$commitCount+$shortSha"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    signingConfigs {
        if (keystorePath != null) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = signingValue("SIGNING_KEYSTORE_PASSWORD", "storePassword")
                keyAlias = signingValue("SIGNING_KEY_ALIAS", "keyAlias")
                keyPassword = signingValue("SIGNING_KEY_PASSWORD", "keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            // Pas de minification pour l'instant : priorité à la fiabilité (à revoir en v1.0).
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
        }
    }

    buildFeatures {
        buildConfig = true
    }

    packaging {
        resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}", "/META-INF/LICENSE*")
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":core:domain"))
    implementation(project(":core:database"))
    implementation(project(":core:data"))
    implementation(project(":core:backup"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:ui"))
    implementation(project(":feature:home"))
    implementation(project(":feature:years"))
    implementation(project(":feature:settings"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.androidx.compose.material3.navigation.suite)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.work.runtime.ktx)
    implementation(libs.hilt.work)
    ksp(libs.hilt.androidx.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
