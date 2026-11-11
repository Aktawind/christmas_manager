plugins {
    alias(libs.plugins.monnoel.android.library)
    alias(libs.plugins.monnoel.hilt)
}

dependencies {
    api(project(":core:domain"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.exifinterface)
    implementation(libs.kotlinx.coroutines.android)
}
