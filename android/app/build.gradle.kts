plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "io.github.graviton94.carpediem"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.graviton94.carpediem"
        minSdk = 26
        targetSdk = 36
        // CI 실행 번호로 버전 코드를 올려, 새 APK 가 이전 것을 덮어쓸 수 있게 한다.
        // 같은 실행을 다시 돌려도 (run attempt) 번호가 겹치지 않게 ×10 + 시도 횟수. CD_VERSION_CODE 로 직접 정할 수도 있음
        versionCode = System.getenv("CD_VERSION_CODE")?.toInt()
            ?: ((System.getenv("GITHUB_RUN_NUMBER") ?: "0").toInt() * 10 + (System.getenv("GITHUB_RUN_ATTEMPT") ?: "1").toInt())
        versionName = System.getenv("CD_VERSION_NAME") ?: "1.0.0"
        // 개발자 도구 (설정의 버전을 여러 번 눌러 켜는 시험 기능): 직접 설치 · debug 빌드에만. Play 업로드 키로 만드는 빌드에서는 꺼짐
        buildConfigField("boolean", "DEV_TOOLS", if (System.getenv("CD_UPLOAD_STORE") != null) "false" else "true")
    }

    signingConfigs {
        // 직접 설치(APK) 전용 키: GitHub Secrets 에서만 (android.yml 이 SIDELOAD_* 를 파일로 풀어 경로 · 암호를 환경 변수로 넘김, keystore/README.md)
        System.getenv("CD_SIDELOAD_STORE")?.let { path ->
            create("sideload") {
                storeFile = file(path)
                storePassword = System.getenv("CD_SIDELOAD_STORE_PASSWORD")
                keyAlias = System.getenv("CD_SIDELOAD_KEY_ALIAS") ?: "sideload"
                keyPassword = System.getenv("CD_SIDELOAD_KEY_PASSWORD")
            }
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
            // 업로드 키가 있으면 (스토어용 AAB) 그것으로, 없으면 직접 설치용 키로 (둘 다 GitHub Secrets)
            signingConfig = signingConfigs.findByName("upload") ?: signingConfigs.findByName("sideload") ?: signingConfigs.getByName("debug")   // 키가 없으면 (내 컴퓨터 · 키 없는 빌드) 시험용 debug 키
        }
        debug {
            // 직접 설치용 키가 있으면 (Secrets) 그것으로, 없으면 기본 debug 키
            signingConfig = signingConfigs.findByName("sideload") ?: signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
    // 앱 안에서 · 설정에서 말을 바꿔도 글자가 빠지지 않게, 모든 말을 한 번에 받음
    bundle { language { enableSplit = false } }
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
    implementation("com.android.billingclient:billing-ktx:8.0.0")
    implementation("androidx.glance:glance-appwidget:1.1.1")
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    // 새 버전 알림 (Play 앱 안 업데이트, 가벼운 방식만)
    implementation("com.google.android.play:app-update-ktx:2.1.0")
}

// 스토어용 AAB 는 업로드 키로만: 키 없이 bundleRelease 를 돌리면 (debug 키 · 개발자 도구가 켜진 채로 만들어지지 않게) 멈춘다
gradle.taskGraph.whenReady {
    if (allTasks.any { it.path == ":app:bundleRelease" } && System.getenv("CD_UPLOAD_STORE") == null)
        throw GradleException("bundleRelease needs the Play upload key (CD_UPLOAD_STORE). Use the android-release workflow.")
}
