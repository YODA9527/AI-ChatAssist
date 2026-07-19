# 踩坑记录（TROUBLESHOOTING）

> 记录 ChatAssist 开发过程中遇到的所有坑、错误、修复方案。
> **遇到新坑必填** — 节省后来人（包括初夏自己）的时间。
> 维护原则：宁可冗长也不要省略，复现步骤必须能照着做。

---

## 坑 #1 — Hilt 2.52 + KSP2 兼容性 BUG

### 现象

构建 `:app:kspDebugKotlin` 任务失败，错误：

```
e: [ksp] [Hilt] Expected @AndroidEntryPoint to have a value. Did you forget to apply the Gradle Plugin? (com.google.dagger.hilt.android)
[Hilt] Processing did not complete. See error above for details.
e: [ksp] [Hilt] Expected @HiltAndroidApp to have a value. Did you forget to apply the Gradle Plugin? (com.google.dagger.hilt.android)
```

### 复现步骤

1. AGP 8.6.1 + Kotlin 2.0.21 + Hilt 2.52 + KSP `2.0.21-1.0.27`
2. 在 `gradle.properties` 设置 `ksp.useKSP2=true`
3. 跑 `./gradlew :app:assembleDebug`

### 根因

Hilt 2.52 的 KSP 处理器尚未完全适配 KSP2，导致注解元数据无法正确读取，
KSP2 模式下认为 `@HiltAndroidApp` / `@AndroidEntryPoint` 没有传 value。

**已知问题跟踪**：[Dagger Issue #4258](https://github.com/google/dagger/issues/4258)

### 修复方案

在 `gradle.properties` 中关闭 KSP2，回退到 KSP1：

```properties
# KSP2 暂未启用：Hilt 2.52 + KSP2 存在兼容性问题
ksp.useKSP2=false
```

### 验证

修复后重新跑 `./gradlew :app:assembleDebug`，应能通过 `:app:kspDebugKotlin` 任务。

### 后续跟踪

- 关注 Hilt 后续版本（2.53+）是否修复
- 关注 Dagger Issue #4258 状态
- 修复后可移除 `ksp.useKSP2=false` 设置

---

## 坑 #2 — Gradle 配置缓存并发冲突

### 现象

启动第二个 Gradle 进程时失败：

```
Timeout waiting to lock Configuration Cache (F:\Chat AI\ChatAssist\.gradle\configuration-cache).
It is currently in use by another Gradle instance.
Owner PID: 23128
Lock file: F:\Chat AI\ChatAssist\.gradle\configuration-cache\configuration-cache.lock
```

### 复现步骤

1. 第一次 `./gradlew :app:assembleDebug` 跑超时被取消
2. 第二次启动 Gradle 进程
3. 锁文件残留导致冲突

### 根因

Gradle 8.10.2 的 Configuration Cache 锁文件在异常退出后没有清理。

### 修复方案

**方法 1**（推荐）：杀掉残留进程后重试

```bash
taskkill /F /PID <owner_pid>
```

**方法 2**：删除配置缓存目录

```bash
rm -rf .gradle/configuration-cache/
```

**方法 3**：临时禁用配置缓存

```bash
./gradlew :app:assembleDebug --no-configuration-cache
```

### 预防

- 超时取消 Gradle 进程前先确认 daemon 已退出
- 避免同时启动多个 Gradle 实例操作同一项目

---

## 坑 #3 — AGP 9.0 deprecation: `android.defaults.buildfeatures.buildconfig`

### 现象

构建日志出现 WARNING：

```
WARNING: The option setting 'android.defaults.buildfeatures.buildconfig=true' is deprecated.
The current default is 'false'.
It will be removed in version 9.0 of the Android Gradle plugin.
```

### 复现步骤

在 `gradle.properties` 设置 `android.defaults.buildfeatures.buildconfig=true`。

### 修复方案

在模块级 `build.gradle.kts` 显式启用：

```kotlin
android {
    buildFeatures {
        buildConfig = true
    }
}
```

然后从 `gradle.properties` 删除 `android.defaults.buildfeatures.buildconfig=true` 行。

### 后续

AGP 9.0 后此设置将完全废弃，必须在模块级配置。

---

## 坑 #4 — `nohup ./gradlew ... &` 在 Git Bash 下会阻塞

### 现象

```bash
nohup ./gradlew :app:assembleDebug > build.log 2>&1 &
```

执行后 shell 不返回，控制台被卡住。

### 根因

Git Bash 的 job control 与 nohup 配合有 bug，stderr 仍然绑定到终端。

### 修复方案

**方法 1**：用 `disown` 解除控制

```bash
nohup ./gradlew :app:assembleDebug > build.log 2>&1 &
disown
```

**方法 2**（推荐）：用 PowerShell `Start-Process`

```powershell
Start-Process -FilePath 'gradlew.bat' `
  -ArgumentList ':app:assembleDebug' `
  -WorkingDirectory 'f:\Chat AI\ChatAssist' `
  -RedirectStandardOutput 'f:\Chat AI\build.log' `
  -RedirectStandardError 'f:\Chat AI\build.err' `
  -WindowStyle Hidden -PassThru
```

### 经验

- Windows Git Bash 下后台启动 Gradle 优先用 PowerShell
- 监控进度直接 `cat build.log` 或 `tail -f build.log`

---

## 坑 #5 — OkHttp 4 `MediaType.parse()` 已废弃

### 现象

```log
e: ConnectionTester.kt:76:58 'fun parse(mediaType: String): MediaType?' is deprecated. moved to extension function.
```

整个 `:app:compileDebugKotlin` 失败（detekt/ktlint 把 deprecated 当 error）。

### 复现步骤

使用 OkHttp 4.x，调用 `okhttp3.MediaType.parse("application/json")`。

### 修复方案

改用 `String.toMediaType()` 扩展函数（Kotlin 风格）：

```kotlin
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

private val JSON_MEDIA_TYPE = "application/json".toMediaType()

Request.Builder()
    .url(url)
    .post(body.toRequestBody(JSON_MEDIA_TYPE))
    .build()
```

### 后续

OkHttp 5+ 会彻底移除静态 `parse()`，所有项目都应尽早迁移到扩展函数。

---

## 坑 #6 — `MutableStateFlow<T>` 协变推断陷阱

### 现象

```log
e: SettingsViewModel.kt:40:39 Assignment type mismatch:
   actual type is 'com.chatassist.presentation.settings.SettingsUiState.Loaded',
   but 'com.chatassist.presentation.settings.SettingsUiState.Loading' was expected.
```

### 根因

```kotlin
// ❌ 错误写法
val state: StateFlow<SettingsUiState> = MutableStateFlow(SettingsUiState.Loading).also { state ->
    viewModelScope.launch {
        configFlow.collect { cfg ->
            state.value = SettingsUiState.Loaded(cfg)  // 类型错误
        }
    }
}.asStateFlow()
```

`MutableStateFlow(SettingsUiState.Loading)` 的类型参数被协变推断为 `Loading`，
后续 `state.value = SettingsUiState.Loaded(cfg)` 自然不匹配。

### 修复方案

显式标注 `MutableStateFlow` 的类型参数，并在 init 块中启动协程：

```kotlin
// ✅ 正确写法
private val _state = MutableStateFlow<SettingsUiState>(SettingsUiState.Loading)
val state: StateFlow<SettingsUiState> = _state.asStateFlow()

init {
    viewModelScope.launch {
        configRepo.observeAll().collect { cfg ->
            _state.value = SettingsUiState.Loaded(cfg)
        }
    }
}
```

### 教训

- 涉及 sealed interface / class 的 `MutableStateFlow`，**永远显式标注泛型**
- 不要用 `also { }` 闭包去启动协程，init 块更清晰

---

## 坑 #7 — Hilt 循环依赖 `ConfigRepository ↔ ModelApiRepository`

### 现象

```
e: [Dagger/DependencyCycle] Found a dependency cycle:
    com.chatassist.domain.repository.ConfigRepository is injected at
        com.chatassist.data.repository.ModelApiRepositoryImpl(configRepository, …)
    com.chatassist.data.repository.ModelApiRepositoryImpl is injected at
        com.chatassist.data.llm.di.NetworkBindModule.bindModelApiRepository(impl)
    com.chatassist.domain.repository.ModelApiRepository is injected at
        com.chatassist.data.local.ConfigRepositoryImpl(…, modelApi)
    com.chatassist.data.local.ConfigRepositoryImpl is injected at
        com.chatassist.data.local.di.RepositoryBindModule.bindConfigRepository(impl)
    com.chatassist.domain.repository.ConfigRepository is injected at
        com.chatassist.data.repository.ModelApiRepositoryImpl(configRepository, …)
```

`:app:hiltJavaCompileDebug` FAILED。

### 复现步骤

1. `ModelApiRepositoryImpl` 直接注入 `ConfigRepository` 读配置
2. `ConfigRepositoryImpl` 注入 `ModelApiRepository` 调用 `testConnection()`
3. 两者互相依赖，Dagger 报 cycle

### 根因

`ConfigRepository.testConnection()` 委托给 `ModelApiRepository.testConnection()`（v0.3 统一网络出口），
而 `ModelApiRepository` 自身又需要从 `ConfigRepository` 读 `apiUrl/apiKey/model`。
双向依赖，Dagger 构造期无法解决。

### 修复方案

把其中一边改为**延迟注入**。Hilt 原生支持 `javax.inject.Provider<T>`：

```kotlin
@Singleton
class ModelApiRepositoryImpl @Inject constructor(
    private val configRepoProvider: Provider<ConfigRepository>,  // ← 关键：延迟注入
    private val serviceFactory: OpenAiApiServiceFactory,
    private val streamingExecutor: StreamingCallExecutor,
    private val whitelist: WhitelistInterceptor,
) : ModelApiRepository {
    override fun call(prompt: BuiltPrompt): Flow<ModelCallEvent> = flow {
        val cfg = configRepoProvider.get().snapshot()  // 用时再 .get()
        ...
    }
}
```

Dagger 不会在构造期解析 Provider 内部，所以 cycle 解除。

### 验证

- `./gradlew :app:hiltJavaCompileDebug` 通过
- `./gradlew :app:assembleDebug` 通过
- `./gradlew :app:test` 17/17 通过

### 后续跟踪

- 替代方案：`dagger.Lazy<T>`（语义等价但 API 不友好）
- 终极方案：把 `testConnection` 从 `ConfigRepository` 接口移除，改为 `SettingsViewModel` 直接注入 `ModelApiRepository`（更彻底解耦，但破坏 v0.2 接口契约）
- 当前选 Provider 是「最小破坏 + 立即解决」的折中

---

## 坑 #8 — JUnit 5 + Android Library 静默 0 tests

### 现象

```
> Task :app:testDebugUnitTest
BUILD SUCCESSFUL
```

但 `app/build/reports/tests/testDebugUnitTest/index.html` 显示：

```html
<div class="counter">0</div>
<p>tests</p>
```

跑了 0 个测试，但没报错。

### 复现步骤

1. `build.gradle.kts` 加 `testImplementation(libs.junit5.jupiter)` + `kotlin-test-junit5`
2. 写测试类（`@Test` 是 jupiter 的）
3. 跑 `./gradlew :app:test` → BUILD SUCCESSFUL，但实际 0 tests

### 根因

Android Library 默认走 **JUnit 4** 平台跑单测。JUnit 5 测试在 JUnit 4 平台上**不会被发现也不会报错**，静默忽略。

需要显式声明用 JUnit Platform 引擎：

### 修复方案

在 `app/build.gradle.kts` 加：

```kotlin
android {
    ...
    testOptions {
        unitTests.all {
            it.useJUnitPlatform()  // 切到 JUnit 5
        }
    }
}
```

同时确认依赖完整：
- `testImplementation(libs.junit5.jupiter)` （aggregator）
- `testImplementation(libs.kotlin.test.junit5)` （kotlin.test.* 桥接）
- `testRuntimeOnly(libs.junit5.platform.launcher)` （Gradle 跑 JUnit5 必需，2024+ 必须显式声明）

### 验证

- 跑 `./gradlew :app:test` → `testDebugUnitTest` 任务真正执行
- 报告 `index.html` 显示 `17 tests, 0 failures`
- 控制台 `> Task :app:testDebugUnitTest` 后不再静默

### 后续跟踪

- AGP 9.0 后此 API 可能改动，但目前稳定
- 也可考虑用 `androidx.test.ext:junit` 做仪器测试侧的 JUnit5（v0.13 引入）

---

## 坑 #9 — mockk `every` 不支持 suspend 函数

### 现象

```kotlin
val configRepo = mockk<ConfigRepository>()
every { configRepo.snapshot() } returns sampleConfig()  // 编译报错
```

```
e: None of the following candidates is applicable for 'every'
e: Suspension functions can be called only within a coroutine body
```

### 根因

`ConfigRepository.snapshot()` 是 `suspend fun`。
mockk 的 `every` 块内不接受 suspend lambda，必须用 `coEvery`。

### 修复方案

```kotlin
import io.mockk.coEvery

val configRepo = mockk<ConfigRepository>()
coEvery { configRepo.snapshot() } returns sampleConfig()  // ✅
```

被 mock 的函数如果是 suspend，必须用 `coEvery` / `coVerify`。
对应地，配置 stub 的返回如果需要 suspend 计算，也用 `coAnswers`。

### 验证

- 编译通过
- 17 个测试全绿

### 后续跟踪

- mockk 2.x 有 `MockK` 注解处理器，未来可能简化
- 注意区分：`coEvery` 仅在 stub suspend 函数时用，普通函数仍用 `every`

---

## 坑清单索引

| # | 一句话 | 状态 |
|---|--------|------|
| #1 | Hilt 2.52 + KSP2 兼容性 BUG | 临时绕过（关 KSP2） |
| #2 | Gradle 配置缓存并发冲突 | 已解决（杀进程或删锁） |
| #3 | AGP 9.0 deprecation: `android.defaults.buildfeatures.buildconfig` | 已迁移到模块级配置 |
| #4 | `nohup ./gradlew ... &` 在 Git Bash 下阻塞 | 改用 PowerShell Start-Process |
| #5 | OkHttp 4 `MediaType.parse()` 已废弃 | 已迁移到 `String.toMediaType()` |
| #6 | `MutableStateFlow<T>` 协变推断陷阱 | 显式标注泛型 + init 块 |
| #7 | Hilt 循环依赖 `ConfigRepository ↔ ModelApiRepository` | 已用 `Provider<T>` 打破 |
| #8 | JUnit 5 + Android Library 静默 0 tests | 已加 `useJUnitPlatform()` |
| #9 | mockk `every` 不支持 suspend 函数 | 已用 `coEvery` 替换 |

---

_初夏 2026-07-18 维护 — 遇到任何坑都加进来！_
