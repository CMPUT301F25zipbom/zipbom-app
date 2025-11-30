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
    implementation("com.google.maps.android:android-maps-utils:3.8.2")
    implementation("implementation 'com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.android.volley:volley:1.2.1")
    implementation(platform("com.google.firebase:firebase-bom:34.3.0"))
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("com.github.bumptech.glide:glide:4.12.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.rendering)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.fragment.testing)
    annotationProcessor("com.github.bumptech.glide:compiler:4.12.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test.espresso:espresso-intents:3.5.1")

        implementation("com.google.firebase:firebase-bom:34.3.0")
        implementation("com.google.firebase:firebase-firestore")
        implementation("com.google.firebase:firebase-storage")

        // AndroidX and UI
        implementation("androidx.appcompat:appcompat:1.6.1")
        implementation("com.google.android.material:material:1.9.0")
        implementation("androidx.constraintlayout:constraintlayout:2.1.4")
        implementation("androidx.recyclerview:recyclerview:1.3.2")
        implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2")
        implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
        implementation("androidx.navigation:navigation-fragment-ktx:2.7.0")
        implementation("androidx.navigation:navigation-ui-ktx:2.7.0")

        // Image loading
        implementation("com.github.bumptech.glide:glide:4.12.0")
        annotationProcessor("com.github.bumptech.glide:compiler:4.12.0")

        // QR Code scanning
        implementation("com.journeyapps:zxing-android-embedded:4.3.0")

        // Unit testing
        testImplementation("junit:junit:4.13.2")
        androidTestImplementation("androidx.test.ext:junit:1.1.5")
        androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

        // Mockito for mocking Firestore
        testImplementation("org.mockito:mockito-core:5.5.0")
        androidTestImplementation("org.mockito:mockito-android:5.5.0")

        // Optional: Firebase test helpers
        testImplementation("com.google.firebase:firebase-firestore:24.7.1")

        // Autofill address
        implementation("com.google.android.libraries.places:places:3.4.0")

}