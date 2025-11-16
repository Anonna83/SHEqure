plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.mypanicapp"
    compileSdk = 35 // Using compileSdk 35 as per your manifest targetApi

    defaultConfig {
        applicationId = "com.example.mypanicapp"
        minSdk = 24 // Keeping minSdk at 24 as previously discussed for broader compatibility
        targetSdk = 35 // Keeping targetSdk at 35 as per your manifest targetApi
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
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // Dependency for Google Play Services Location API (FusedLocationProviderClient)
    // This is crucial for getting the device's location efficiently.
    implementation("com.google.android.gms:play-services-location:21.0.1") // Using a common stable version

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation("androidx.fragment:fragment-ktx:1.6.2")
}