plugins {
    alias(libs.plugins.monnoel.jvm.library)
}

dependencies {
    api(libs.kotlinx.coroutines.core)
    api(libs.javax.inject)
    api(project(":core:model"))
}
