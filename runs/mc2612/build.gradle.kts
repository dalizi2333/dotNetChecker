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

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

neoForge {
    version = "26.1.2.53-beta"

    runs {
        all {
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

// 自动将主项目构建的 dotNetChecker JAR 复制到 run/mods/
tasks.matching { it.name.startsWith("run") }.configureEach {
    doFirst {
        val jarFile = rootDir.parentFile.parentFile.resolve("build/libs/dotNetChecker-1.0.0.jar")
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
