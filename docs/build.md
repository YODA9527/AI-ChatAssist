# 构建与签名说明

> ChatAssist 的本地构建、签名、发布流程。
> 面向开发者与发布者。

---

## 1. 环境要求

| 工具 | 版本 | 备注 |
|------|------|------|
| JDK | 17+ | Temurin 推荐 |
| Android SDK | API 26-35 | platform 35 + build-tools 35 |
| Android Studio | Hedgehog (2023.1) 或更新 | 推荐 |
| Gradle | 8.10.2 | 项目自带 wrapper，无需手动装 |
| Kotlin | 2.0.21 | 由 Gradle 插件管理 |

### 1.1 环境变量

```bash
# JDK
JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot

# Android SDK
ANDROID_HOME=F:\DevTools\AndroidSDK
ANDROID_SDK_ROOT=F:\DevTools\AndroidSDK
```

## 2. 克隆与初始化

```bash
git clone https://github.com/<your-name>/ChatAssist.git
cd ChatAssist
```

## 3. 构建命令

### 3.1 Debug 包

```bash
./gradlew :app:assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk
```

### 3.2 Release 包

```bash
./gradlew :app:assembleRelease
# 产物：app/build/outputs/apk/release/app-release.apk
# 需要先配置签名（见 §4）
```

### 3.3 全部构建

```bash
./gradlew assemble
```

### 3.4 清理

```bash
./gradlew clean
```

## 4. 签名配置

### 4.1 生成 keystore

```bash
keytool -genkey -v \
  -keystore chatassist-release.jks \
  -alias chatassist \
  -keyalg RSA -keysize 2048 \
  -validity 10000
```

### 4.2 配置签名（不提交到 Git）

创建 `keystore.properties`（**已在 .gitignore 中**）：

```properties
RELEASE_STORE_FILE=/path/to/chatassist-release.jks
RELEASE_STORE_PASSWORD=your-store-password
RELEASE_KEY_ALIAS=chatassist
RELEASE_KEY_PASSWORD=your-key-password
```

### 4.3 在 `app/build.gradle.kts` 引用

```kotlin
// 在 android { ... } 块内
signingConfigs {
    create("release") {
        val keystoreProperties = Properties().apply {
            load(rootProject.file("keystore.properties").inputStream())
        }
        storeFile = file(keystoreProperties["RELEASE_STORE_FILE"] as String)
        storePassword = keystoreProperties["RELEASE_STORE_PASSWORD"] as String
        keyAlias = keystoreProperties["RELEASE_KEY_ALIAS"] as String
        keyPassword = keystoreProperties["RELEASE_KEY_PASSWORD"] as String
    }
}

buildTypes {
    release {
        signingConfig = signingConfigs.getByName("release")
        // ... 其他配置
    }
}
```

## 5. 测试

### 5.1 单元测试

```bash
./gradlew :app:testDebugUnitTest
```

### 5.2 仪器测试（需要设备/模拟器）

```bash
./gradlew :app:connectedDebugAndroidTest
```

### 5.3 Lint

```bash
./gradlew :app:lintDebug
# 报告：app/build/reports/lint-results-debug.html
```

### 5.4 Detekt

```bash
./gradlew :app:detekt
```

## 6. 性能测试

```bash
./gradlew :app:assembleBenchmark
./gradlew :app:pixel6Api31BenchmarkAndroidTest
# 或
./gradlew :app:benchmarkAndroidTest
```

## 7. 常见构建问题

### 7.1 Gradle Wrapper 下载慢
- 配置国内镜像（`~/.gradle/init.gradle.kts`）
- 或使用代理

### 7.2 KSP 报错 "Did you forget to apply the Gradle Plugin?"
- Hilt 2.52 + KSP2 存在兼容性问题
- 解决：在 `gradle.properties` 设置 `ksp.useKSP2=false`
- 详见 [TROUBLESHOOTING.md](./TROUBLESHOOTING.md)

### 7.3 配置缓存冲突
- 删除 `.gradle/configuration-cache/` 后重试
- 或在 `gradle.properties` 关闭 `org.gradle.configuration-cache=true`

### 7.4 依赖下载失败
- 检查网络
- 配置镜像源（阿里云 / 腾讯云 Maven 镜像）

## 8. 发布到 GitHub

```bash
# 打 tag
git tag -a v0.1.0 -m "v0.1.0 工程骨架"
git push origin v0.1.0

# GitHub Actions 自动构建并发布 Release
# 或手动上传 app-release.apk
```

## 9. CI/CD 规划

- [ ] GitHub Actions 自动化 build / test / lint
- [ ] 自动生成 Release APK 并附到 GitHub Release
- [ ] 自动跑 Paparazzi 截图比对
- [ ] 自动跑 Macrobenchmark
