@file:Suppress("UnstableApiUsage")

// v0.15 模块 13 测试基建：Macrobenchmark 模块
// 官方模板要求：com.android.test plugin + targetProjectPath + benchmark buildType
// 依赖写法：com.android.test plugin 下的 androidTest source set 用 implementation / runtimeOnly
plugins {
    id("com.android.test")
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.chatassist.benchmark"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        targetSdk = 35
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // v0.15：Mumu 模拟器 + 测 :app debug build，跳过严格检查
        testInstrumentationRunnerArguments["androidx.benchmark.suppressErrors"] = "DEBUGGABLE,NOT-SELF-INSTRUMENTING"
    }

    buildTypes {
        debug {
            // v0.15：保留 debuggable 以便 Mumu 上跑通
        }
        // benchmark buildType：Macrobenchmark 默认走 release 派生，不混淆
        create("benchmark") {
            isMinifyEnabled = false
            isDebuggable = false
        }
    }

    // 指向被测 app 模块
    targetProjectPath = ":app"

    // v0.15：不开 self-instrumenting，让 Macrobenchmark 测 :app 而不是 benchmark module 自己
    // （self-instrumenting 模式下 target = benchmark APK 自己，killProcess 会杀自己）
    // 用 suppressErrors 把 NOT-SELF-INSTRUMENTING 警告降级

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += listOf(
                "META-INF/LICENSE.md",
                "META-INF/LICENSE-notice.md",
                "META-INF/AL2.0",
                "META-INF/LGPL2.1",
            )
        }
    }
}

dependencies {
    // com.android.test plugin 下：androidTest source set 用 implementation / runtimeOnly
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.androidx.test.rules)
    implementation(libs.espresso.core)
    implementation(libs.junit4)
    runtimeOnly(libs.junit5.jupiter)
    runtimeOnly(libs.junit5.platform.launcher)
}
