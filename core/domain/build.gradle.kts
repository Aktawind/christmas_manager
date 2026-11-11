plugins {
    alias(libs.plugins.monnoel.jvm.library)
}

dependencies {
    api(project(":core:model"))
    api(project(":core:common"))
}
