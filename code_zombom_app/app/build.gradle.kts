plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.code_zombom_app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.code_zombom_app"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // This manages all Firebase library versions for you. Only declare it once.
    implementation(platform("com.google.firebase:firebase-bom:34.3.0"))

    // --- KEEP THE LIBRARIES YOU ACTUALLY USE ---
    // These now use the versions defined in the BOM or your version catalog (libs.versions.toml)
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")

    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("com.github.bumptech.glide:glide:4.12.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.12.0")

    implementation("androidx.recyclerview:recyclerview:1.3.2") // It's okay to keep one of these for now
    implementation("com.google.android.libraries.places:places:3.4.0")

    // Use the version catalog aliases (libs.*) for standard AndroidX libraries
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.rendering)

    // --- KEEP YOUR TESTING LIBRARIES ---
    testImplementation(libs.junit)
    testImplementation("org.mockito:mockito-core:5.5.0")
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation("androidx.test.espresso:espresso-intents:3.5.1")
    androidTestImplementation("org.mockito:mockito-android:5.5.0")

    // Additional dependencies already in the project
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // Fragment testing
    implementation(libs.fragment.testing)

    // Firestore test helpers
    testImplementation("com.google.firebase:firebase-firestore:24.7.1")

    // Google Maps + Heat Map
    implementation("com.google.android.gms:play-services-maps:18.1.0")
    implementation("com.google.maps.android:android-maps-utils:3.4.0")
}
