plugins {
    alias(libs.plugins.monnoel.android.library)
    alias(libs.plugins.monnoel.android.compose)
}

dependencies {
    api(project(":core:designsystem"))
    implementation(project(":core:common"))
    implementation(project(":core:domain"))
    api(libs.coil.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
}
