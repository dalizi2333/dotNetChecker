package com.goblincoders;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.neoforged.fml.common.Mod;

/**
 * NeoForge mod entry point for dotNetChecker.
 * <p>
 * This class has a compile-time dependency on NeoForge API only.
 * The actual .NET detection logic in DotNetCheckerImpl is pure Java with zero dependencies.
 * <p>
 * Register via {@code @Mod} annotation and mods.toml.
 */
@Mod("dotnetchecker")
public class DotNetCheckerMod {

    private static final Logger LOGGER = LogManager.getLogger("dotNetChecker");

    public DotNetCheckerMod() {
        // Perform .NET Runtime check during mod construction
        // This runs very early in the game loading process
        boolean dotNetInstalled = DotNetChecker.check();

        // Also log check results to latest.log (DotNetChecker.check() only uses System.out)
        LOGGER.info(".NET Runtime check complete. Installed: {}", dotNetInstalled);

        if (!dotNetInstalled) {
            block();
        }
    }

    private static void block() {
        // Log each banner line at ERROR level so it appears in latest.log
        LOGGER.error("");
        LOGGER.error("  +----------------------------------------------------------+");
        LOGGER.error("  |                    GAME LAUNCH BLOCKED                    |");
        LOGGER.error("  +----------------------------------------------------------+");
        LOGGER.error("  |                                                          |");
        LOGGER.error("  |  .NET Runtime 8.0+ is required but was not found.        |");
        LOGGER.error("  |                                                          |");
        LOGGER.error("  |  Download: https://dotnet.microsoft.com/download/dotnet  |");
        LOGGER.error("  |                                                          |");
        LOGGER.error("  +----------------------------------------------------------+");
        LOGGER.error("");
        // Also print to stdout so it's visible in terminal (redundant but harmless)
        System.out.println("  [dotNetChecker] GAME LAUNCH BLOCKED: .NET Runtime 8.0+ not found.");
        System.out.println("  [dotNetChecker] Download: https://dotnet.microsoft.com/download/dotnet");
        // Throw to let NeoForge handle the shutdown properly (no zombie processes)
        throw new RuntimeException(".NET Runtime 8.0+ not found - game launch blocked.");
    }
}
