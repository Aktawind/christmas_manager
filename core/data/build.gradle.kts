plugins {
    alias(libs.plugins.monnoel.android.library)
    alias(libs.plugins.monnoel.hilt)
}

dependencies {
    api(project(":core:domain"))
    implementation(project(":core:database"))
    implementation(libs.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
}
