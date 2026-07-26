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

### Adding Test Environments for Other Versions

To test dotNetChecker against other NeoForge/Minecraft versions:

1. **Create the directory** `runs/<version-name>/` (e.g. `runs/mc1211`)

2. **Copy** `runs/mc1211/build.gradle.kts` as template, then adapt:
   - `neoForge.version` — the target NeoForge version (e.g. `"21.1.133"` or `"26.1.2.53-beta"`)
   - Optional: add `java { toolchain { languageVersion = JavaLanguageVersion.of(N) } }` (omit for default Java 21)
   - **Keep** the `runs { all { ... } }` block — it reads `-PdotNetChecker.jvmArgs`, which powers the `-TestFailVersion` test feature
   - **Keep** `dependsOn(":jar")`, the JAR existence check, and the copy logic

   Full template (matches `runs/mc1211/build.gradle.kts`):

   ```kotlin
   plugins {
       id("net.neoforged.moddev") version "2.0.141"
   }

   repositories.clear()
   repositories {
       mavenLocal()
       maven("https://neoforged.forgecdn.net/releases")
       maven("https://neoforged.forgecdn.net/mojang-meta")
       maven("https://libraries.minecraft.net/")
       maven("https://maven.aliyun.com/repository/public/")
       maven("https://maven.aliyun.com/repository/central/")
       mavenCentral()
   }

   // Uncomment if the target needs a JDK other than Java 21:
   // java {
   //     toolchain {
   //         languageVersion = JavaLanguageVersion.of(25)
   //     }
   // }

   neoForge {
       version = "<NeoForge version>"  // e.g. "21.1.133" or "26.1.2.53-beta"

       runs {
           all {
               // Supports -PdotNetChecker.jvmArgs passthrough (required by -TestFailVersion)
               val extraJvmArgs = project.findProperty("dotNetChecker.jvmArgs") as? String
               if (!extraJvmArgs.isNullOrBlank()) {
                   jvmArguments.addAll(extraJvmArgs.split("\\s+".toRegex()).filter { it.isNotBlank() })
               }
           }
           create("server") { server() }
           create("client") { client() }
       }
   }

   // Auto-build main project JAR and copy to run/mods/
   tasks.matching { it.name.startsWith("run") }.configureEach {
       dependsOn(":jar")
       doFirst {
           val jarFile = rootDir.resolve("build/libs/dotNetChecker-1.0.0.jar")
           if (!jarFile.exists()) {
               throw GradleException("dotNetChecker JAR not found at ${jarFile.absolutePath}. Build the main project first.")
           }
           mkdir("run/mods")
           copy {
               from(jarFile)
               into("run/mods")
           }
           logger.lifecycle("Copied dotNetChecker JAR to run/mods/")
       }
   }
   ```

3. **Register** in `settings.gradle.kts`:
   ```kotlin
   include("runs:mc1211", "runs:mc2612", "runs:<version-name>")
   ```

4. **Run** via subproject task:
   ```
   .\gradlew.bat :runs:<version-name>:runServer
   ```

5. **(Optional)** Create IntelliJ run configurations in `.idea/runConfigurations/` for one-click launch from the IDE toolbar.

## License

MIT
