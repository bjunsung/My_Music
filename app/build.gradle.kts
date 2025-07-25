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


dependencies {    // Room core
    implementation("io.github.everythingme:overscroll-decor-android:1.1.1") //overscroll
    implementation("androidx.room:room-runtime:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    implementation(files("libs/auth-release.aar"))
    implementation("androidx.core:core-ktx:1.12.0") // Kotlin DSL 필요
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("androidx.webkit:webkit:1.8.0") // WebView 지원 향상
    implementation("com.github.chrisbanes:PhotoView:2.3.0") // 이미지 확대 가능
    implementation("com.google.android.material:material:1.12.0") //layout 모서리 둥글게
    //annotationProcessor("com.github.bumptech.glide:compiler:4.16.0") // Java 프로젝트용 Glide
    implementation("com.github.bumptech.glide:glide:4.16.0")    //Glide 설정
    kapt("com.github.bumptech.glide:compiler:4.16.0") //⬅️ annotationProcessor를 kapt로 변경
    implementation("jp.wasabeef:recyclerview-animators:4.0.2") //recycler view item change animation
    implementation("com.squareup.picasso:picasso:2.71828")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.jakewharton.picasso:picasso2-okhttp3-downloader:1.1.0")
    implementation("com.kizitonwose.calendar:view:2.3.0") //custom 캘린더
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0") //chart
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
    implementation("com.tbuonomo:dotsindicator:4.3") // 원형 인디케이터
    implementation("me.relex:circleindicator:2.1.6")
    implementation("com.google.android.exoplayer:exoplayer:2.19.1")
    // Compose Core
    implementation("androidx.compose.ui:ui:1.5.0")
    implementation("androidx.compose.material:material:1.5.0")
    implementation("androidx.compose.ui:ui-tooling-preview:1.5.0")
    debugImplementation("androidx.compose.ui:ui-tooling:1.5.0")

// Activity-Compose (Java Activity에서도 ComposeView 쓸 때 필수)
    implementation("androidx.activity:activity-compose:1.7.2")

// Coil (이미지 로드 시)
    implementation("io.coil-kt:coil-compose:2.3.0")

    implementation("com.google.android.flexbox:flexbox:3.0.0")
    implementation(libs.palette)  //Glide
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

