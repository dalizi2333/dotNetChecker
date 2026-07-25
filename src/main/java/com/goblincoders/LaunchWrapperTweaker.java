package com.goblincoders;

import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;

import java.io.File;
import java.util.List;

/**
 * LaunchWrapper Tweaker - entry point for the mod.
 * <p>
 * This class is loaded by Minecraft's LaunchWrapper system before any game code runs.
 * It performs the .NET Runtime check and blocks game startup if .NET is not installed.
 * <p>
 * The TweakClass is declared in the JAR's MANIFEST.MF:
 * {@code TweakClass: com.goblincoders.LaunchWrapperTweaker}
 */
public class LaunchWrapperTweaker implements ITweaker {

    @Override
    public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {
        // No options to process for this simple checker mod
    }

    @Override
    public void injectIntoClassLoader(LaunchClassLoader classLoader) {
        // Perform .NET Runtime check early in the loading process
        boolean dotNetInstalled = DotNetChecker.check();

        if (!dotNetInstalled) {
            throw new RuntimeException(
                ".NET Runtime 未安装！游戏无法启动。\n" +
                "请安装 .NET 8.0 或更高版本：https://dotnet.microsoft.com/download/dotnet/8.0"
            );
        }
    }

    @Override
    public String getLaunchTarget() {
        // Return null to use the default launch target
        return null;
    }

    @Override
    public String[] getLaunchArguments() {
        return new String[0];
    }
}
