plugins {
    //alias(libs.plugins.android.application)
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0"
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
        compose = true
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"  // 사용하는 Compose 버전 맞게
    }
}


// 파일 상단에 버전을 변수로 선언하면 관리하기 편합니다.
val media3Version = "1.8.0" // sessionCompat을 지원하는 안정적인 최신 버전

dependencies {
    // Room, Glide 등 기존에 잘 사용하던 다른 라이브러리들은 그대로 둡니다.
    implementation("io.github.everythingme:overscroll-decor-android:1.1.1")
    implementation("androidx.room:room-runtime:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    implementation(files("libs/auth-release.aar"))
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("androidx.webkit:webkit:1.8.0")
    implementation("com.github.chrisbanes:PhotoView:2.3.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    kapt("com.github.bumptech.glide:compiler:4.16.0")
    implementation("jp.wasabeef:recyclerview-animators:4.0.2")
    implementation("com.squareup.picasso:picasso:2.71828")
    implementation("com.jakewharton.picasso:picasso2-okhttp3-downloader:1.1.0")
    implementation("com.kizitonwose.calendar:view:2.3.0")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
    implementation("com.tbuonomo:dotsindicator:4.3")
    implementation("me.relex:circleindicator:2.1.6")
    implementation("com.google.android.flexbox:flexbox:3.0.0")

    // ⭐️⭐️⭐️ [수정된 부분] ⭐️⭐️⭐️
    // 기존의 exoplayer와 media3 관련 라인을 모두 지우고 아래 내용으로 교체하세요.
    implementation("androidx.media3:media3-exoplayer:$media3Version")
    implementation("androidx.media3:media3-session:$media3Version")
    implementation("androidx.media3:media3-ui:$media3Version") // 알림, UI 컨트롤러에 필요

    // Compose 관련 라이브러리들
    implementation(platform("androidx.compose:compose-bom:2024.05.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material:material")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("io.coil-kt:coil-compose:2.3.0")

    // 버전 카탈로그(libs)로 관리하던 라이브러리들
    implementation(libs.palette)
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

    implementation("androidx.media:media:1.7.0") // 2025년 기준 안정적인 최신 버전입니다.
}

