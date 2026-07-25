package com.goblincoders;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.goblincoders.checker.DotNetCheckerImpl;
import com.goblincoders.checker.DotNetCheckerImpl.DotNetResult;
import com.goblincoders.checker.DotNetRuntimeRegistry;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * NeoForge mod entry point for dotNetChecker.
 * <p>
 * Detection runs early (constructor), but validation is deferred to
 * {@link FMLCommonSetupEvent} so other mods have a chance to register
 * their version requirements via {@link DotNetRuntimeRegistry}.
 * <p>
 * Other mods use:
 * <pre>{@code
 * DotNetRuntimeRegistry.registerRequiredVersion("mymod", "8.0.0");
 * }</pre>
 * Call this during your mod constructor (before FMLCommonSetupEvent fires).
 */
@Mod("dotnetchecker")
public class DotNetCheckerMod {

    private static final Logger LOGGER = LogManager.getLogger("dotNetChecker");
    private static DotNetResult detectionResult;

    public DotNetCheckerMod() {
        // Phase 1: Detect .NET Runtime (runs early, during mod construction)
        // Do NOT validate registrations yet — other mods may not have registered yet.
        detectionResult = DotNetCheckerImpl.check();

        LOGGER.info(".NET Runtime check complete. Installed: {}, Version: {}",
            detectionResult.installed, detectionResult.version);

        if (detectionResult.installed) {
            LOGGER.info(".NET Runtime {} detected at {}", detectionResult.version, detectionResult.path);
        }

        // Test mode: check for system properties to inject a simulated requirement
        // Set via: -Ddotnetchecker.test.requireVersion=99.0.0 -Ddotnetchecker.test.requireModId=testrunner
        String testVersion = System.getProperty("dotnetchecker.test.requireVersion");
        String testModId = System.getProperty("dotnetchecker.test.requireModId");
        if (testVersion != null && testModId != null) {
            DotNetRuntimeRegistry.registerRequiredVersion(testModId, testVersion);
            LOGGER.warn("TEST MODE: Injected requirement {} >= {} from {}", testModId, testVersion, testModId);
        }
    }

    /**
     * Event subscriber for mod lifecycle events.
     * Registered via annotation, no manual bus registration needed.
     */
    @EventBusSubscriber(modid = "dotnetchecker", bus = Bus.MOD)
    public static class LifecycleEvents {
        @SubscribeEvent
        public static void onCommonSetup(FMLCommonSetupEvent event) {
            // Phase 2: Validate all registrations (after all mod constructors have run)
            // By this point, every mod has had a chance to call registerRequiredVersion().
            if (!detectionResult.installed) {
                String required = DotNetRuntimeRegistry.getHighestRequiredVersion();
                String topMod = findHighestRequester();
                block("not_found", required + (topMod != null ? " (by " + topMod + ")" : ""));
                return;
            }

            String required = DotNetRuntimeRegistry.getHighestRequiredVersion();
            if (!DotNetRuntimeRegistry.meetsAllRequirements(detectionResult.version)) {
                String topMod = findHighestRequester();
                block("version_mismatch",
                    "Installed " + detectionResult.version +
                    " / Required " + required +
                    (topMod != null ? " (by " + topMod + ")" : ""));
                return;
            }

            LOGGER.info(".NET Runtime validation passed. Installed {} >= required {}", 
                detectionResult.version, required);
        }
    }

    private static void block(String reason, String detail) {
        if ("not_found".equals(reason)) {
            LOGGER.error("");
            LOGGER.error("  +----------------------------------------------------------+");
            LOGGER.error("  |                    GAME LAUNCH BLOCKED                    |");
            LOGGER.error("  +----------------------------------------------------------+");
            LOGGER.error("  |                                                          |");
            LOGGER.error("  |  .NET Runtime is required but was not found.             |");
            LOGGER.error("  |  Required: " + padRight(detail != null ? detail : "8.0.0", 47) + " |");
            LOGGER.error("  |                                                          |");
            LOGGER.error("  |  Download: https://dotnet.microsoft.com/download/dotnet  |");
            LOGGER.error("  |                                                          |");
            LOGGER.error("  +----------------------------------------------------------+");
            LOGGER.error("");

            Map<String, String> reqs = DotNetRuntimeRegistry.getRequiredVersions();
            if (!reqs.isEmpty()) {
                LOGGER.error("  The following mods require .NET Runtime:");
                for (Map.Entry<String, String> entry : reqs.entrySet()) {
                    LOGGER.error("    - {} requires >= {}", entry.getKey(), entry.getValue());
                }
                LOGGER.error("");
            }

            System.out.println("  [dotNetChecker] GAME LAUNCH BLOCKED: .NET Runtime not found.");
            System.out.println("  [dotNetChecker] Required: " + (detail != null ? detail : "8.0.0"));
            System.out.println("  [dotNetChecker] Download: https://dotnet.microsoft.com/download/dotnet");
            throw new RuntimeException(
                ".NET Runtime not found. Required: " + (detail != null ? detail : "8.0.0"));
        }

        if ("version_mismatch".equals(reason)) {
            LOGGER.error("");
            LOGGER.error("  +----------------------------------------------------------+");
            LOGGER.error("  |                    GAME LAUNCH BLOCKED                    |");
            LOGGER.error("  +----------------------------------------------------------+");
            LOGGER.error("  |                                                          |");
            LOGGER.error("  |  Installed .NET Runtime version does not meet             |");
            LOGGER.error("  |  requirements.                                           |");
            LOGGER.error("  |                                                          |");
            LOGGER.error("  |  " + padRight(detail != null ? detail : "?", 56) + " |");
            LOGGER.error("  |                                                          |");
            LOGGER.error("  |  Download: https://dotnet.microsoft.com/download/dotnet  |");
            LOGGER.error("  |                                                          |");
            LOGGER.error("  +----------------------------------------------------------+");
            LOGGER.error("");

            Map<String, String> reqs = DotNetRuntimeRegistry.getRequiredVersions();
            if (!reqs.isEmpty()) {
                LOGGER.error("  Mod version requirements:");
                for (Map.Entry<String, String> entry : reqs.entrySet()) {
                    LOGGER.error("    - {} requires >= {}", entry.getKey(), entry.getValue());
                }
                LOGGER.error("");
            }

            System.out.println("  [dotNetChecker] GAME LAUNCH BLOCKED: .NET version mismatch.");
            System.out.println("  [dotNetChecker] " + (detail != null ? detail : ""));
            System.out.println("  [dotNetChecker] Download: https://dotnet.microsoft.com/download/dotnet");
            throw new RuntimeException(
                ".NET Runtime version insufficient: " + (detail != null ? detail : "unknown"));
        }
    }

    private static String padRight(String s, int len) {
        if (s.length() >= len) return s.substring(0, len);
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < len) sb.append(' ');
        return sb.toString();
    }

    /**
     * Find the mod ID that registered the highest required .NET version.
     */
    private static String findHighestRequester() {
        String highestVer = null;
        String highestMod = null;
        for (Map.Entry<String, String> entry : DotNetRuntimeRegistry.getRequiredVersions().entrySet()) {
            String ver = entry.getValue();
            if (highestVer == null || DotNetCheckerImpl.compareVersions(ver, highestVer) > 0) {
                highestVer = ver;
                highestMod = entry.getKey();
            }
        }
        return highestMod;
    }
}
