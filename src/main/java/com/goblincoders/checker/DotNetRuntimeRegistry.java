package com.goblincoders.checker;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for mods to declare their required .NET Runtime version.
 * <p>
 * Other mods call {@link #registerRequiredVersion(String, String)} during their
 * constructor or early initialization to declare the minimum .NET version they need.
 * The dotNetChecker mod will then validate all registered requirements against
 * the installed .NET Runtime and block the game if any requirement is not met.
 * <p>
 * Thread-safe.
 */
public class DotNetRuntimeRegistry {

    private static final Map<String, String> requirements = new ConcurrentHashMap<>();

    // Default requirement when no mods register (dotNetChecker itself)
    private static final String DEFAULT_MIN_VERSION = "8.0.0";

    private DotNetRuntimeRegistry() {
    }

    /**
     * Register a minimum required .NET Runtime version for a mod.
     * <p>
     * Call this during your mod's constructor or {@code @Mod} constructor.
     *
     * @param modId      the mod's ID (e.g. "mymod")
     * @param minVersion minimum .NET version required, in semver format (e.g. "8.0.0")
     * @throws IllegalArgumentException if modId is null/empty or minVersion is invalid
     */
    public static void registerRequiredVersion(String modId, String minVersion) {
        if (modId == null || modId.isBlank()) {
            throw new IllegalArgumentException("modId must not be null or empty");
        }
        if (minVersion == null || !minVersion.matches("\\d+\\.\\d+(\\.\\d+.*)?")) {
            throw new IllegalArgumentException("Invalid version format: " + minVersion);
        }
        requirements.put(modId, minVersion);
    }

    /**
     * Returns an unmodifiable view of all registered version requirements.
     * Key = modId, Value = minimum version string.
     */
    public static Map<String, String> getRequiredVersions() {
        return Collections.unmodifiableMap(requirements);
    }

    /**
     * Returns the highest minimum version required across all registered mods.
     * Returns {@code "8.0.0"} if no mods have registered (the default minimum).
     */
    public static String getHighestRequiredVersion() {
        String highest = null;
        for (String ver : requirements.values()) {
            if (highest == null || DotNetCheckerImpl.compareVersions(ver, highest) > 0) {
                highest = ver;
            }
        }
        return highest != null ? highest : DEFAULT_MIN_VERSION;
    }

    /**
     * Check if the given installed version meets all registered requirements.
     *
     * @param installedVersion the detected .NET Runtime version
     * @return true if all requirements are satisfied
     */
    public static boolean meetsAllRequirements(String installedVersion) {
        String required = getHighestRequiredVersion();
        return DotNetCheckerImpl.compareVersions(installedVersion, required) >= 0;
    }

    /**
     * Clear all registered requirements (for testing).
     */
    public static void clear() {
        requirements.clear();
    }
}
