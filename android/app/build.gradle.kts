plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "io.github.graviton94.carpediem"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.graviton94.carpediem"
        minSdk = 26
        targetSdk = 35
        // CI 실행 번호로 버전 코드를 올려, 새 APK 가 이전 것을 덮어쓸 수 있게 한다
        versionCode = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()
        versionName = System.getenv("CD_VERSION_NAME") ?: "1.0.0"
    }

    signingConfigs {
        // 직접 설치(APK) 전용 키. Google Play 출시에는 쓰지 않는다 (keystore/README.md)
        create("sideload") {
            storeFile = rootProject.file("keystore/sideload.jks")
            storePassword = "carpediem-sideload"
            keyAlias = "sideload"
            keyPassword = "carpediem-sideload"
        }
        // Google Play 업로드 키: GitHub Secrets 에서만 (android-release.yml 이 파일로 풀어 경로 · 암호를 환경 변수로 넘김)
        System.getenv("CD_UPLOAD_STORE")?.let { path ->
            create("upload") {
                storeFile = file(path)
                storePassword = System.getenv("CD_UPLOAD_STORE_PASSWORD")
                keyAlias = System.getenv("CD_UPLOAD_KEY_ALIAS")
                keyPassword = System.getenv("CD_UPLOAD_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // 업로드 키가 있으면 (스토어용 AAB) 그것으로, 없으면 직접 설치용 키로
            signingConfig = signingConfigs.findByName("upload") ?: signingConfigs.getByName("sideload")
        }
        debug {
            signingConfig = signingConfigs.getByName("sideload")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
}

dependencies {
    implementation(project(":core"))

    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.core:core-ktx:1.15.0")
    // 응원하기 결제 (Google Play)
    implementation("com.android.billingclient:billing-ktx:7.1.1")
    implementation("androidx.glance:glance-appwidget:1.1.1")
    implementation("androidx.work:work-runtime-ktx:2.10.0")
}
