package com.aktawind.monnoel.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

const val COMPILE_SDK = 36
const val MIN_SDK = 26
const val TARGET_SDK = 36

val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun Project.configureKotlinAndroid(extension: CommonExtension<*, *, *, *, *, *>) {
    extension.apply {
        compileSdk = COMPILE_SDK
        defaultConfig {
            minSdk = MIN_SDK
        }
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
        testOptions {
            unitTests.isIncludeAndroidResources = true
            unitTests.isReturnDefaultValues = true
        }
        lint {
            abortOnError = true
            warningsAsErrors = false
            checkDependencies = false
            disable += setOf("GradleDependency", "NewerVersionAvailable", "AndroidGradlePluginVersion", "ObsoleteSdkInt")
        }
    }
    configureKotlin()
}

internal fun Project.configureKotlin() {
    // Robolectric hors ligne si un dossier de dépendances est fourni (environnement sans accès réseau).
    tasks.withType<org.gradle.api.tasks.testing.Test>().configureEach {
        System.getenv("ROBOLECTRIC_DEPS_DIR")?.let { dir ->
            systemProperty("robolectric.offline", "true")
            systemProperty("robolectric.dependency.dir", dir)
        }
    }
    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
            freeCompilerArgs.addAll(
                "-Xannotation-default-target=param-property",
            )
        }
    }
}
