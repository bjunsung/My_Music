plugins {
    //alias(libs.plugins.android.application)
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
}


android {
    namespace = "com.example.mymusic"
    compileSdk = 35

    signingConfigs {
        getByName("debug") {
            storeFile = file("C:\\Users\\baejunsung\\Documents\\android studio\\key\\My_Music_key.jks")
            storePassword = "cgial01"
            keyAlias = "key0"
            keyPassword = "cgial01"
        }

        create("release") {
            storeFile = file("C:\\Users\\baejunsung\\Documents\\android studio\\key\\My_Music_key.jks")
            storePassword = "cgial01"
            keyAlias = "key0"
            keyPassword = "cgial01"
        }
    }


    defaultConfig {
        applicationId = "com.example.mymusic"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders.put("redirectSchemeName", "com.example.mymusic")
        manifestPlaceholders.put("redirectHostName", "callback")

    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        getByName("debug") {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            signingConfig = signingConfigs.getByName("debug")
            isDebuggable = true;
        }


    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}


dependencies {    // Room core
    implementation("io.github.everythingme:overscroll-decor-android:1.1.1") //overscroll
    implementation("com.github.bumptech.glide:glide:4.16.0")  //Glide
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0") // Java 프로젝트용 Glide
    implementation("androidx.room:room-runtime:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    implementation(files("libs/auth-release.aar"))
    implementation("androidx.core:core-ktx:1.12.0") // Kotlin DSL 필요
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.squareup.picasso:picasso:2.71828")
    implementation("androidx.webkit:webkit:1.8.0") // WebView 지원 향상
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.room.common.jvm)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}

