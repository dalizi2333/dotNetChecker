pluginManagement {
    repositories {
        maven("https://neoforged.forgecdn.net/releases")
        maven("https://maven.aliyun.com/repository/gradle-plugin/")
        maven("https://maven.aliyun.com/repository/public/")
        mavenCentral()
        gradlePluginPortal()
    }
}
rootProject.name = "dotNetChecker"

include("runs:mc1211", "runs:mc2612")
