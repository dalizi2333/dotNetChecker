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

### 添加其他版本的测试环境

要让 dotNetChecker 针对其他 NeoForge/Minecraft 版本进行测试：

1. **创建目录** `runs/<版本名>/`（例如 `runs/mc1211`）

2. **复制** 现有的测试 `build.gradle.kts` 作为模板，然后修改：
   - `neoForge.version` — 目标 NeoForge 版本
   - `java.toolchain.languageVersion` — 所需的 JDK 版本（默认 Java 21 可省略）
   - `dependsOn(":jar")` 和 JAR 复制逻辑直接复用

   ```kotlin
   // runs/<版本名>/build.gradle.kts — 模板
   plugins {
       id("net.neoforged.moddev") version "2.0.141"
   }
   repositories {
       maven("https://neoforged.forgecdn.net/releases")
       maven("https://neoforged.forgecdn.net/mojang-meta")
       maven("https://libraries.minecraft.net/")
       mavenCentral()
   }
   neoForge {
       version = "<NeoForge 版本>"          // 例如 "21.1.133"
       runs {
           create("server") { server() }
           create("client") { client() }
       }
   }
   tasks.matching { it.name.startsWith("run") }.configureEach {
       dependsOn(":jar")
       doFirst {
           val jarFile = rootDir.resolve("build/libs/dotNetChecker-1.0.0.jar")
           mkdir("run/mods")
           copy { from(jarFile) into("run/mods") }
       }
   }
   ```

3. **注册** 到 `settings.gradle.kts`：
   ```kotlin
   include("runs:mc1211", "runs:mc2612", "runs:<版本名>")
   ```

4. **运行** 子项目任务：
   ```
   .\gradlew.bat :runs:<版本名>:runServer
   ```

5. **（可选）** 在 `.idea/runConfigurations/` 中创建 IntelliJ 运行配置，实现 IDE 工具栏一键启动。

## 许可证

MIT
