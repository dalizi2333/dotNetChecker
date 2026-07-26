plugins {
    id("net.neoforged.moddev") version "2.0.141"
}

// 测试环境只需要能运行 Minecraft Server，不需要自己的源代码
// dotNetChecker 的 JAR 由 test.ps1 或 Gradle task 复制到 run/mods/

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

neoForge {
    version = "21.1.133"

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

// 自动构建主项目 JAR 并复制到 run/mods/
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
