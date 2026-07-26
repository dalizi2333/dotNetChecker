---
name: "add-dotnetchecker-test-env"
description: "Adds a new NeoForge/Minecraft test environment for the dotNetChecker project. Invoke when user wants to test dotNetChecker against a different Minecraft version or NeoForge release."
---

# Add dotNetChecker Test Environment

This skill adds a new test environment for the dotNetChecker mod project against a different NeoForge/Minecraft version. It handles all files: subproject build script, root settings registration, test script update, and optional IntelliJ run configurations.

## Prerequisites

- The `dotNetChecker` project root is the current working directory
- The target NeoForge version and its required JDK version are known
- Existing test environments at `runs/mc1211/` and `runs/mc2612/` serve as reference

## Steps

### 1. Create the subproject directory

Create `runs/<version-name>/` with a `build.gradle.kts`.

**Version naming convention:**
- Use `mc<major><minor>` for MC versions (e.g. `mc1211` for MC 1.21.1, `mc2612` for MC 26.1.2)
- Use `mc<year><release>` for the new year-based scheme (e.g. `mc2601` for MC 26.0.1)

### 2. Write `runs/<version-name>/build.gradle.kts`

Use the following as a template, adapting:

- **`neoForge.version`** — the exact NeoForge version string (e.g. `"21.1.133"`, `"26.1.2.53-beta"`)
- **`java.toolchain.languageVersion`** — required JDK version; omit for default Java 21, add `JavaLanguageVersion.of(N)` for other versions

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

// Only add if the required JDK differs from Java 21:
// java {
//     toolchain {
//         languageVersion = JavaLanguageVersion.of(25)
//     }
// }

neoForge {
    version = "<NeoForge version>"  // e.g. "21.1.133" or "26.1.2.53-beta"

    runs {
        all {
            // Support passing extra JVM arguments via Gradle property:
            //   -PdotNetChecker.jvmArgs="-Ddotnetchecker.test.requireVersion=99.0.0 -Ddotnetchecker.test.requireModId=testrunner"
            val extraJvmArgs = project.findProperty("dotNetChecker.jvmArgs") as? String
            if (!extraJvmArgs.isNullOrBlank()) {
                jvmArguments.addAll(extraJvmArgs.split("\\s+".toRegex()).filter { it.isNotBlank() })
            }
        }
        create("server") {
            server()
        }
        create("client") {
            client()
        }
    }
}

// Auto-build the main project JAR and copy it to run/mods/
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

### 3. Register in root `settings.gradle.kts`

Add the new version name to the `include(...)` line in `settings.gradle.kts`:

```kotlin
include("runs:mc1211", "runs:mc2612", "runs:<version-name>")
```

### 4. (Optional) Create IntelliJ run configurations

Create XML files in `.idea/runConfigurations/` for one-click launch in IntelliJ IDEA.

**Server run config** — `.idea/runConfigurations/<version-name>___Server.xml`:
```xml
<component name="ProjectRunConfigurationManager">
  <configuration default="false" name="<version-name> - Server" type="GradleRunConfiguration" factoryName="Gradle">
    <ExternalSystemSettings>
      <option name="executionName" />
      <option name="externalProjectPath" value="$PROJECT_DIR$" />
      <option name="externalSystemIdString" value="GRADLE" />
      <option name="scriptParameters" value="" />
      <option name="taskDescriptions"><list /></option>
      <option name="taskNames"><list><option value=":runs:<version-name>:runServer" /></list></option>
      <option name="vmOptions" value="" />
    </ExternalSystemSettings>
  </configuration>
</component>
```

**Client run config** — `.idea/runConfigurations/<version-name>___Client.xml`:
```xml
<component name="ProjectRunConfigurationManager">
  <configuration default="false" name="<version-name> - Client" type="GradleRunConfiguration" factoryName="Gradle">
    <ExternalSystemSettings>
      <option name="executionName" />
      <option name="externalProjectPath" value="$PROJECT_DIR$" />
      <option name="externalSystemIdString" value="GRADLE" />
      <option name="scriptParameters" value="" />
      <option name="taskDescriptions"><list /></option>
      <option name="taskNames"><list><option value=":runs:<version-name>:runClient" /></list></option>
      <option name="vmOptions" value="" />
    </ExternalSystemSettings>
  </configuration>
</component>
```

**Fail test run config** — `.idea/runConfigurations/<version-name>___Server__Fail_Test_.xml`:
```xml
<component name="ProjectRunConfigurationManager">
  <configuration default="false" name="<version-name> - Server (Fail Test)" type="GradleRunConfiguration" factoryName="Gradle">
    <ExternalSystemSettings>
      <option name="executionName" />
      <option name="externalProjectPath" value="$PROJECT_DIR$" />
      <option name="externalSystemIdString" value="GRADLE" />
      <option name="scriptParameters" value="-PdotNetChecker.jvmArgs=&quot;-Ddotnetchecker.test.requireVersion=99.0.0 -Ddotnetchecker.test.requireModId=testrunner&quot;" />
      <option name="taskDescriptions"><list /></option>
      <option name="taskNames"><list><option value=":runs:<version-name>:runServer" /></list></option>
      <option name="vmOptions" value="" />
    </ExternalSystemSettings>
  </configuration>
</component>
```

### 5. (Optional) Update `test.ps1`

Add the new version name to the `ValidateSet` parameter at the top of `test.ps1`:

```powershell
[Parameter(Position = 0)]
[ValidateSet("mc1211", "mc2612", "<version-name>")]
[string]$Version = "mc1211",
```

### 6. Verify the build

Run from the project root:

```
.\gradlew.bat :runs:<version-name>:tasks
```

Confirm `runServer` and `runClient` appear under "Mod development tasks".

## Verifying the Result

- `.\gradlew.bat :runs:<version-name>:runServer` should launch the server successfully (blocking if no .NET runtime detected)
- `.\gradlew.bat :runs:<version-name>:runClient` should launch a Minecraft client
- IntelliJ should show the new run configurations in the toolbar dropdown after project sync
