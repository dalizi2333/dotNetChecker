# 调查报告：Linux（CachyOS）上 NeoForge 1.21.1 JPMS 崩溃问题

## 报告人
dalizi233

## 结论
**暂时不会在 Linux 上测试 NeoForge 1.21.1（mc1211）的表现。** 该环境在 Linux 上存在 JPMS 模块系统兼容性问题，不阻塞项目主线开发。

---

## 现象

在 CachyOS Linux 上，运行 NeoForge 1.21.1（ModLauncher 11.0.4 + Java 21）的测试服务器时，在 Mod List 输出后立即崩溃：

```
Exception in thread "main" java.lang.module.ResolutionException:
Module fml_loader reads more than one module named fml_earlydisplay
```

Windows 11 上完全相同的配置（同一项目、同一 Gradle 缓存、同一 Java 21.0.11/21.0.12）正常运行，无此错误。

**该问题与 dotNetChecker Mod 无关**——移除 dotNetChecker 后同样崩溃（错误反转为 `fml_earlydisplay reads more than one module named fml_loader`）。

**该问题为 NeoForge 1.21.1 通用问题**——使用官方 MDK（https://github.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle.git）在 Linux 上同样复现。

---

## 触发条件

### 必要条件
- Linux（CachyOS）
- NeoForge 1.21.1（ModLauncher 11.0.4）
- Java 21（Temurin 21.0.11/21.0.12、GraalVM 21.0.2 均触发）
- **Gradle 缓存目录 `~/.gradle/caches/modules-2/files-2.1` 为符号链接，指向 NTFS 分区**

### 触发场景
| 场景 | 结果 |
|------|------|
| Linux + 符号链接指向 NTFS | ❌ 崩溃 |
| Linux + 无符号链接（btrfs 真实目录） | ✅ 正常 |
| Windows + 符号链接指向 NTFS | ✅ 正常 |

---

## 根因分析

### 直接原因
`earlydisplay-4.0.38.jar` 和 `loader-4.0.38.jar` 同时出现在两处：
1. Java 启动参数 `-cp`（classpath）
2. ModLauncher 的 `-DlegacyClassPath.file`（legacyClasspath）

ModLauncher 的 `ModuleLayerHandler.buildLayer` 从两处加载，JPMS `Resolver.checkExportSuppliers()` 检测到同一模块名被加载两次，报 `ResolutionException`。

### 为什么只有 Linux 触发？
**符号链接 + NTFS 文件系统的路径规范化差异。**

当 `files-2.1` 是符号链接指向 NTFS 分区时：
- **Linux 上**：JPMS 模块系统在解析路径时，可能因符号链接/跨文件系统/路径大小写敏感等因素，将同一 jar 视为两个不同的模块实例
- **Windows 上**：路径处理方式不同（不区分大小写、符号链接机制不同），能正确识别为同一文件

当移除符号链接、缓存直接在 btrfs 上时，Linux 也正常工作，说明问题不在于 Linux 内核本身，而在于**跨文件系统（btrfs → NTFS）的符号链接路径解析**。

### 排除的因素
- ~~Java 版本回归（21.0.12 新引入）~~ → 21.0.11 在 Linux 同样崩
- ~~厂商差异（Temurin vs Corretto）~~ → 同一 Temurin 21.0.11，Windows 不崩 Linux 崩
- ~~dotNetChecker 特有~~ → 官方 MDK 同样复现

---

## 影响范围

| 测试环境 | NeoForge 版本 | 加载器 | Java | 结果 |
|-----------|--------------|--------|------|------|
| mc1211 | 21.1.133 | ModLauncher 11.0.4 | 21 | ❌ Linux 崩 |
| mc2612 | 26.1.2 | FancyModLoader 11.0.13 | 25 | ✅ 正常 |

mc2612（NeoForge 26.1.2）使用不同的加载器（FancyModLoader），不走 `ModuleLayerHandler`，完全不受影响。

---

## 后续计划

- 不在 Linux 上测试 NeoForge 1.21.1
- Linux 上如有特定需求，基于更高版本的 NeoForge（如 26.1.2+）单独开发
- 如果需要在 Linux 上使用 1.21.1，一个可行的 workaround 是：
  - **不在 `~/.gradle/caches/` 下使用指向 NTFS 的符号链接**
  - 或者将 Gradle 缓存目录改为 btrfs 上的真实目录
