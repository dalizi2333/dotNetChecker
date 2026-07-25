# dotNetChecker

A NeoForge mod that checks if the .NET Runtime is installed before the game launches, and allows other mods to declare their .NET version requirements.

## Features

- **Early Detection** -- checks for .NET Runtime during mod construction, before any game content loads
- **No SDK Required** -- detects runtime-only installs via `dotnet --list-runtimes`, with filesystem scan fallback
- **Cross-Version** -- works on NeoForge 21.1 (MC 1.21.1, Java 21) and NeoForge 26.1.2 (MC 26.1.2, Java 25)
- **Registry API** -- other mods can register their minimum required version; dotNetChecker validates all requirements in one pass
- **Clear Feedback** -- block screen in `latest.log` shows installed version, required version, and which mod requested it

## How It Works

dotNetChecker uses a two-phase approach to handle NeoForge's parallel mod construction:

| Phase | When | What |
|---|---|---|
| **Phase 1** | `@Mod` constructor | Detect .NET Runtime version, log result |
| **Phase 2** | `FMLCommonSetupEvent` | Validate all registered version requirements, block if unmet |

Other mods register their requirements during their own constructor (Phase 1). Since all constructors finish before `FMLCommonSetupEvent` fires, the registry is complete by Phase 2.

## Usage for Other Modders

If your mod depends on .NET Runtime, register your minimum version in your `@Mod` constructor:

```java
import com.goblincoders.checker.DotNetRuntimeRegistry;

@Mod("mymod")
public class MyMod {
    public MyMod() {
        DotNetRuntimeRegistry.registerRequiredVersion("mymod", "8.0.0");
    }
}
```

No need to worry about calling order -- dotNetChecker collects all registrations and validates them after every mod has constructed.

### Adding as a Dependency

dotNetChecker is not yet on any Maven repository. Until then, use [JitPack](https://jitpack.io/) to build directly from GitHub:

```kotlin
repositories {
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("com.github.dalizi2333:dotNetChecker:main-SNAPSHOT")
}
```

## Building

```powershell
.\gradlew.bat jar
```

The JAR is output to `build/libs/dotNetChecker-1.0.0.jar`.

## Testing

### Prerequisites

- Java 21+ (for mc1211 test environment)
- Java 25+ (for mc2612 test environment)
- Gradle auto-resolves the correct JDK via toolchain; set `JAVA_HOME` to a compatible version

### Quick Test

```powershell
.\test.ps1 mc1211 server
.\test.ps1 mc2612 server
```

### Version Mismatch Test

Simulate another mod requiring a higher version than what's installed:

```powershell
.\test.ps1 mc1211 server -TestFailVersion 99.0.0
```

This injects a requirement of `>= 99.0.0` from a fake mod `testrunner`, causing the validation to fail and display the block screen.

## License

MIT
