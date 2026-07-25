plugins {
    id("net.neoforged.moddev") version "2.0.115"
    java
}

group = project.properties["group"].toString()
version = project.properties["version"].toString()

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
    withSourcesJar()
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

// 清除 NeoForge 插件自动添加的 maven.neoforged.net，替换为 forgecdn 镜像
repositories.clear()
repositories {
    mavenLocal()
    maven("https://maven.aliyun.com/repository/public/")
    maven("https://maven.aliyun.com/repository/central/")
    maven("https://maven.aliyun.com/repository/gradle-plugin/")
    mavenCentral()
    maven("https://libraries.minecraft.net/")
    maven("https://neoforged.forgecdn.net/releases")
    maven("https://neoforged.forgecdn.net/mojang-meta")
}

configurations {
    implementation {
        isTransitive = false
    }
}

neoForge {
    version = "21.1.133"
    validateAccessTransformers = true

    mods {
        register("dotnetchecker") {
            sourceSet(sourceSets.main.get())
        }
    }
}

dependencies {
    compileOnly("net.minecraft:launchwrapper:1.12")
}

// 测试在单独的 test 项目中运行，此处禁用 run 任务
neoForge {
    runs {
        // 空块，不生成 runClient/runServer
    }
}

tasks.jar {
    manifest {
        attributes(
            "TweakClass" to "com.goblincoders.LaunchWrapperTweaker",
            "TweakOrder" to 33,
        )
    }
    archiveBaseName.set("dotNetChecker")
}

tasks.processResources {
    filesMatching("META-INF/neoforge.mods.toml") {
        expand("version" to project.version)
    }
}
