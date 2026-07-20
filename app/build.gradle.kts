import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.paparazzi)
    alias(libs.plugins.jacoco)
}

android {
    namespace = "com.chatassist"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.chatassist"
        minSdk = 26
        targetSdk = 35
        versionCode = 22
        versionName = "0.22.0"

        // v0.15：HiltTestRunner 让 @HiltAndroidTest 拿到 HiltTestApplication 隔离容器
        testInstrumentationRunner = "com.chatassist.HiltTestRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // v0.22 (B6)：BuildConfig 注入构建期元信息（About 页显示 + 排错定位）
        //   - BUILD_TIME 用 ISO8601 字符串：避免 long 字面量后缀 `L` 在 Kotlin DSL 模板里被吞
        //   - GIT_SHA 从 project property `GIT_SHA` 读（CI 传入）；本机 `gradle -PGIT_SHA=$(git rev-parse --short HEAD)`；
        //     没传则 "unknown"（非阻塞，开发者本地/导出 tarball 也能跑）
        //   为什么不直接 `exec("git ...")`？Configuration cache 8.6+ 禁用 configuration 阶段启动子进程。
        val buildTimeIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
        buildConfigField("String", "BUILD_TIME", "\"$buildTimeIso\"")
        val gitShaValue = (project.findProperty("GIT_SHA") as? String)?.takeIf { it.isNotBlank() } ?: "unknown"
        buildConfigField("String", "GIT_SHA", "\"$gitShaValue\"")
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            buildConfigField("String", "VERSION_SUFFIX", "\"debug\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "VERSION_SUFFIX", "\"\"")
        }
        // v0.15：Macrobenchmark 配套 buildType，继承 release（不混淆 + 走 release 配置）
        create("benchmark") {
            initWith(getByName("release"))
            isMinifyEnabled = false
            isShrinkResources = false
            isDebuggable = false  // Macrobenchmark 要求 target app 不可 debuggable
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
            buildConfigField("String", "VERSION_SUFFIX", "\"benchmark\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests.all {
            it.useJUnitPlatform()  // 切到 JUnit 5（v0.3 引入）
        }
        // v0.14：启用 JaCoCo 单元测试覆盖率（Domain/Data 层 ≥80% 目标）
        unitTests.isIncludeAndroidResources = true
    }

    packaging {
        resources {
            excludes += listOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/LICENSE.md",
                "/META-INF/LICENSE-notice.md",
            )
        }
    }
}

dependencies {
    // AndroidX 核心
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.splashscreen)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // DataStore
    implementation(libs.datastore.preferences)

    // 网络
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.okhttp.sse)
    implementation(libs.kotlinx.serialization.json)

    // 协程
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)

    // 后台
    implementation(libs.workmanager)

    // ML Kit
    implementation(libs.mlkit.text.recognition)

    // 其他
    implementation(libs.encrypted.prefs)
    implementation(libs.timber)

    // Debug 工具
    debugImplementation(libs.androidx.compose.ui.tooling)

    // 单元测试
    testImplementation(libs.junit5.jupiter)
    testImplementation(libs.kotlin.test.junit5)  // kotlin.test.* 桥接 JUnit5
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.room.testing)
    // Paparazzi 1.3.4 配套 JUnit 4（@get:Rule / @Test from JUnit 4）
    testImplementation(libs.junit4)
    testRuntimeOnly(libs.junit5.platform.launcher)  // Gradle 8.x 跑 JUnit5 必需
    testRuntimeOnly(libs.junit.vintage.engine)  // JUnit5 vintage 跑 JUnit4 测试

    // 仪器测试（v0.14：androidTest 框架 + Compose UI Test）
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.junit5.jupiter)
    androidTestImplementation(libs.mockk)
    androidTestImplementation(libs.coroutines.test)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.hilt.work)  // v0.15：让 WorkManager 测试有 Hilt rule
    androidTestImplementation(libs.hilt.android.testing)  // v0.15：@HiltAndroidTest / HiltAndroidRule
    androidTestImplementation(libs.room.testing)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

// v0.14 全量测试：JaCoCo 覆盖率报告（Domain/Data 层 ≥80% 目标）
// 跑 ./gradlew :app:jacocoTestReport 生成 app/build/reports/jacoco/test/html/index.html
tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    extensions.configure(JacocoTaskExtension::class) {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*", "*.R", "*.R$*")
    }
}

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
    classDirectories.setFrom(
        fileTree("$buildDir/tmp/kotlin-classes/debug") {
            exclude("**/R.class", "**/R$*.class", "**/BuildConfig.*")
        },
    )
    sourceDirectories.setFrom(files("src/main/kotlin"))
    executionData.setFrom(files("$buildDir/jacoco/testDebugUnitTest.exec"))
}

// v0.22 (B6)：BuildConfig 注入构建期元信息。
// 设计：
//   - BUILD_TIME：ISO8601 字符串，configuration 阶段无副作用
//   - GIT_SHA：从 project property `GIT_SHA` 读（CI 传入）；本机 `gradle -PGIT_SHA=$(git rev-parse --short HEAD)`；
//     没传则 "unknown"（非阻塞，开发者本地/导出 tarball 也能跑）
//   为什么不直接 `exec("git ...")`？Configuration cache 8.6+ 禁用 configuration 阶段启动子进程。
