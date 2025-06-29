// build.gradle.kts (benchmark 모듈)

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.benchmark) // Microbenchmark 라이브러리 플러그인
    // Kotlin 사용 여부와 상관없이, Android 프로젝트에서는 대부분 Kotlin 플러그인이 필요할 수 있습니다.
    // 만약 완전히 Java만 사용하고 Kotlin 관련 코드가 없다면 이 줄은 제거해도 됩니다.
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.benchmark"
    compileSdk = 35 // 최신 API 레벨 유지

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.benchmark.junit4.AndroidBenchmarkRunner"
    }

    // --- 중요 수정 사항 시작 ---

    // 벤치마크를 실행할 빌드 타입을 명시합니다. 일반적으로 "release" 타입을 사용합니다.
    testBuildType = "release" // 벤치마크는 release 빌드 타입으로 실행됩니다.

    buildTypes {
        // 'debug' 빌드 타입은 벤치마크에 적합하지 않습니다.
        // 벤치마크는 'debuggable false'인 빌드에서 실행되어야 합니다.
        // 따라서 'release' 빌드 타입을 명시적으로 설정하는 것이 중요합니다.
        release {
            // 이 빌드 타입이 기본 벤치마크 빌드 타입이 됩니다.
            isDefault = true

            // **가장 중요**: 벤치마크는 디버그 불가능해야 합니다.
            // 그래들에서 직접 설정할 수 없으므로, AndroidManifest.xml에서 설정해야 합니다.
            // 이 부분에 대한 설명은 아래에 추가하겠습니다.
            // isDebuggable = false // 이 줄은 라이브러리 모듈에서는 직접 설정할 수 없습니다.

            isMinifyEnabled = true // 코드 축소 활성화 (성능에 더 가깝게)
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "benchmark-proguard-rules.pro"
            )
            // 대상 앱의 빌드 타입과 일치하도록 폴백 설정 (Macrobenchmark에서 더 중요)
            matchingFallbacks += listOf("release")
        }
        // debug 빌드 타입은 벤치마크에서는 사용하지 않으므로, 제거하거나 비활성화하는 것이 좋습니다.
        // 아니면 최소한 isDebuggable = true로 유지하고 벤치마크 대상에서 제외해야 합니다.
        // 일반적으로 벤치마크 모듈에서는 release 빌드 타입만 사용합니다.
        debug {
            // 디버그 빌드는 벤치마크용이 아니므로, 필요한 경우에만 유지합니다.
            // isDebuggable = true // 기본값이므로 명시할 필요는 없습니다.
        }
    }
    // --- 중요 수정 사항 끝 ---

    compileOptions {
        // 자바 소스 및 타겟 컴파일 버전을 프로젝트에 맞게 설정합니다.
        // 현재 11로 설정되어 있으니, JDK 11을 사용하고 있다면 적절합니다.
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // AndroidX Test 라이브러리 (JUnit 테스트 실행에 필요)
    androidTestImplementation(libs.runner)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.junit)

    // Microbenchmark JUnit 4 라이브러리
    androidTestImplementation(libs.benchmark.junit4)

    // 벤치마크 대상이 되는 모듈을 여기에 추가할 수 있습니다.
    // 예를 들어, 앱 모듈에 있는 코드를 벤치마크하고 싶다면 (권장되지 않지만):
    // implementation(project(":app"))
    // 하지만 벤치마크 문서에서는 벤치마크할 코드를 별도의 라이브러리 모듈로 분리하는 것을 권장합니다.
    // 만약 벤치마크할 로직이 `app` 모듈에 있다면, 해당 로직을 새로운 라이브러리 모듈로 옮긴 후
    // 그 라이브러리 모듈을 이곳에 의존성으로 추가하는 것이 가장 좋은 방법입니다.
    // implementation(project(":your-code-library"))
}