# dotNetChecker

一个简单且有点神经的 Minecraft NeoForge MOD，用于检查当前环境是否安装了 .NET Runtime，为其他需要 .NET 运行时的 MOD（如基于 Avalonia.Controls.WebView 的 WebUI MOD）提供环境探测能力。

## 🌟 特点

- **跨平台支持**：Windows / Linux / macOS
- **跨版本兼容**：一个 JAR 包支持所有 Minecraft 版本（原则上支持 1.21.1+，实际可支持更多版本）
- **零依赖**：不引用任何 Minecraft/NeoForge API，纯 Java 实现
- **强制拦截**：检测失败时阻止游戏启动并输出详细错误提示

## 🎯 设计目标

为另一个需要在游戏内显示 Web GUI 的 MOD 提供环境检查：

```
┌─────────────────────────────────────────────────────────────┐
│                    dotNetChecker                            │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  检测 .NET Runtime 是否安装                           │  │
│  │  ├── Windows: dotnet --version / 注册表              │  │
│  │  ├── Linux:   dotnet --version / 文件路径            │  │
│  │  └── macOS:   dotnet --version / 文件路径            │  │
│  └──────────────────────────────────────────────────────┘  │
│                           ↓                                │
│                    输出检测结果到日志                        │
│                           ↓                                │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  另一个 WebUI MOD (使用 .NET)                        │  │
│  │  └── Avalonia.Controls.WebView / .NET MAUI          │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

## 🔧 检测逻辑

### .NET Runtime 检测

| 平台 | 检测方式 | 优先级 |
|------|---------|--------|
| **所有平台** | `dotnet --version` 命令 | 1 |
| **Windows** | 注册表 `HKLM\SOFTWARE\dotnet\Setup\InstalledVersions` | 2 |
| **Linux/macOS** | 检查文件 `/usr/share/dotnet/` 或 `$HOME/.dotnet/` | 2 |

### 检测结果输出

**成功（已安装）：**

```
[dotNetChecker] ============================================
[dotNetChecker] .NET Runtime 检测结果
[dotNetChecker] ============================================
[dotNetChecker] 操作系统: Windows 11
[dotNetChecker] .NET Runtime 版本: 8.0.100
[dotNetChecker] 安装路径: C:\Program Files\dotnet
[dotNetChecker] ============================================
[dotNetChecker] 状态: 已安装 ✓
[dotNetChecker] ============================================
```

**失败（未安装）：**

```
[dotNetChecker] ============================================
[dotNetChecker] ⚠️  .NET Runtime 未检测到！⚠️
[dotNetChecker] ============================================
[dotNetChecker] 操作系统: Windows 11
[dotNetChecker] .NET Runtime 版本: 未安装
[dotNetChecker] ============================================
[dotNetChecker] 请安装 .NET 8.0 或更高版本的运行时环境：
[dotNetChecker] 
[dotNetChecker] 📥 下载地址：
[dotNetChecker]     https://dotnet.microsoft.com/download/dotnet/8.0
[dotNetChecker] 
[dotNetChecker] 💡 安装步骤：
[dotNetChecker]     1. 打开上述链接
[dotNetChecker]     2. 点击 "Download .NET 8.0 Runtime"
[dotNetChecker]     3. 运行安装程序并完成安装
[dotNetChecker]     4. 重启游戏
[dotNetChecker] ============================================
[dotNetChecker] ❌ 游戏启动已被阻止
[dotNetChecker] ============================================
```

## 📁 项目结构

```
dotNetChecker/
├── build.gradle.kts          # Gradle 构建配置（使用 shadowJar）
├── settings.gradle.kts       # Gradle 模块配置
├── gradle.properties         # 项目版本等属性
├── gradlew                   # Gradle Wrapper（Unix）
├── gradlew.bat               # Gradle Wrapper（Windows）
└── src/main/
    ├── java/
    │   └── com/goblint/
    │       ├── DotNetChecker.java          # 主检查类
    │       ├── LaunchWrapperTweaker.java   # LaunchWrapper Tweaker（入口）
    │       └── checker/
    │           └── DotNetCheckerImpl.java  # .NET Runtime 检测实现（跨平台）
    └── resources/
        └── META-INF/
            └── mods.toml                   # NeoForge 元数据
```

## 🚀 工作流程

```
Minecraft 启动
    ↓
加载器读取 MOD JAR
    ↓
发现 LaunchWrapperTweaker（manifest 中声明）
    ↓
Tweaker 初始化时执行 .NET Runtime 检查
    ↓
        ┌──────────────┐
        │   检查通过    │
        ↓              ↓
    .NET 已安装    .NET 未安装
        ↓              ↓
    输出成功日志    输出错误提示
        ↓              ↓
    正常启动游戏    ❌ 阻止启动
```

## 📦 构建

```bash
# 编译打包
./gradlew shadowJar

# 输出 JAR 位置
build/libs/dotNetChecker-1.0.0-all.jar
```

## 📝 元数据

### mods.toml

```toml
modLoader="javafml"
loaderVersion="[47,)"
license="MIT"
issueTrackerURL="https://github.com/dalizi2333/dotNetChecker/issues"
showAsResourcePack=false

[[mods]]
modId="dotnetchecker"
version="1.0.0"
displayName="dotNetChecker"
authors="dalizi2333"
description="检查当前环境是否安装了 .NET Runtime"
```

## 🎮 使用

1. 将 `dotNetChecker-1.0.0-all.jar` 放入 `mods/` 目录
2. 启动游戏
3. 在日志中查看检测结果

## 📄 许可证

MIT License

## 🙏 致谢

- 参考了 [I18nUpdateMod3](https://github.com/CFPAOrg/I18nUpdateMod3) 的跨版本兼容方案
