plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.app_truyen"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.app_truyen"
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
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)


    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    //Firebase
    implementation(platform("com.google.firebase:firebase-bom:34.5.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")

    //Cloudinary
    implementation("com.cloudinary:cloudinary-android:3.0.2")
    implementation("com.cloudinary:cloudinary-android-download:3.0.2")
    implementation("com.cloudinary:cloudinary-android-preprocess:3.0.2")

    //Retrofit core library
    implementation("com.squareup.retrofit2:retrofit:2.9.0")

    // Converter GSON để chuyển đổi JSON sang objects
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    //Thư viện okhttp để up load file
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    implementation("com.github.bumptech.glide:glide:4.16.0")

    implementation("com.google.android.flexbox:flexbox:3.0.0")

}