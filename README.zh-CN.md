# dotNetChecker

一个 NeoForge Mod，在游戏启动前检查 .NET Runtime 是否已安装，并允许其他模组声明自己需要的 .NET 版本。

## 功能

- **早期检测** — 在模组构造阶段检查 .NET Runtime，远早于游戏内容加载
- **无需 SDK** — 通过 `dotnet --list-runtimes` 检测纯运行时安装，降级为文件系统扫描
- **跨版本** — 支持 NeoForge 21.1（MC 1.21.1，Java 21）和 NeoForge 26.1.2（MC 26.1.2，Java 25）
- **注册 API** — 其他模组可以上报所需的最低版本；dotNetChecker 统一校验
- **清晰反馈** — `latest.log` 中显示已安装版本、所需版本和提出要求的模组 ID

## 工作原理

dotNetChecker 采用两阶段策略以应对 NeoForge 的并行模组构造：

| 阶段 | 时机 | 工作内容 |
|---|---|---|
| **阶段 1** | `@Mod` 构造器 | 检测 .NET Runtime 版本，记录日志 |
| **阶段 2** | `FMLCommonSetupEvent` | 校验所有注册的版本需求，不满足则阻断 |

其他模组在自己的构造器（阶段 1）中注册需求。所有构造器执行完毕后才触发 `FMLCommonSetupEvent`，因此阶段 2 时注册表是完整的。

## 其他模组接入

如果你的模组依赖 .NET Runtime，在 `@Mod` 构造器中注册最低版本：

```java
import com.goblincoders.checker.DotNetRuntimeRegistry;

@Mod("mymod")
public class MyMod {
    public MyMod() {
        DotNetRuntimeRegistry.registerRequiredVersion("mymod", "8.0.0");
    }
}
```

无需担心调用顺序——dotNetChecker 会在所有模组构造完成后统一校验。

### 添加依赖

dotNetChecker 尚未发布到 Maven 仓库。在此之前可以通过 [JitPack](https://jitpack.io/) 直接从 GitHub 构建：

```kotlin
repositories {
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("com.github.dalizi2333:dotNetChecker:main-SNAPSHOT")
}
```

## 构建

```powershell
.\gradlew.bat jar
```

JAR 输出到 `build/libs/dotNetChecker-1.0.0.jar`。

## 测试

### 前置条件

- Java 21+（用于 mc1211 测试环境）
- Java 25+（用于 mc2612 测试环境）
- Gradle 通过工具链自动解析对应 JDK；将 `JAVA_HOME` 设为兼容版本即可

### 快速测试

```powershell
.\test.ps1 mc1211 server
.\test.ps1 mc2612 server
```

### 版本不满足测试

模拟其他模组需要比当前更高的版本：

```powershell
.\test.ps1 mc1211 server -TestFailVersion 99.0.0
```

这会注入一个来自假模组 `testrunner` 的 `>= 99.0.0` 版本需求，使校验失败并显示阻断画面。

## 许可证

MIT
