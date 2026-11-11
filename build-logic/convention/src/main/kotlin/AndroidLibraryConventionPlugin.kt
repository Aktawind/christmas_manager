import com.aktawind.monnoel.buildlogic.TARGET_SDK
import com.aktawind.monnoel.buildlogic.configureKotlinAndroid
import com.aktawind.monnoel.buildlogic.libs
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.library")
            pluginManager.apply("org.jetbrains.kotlin.android")
            extensions.configure<LibraryExtension> {
                configureKotlinAndroid(this)
                testOptions.targetSdk = TARGET_SDK
                lint.targetSdk = TARGET_SDK
                // Espace de noms dérivé du chemin du module : :core:database -> com.aktawind.monnoel.core.database
                namespace = "com.aktawind.monnoel" + path.replace(':', '.')
            }
            dependencies {
                add("testImplementation", libs.findLibrary("junit").get())
                add("testImplementation", libs.findLibrary("kotlin-test").get())
                add("testImplementation", libs.findLibrary("kotlinx-coroutines-test").get())
            }
        }
    }
}
