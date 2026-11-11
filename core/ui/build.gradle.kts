plugins {
    alias(libs.plugins.monnoel.android.library)
    alias(libs.plugins.monnoel.android.compose)
}

dependencies {
    api(project(":core:designsystem"))
    implementation(project(":core:common"))
}
