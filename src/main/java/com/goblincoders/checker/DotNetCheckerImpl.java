package com.goblincoders.checker;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * .NET Runtime detection implementation - cross-platform, zero dependency.
 * Supports Windows, Linux, and macOS.
 */
public class DotNetCheckerImpl {

    private static final String OS_NAME = System.getProperty("os.name", "unknown").toLowerCase(Locale.ROOT);
    private static final String OS_ARCH = System.getProperty("os.arch", "unknown");

    /**
     * Result of .NET Runtime detection.
     */
    public static class DotNetResult {
        public final boolean installed;
        public final String version;
        public final String path;
        public final String errorMessage;

        public DotNetResult(boolean installed, String version, String path, String errorMessage) {
            this.installed = installed;
            this.version = version;
            this.path = path;
            this.errorMessage = errorMessage;
        }
    }

    /**
     * Check if .NET Runtime is installed on the current system.
     */
    public static DotNetResult check() {
        // Priority 1: Try 'dotnet --list-runtimes' command (handles runtime-only installs)
        DotNetResult commandResult = checkDotnetListRuntimes();
        if (commandResult.installed) {
            return commandResult;
        }

        // Priority 2: Try 'dotnet --version' command (SDK installs)
        commandResult = checkDotnetCommand();
        if (commandResult.installed) {
            return commandResult;
        }

        // Priority 3: Platform-specific fallback checks
        if (isWindows()) {
            DotNetResult fsResult = checkWindowsFileSystem();
            if (fsResult.installed) return fsResult;
            return checkWindowsRegistry();
        } else if (isLinux() || isMac()) {
            return checkUnixPaths();
        }

        return new DotNetResult(false, "未安装", "", "未知的操作系统: " + OS_NAME);
    }

    /**
     * Check using 'dotnet --version' command.
     */
    private static DotNetResult checkDotnetCommand() {
        try {
            ProcessBuilder pb = new ProcessBuilder("dotnet", "--version");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String version = reader.readLine();
                int exitCode = process.waitFor();

                if (exitCode == 0 && version != null && !version.isEmpty()) {
                    // Find dotnet path
                    String dotnetPath = findDotnetPath();
                    return new DotNetResult(true, version.trim(), dotnetPath, null);
                }
            }
        } catch (IOException e) {
            // dotnet command not found
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return new DotNetResult(false, "未安装", "", "");
    }

    /**
     * Check using 'dotnet --list-runtimes' command (works with runtime-only installs, no SDK needed).
     */
    private static DotNetResult checkDotnetListRuntimes() {
        try {
            ProcessBuilder pb = new ProcessBuilder("dotnet", "--list-runtimes");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            String highestVersion = null;
            String dotnetPath = null;
            Pattern pattern = Pattern.compile("Microsoft\\.NETCore\\.App\\s+(\\d+\\.\\d+\\.\\d+.*?)\\s+\\[(.*?)\\]");

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    Matcher m = pattern.matcher(line);
                    if (m.find()) {
                        String ver = m.group(1).trim();
                        String path = m.group(2).trim();
                        if (highestVersion == null || compareVersions(ver, highestVersion) > 0) {
                            highestVersion = ver;
                            dotnetPath = path;
                        }
                    }
                }
            }
            int exitCode = process.waitFor();

            if (exitCode == 0 && highestVersion != null) {
                return new DotNetResult(true, highestVersion, dotnetPath, null);
            }
        } catch (IOException e) {
            // dotnet command not found
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return new DotNetResult(false, "未安装", "", "");
    }

    /**
     * Check Windows filesystem for .NET Runtime directories (no SDK needed).
     */
    private static DotNetResult checkWindowsFileSystem() {
        // Check DOTNET_ROOT environment variable first
        String dotnetRoot = System.getenv("DOTNET_ROOT");
        List<Path> searchPaths = new ArrayList<>();
        if (dotnetRoot != null) {
            searchPaths.add(Paths.get(dotnetRoot, "shared", "Microsoft.NETCore.App"));
        }
        String programFiles = System.getenv("ProgramFiles");
        if (programFiles != null) {
            searchPaths.add(Paths.get(programFiles, "dotnet", "shared", "Microsoft.NETCore.App"));
        }
        String programFilesX86 = System.getenv("ProgramFiles(x86)");
        if (programFilesX86 != null) {
            searchPaths.add(Paths.get(programFilesX86, "dotnet", "shared", "Microsoft.NETCore.App"));
        }

        String bestVersion = null;
        String bestPath = null;

        for (Path dir : searchPaths) {
            if (Files.isDirectory(dir)) {
                try {
                    String[] versionDirs = Files.list(dir)
                        .filter(Files::isDirectory)
                        .map(p -> p.getFileName().toString())
                        .filter(v -> v.matches("\\d+\\.\\d+\\.\\d+.*"))
                        .toArray(String[]::new);

                    for (String v : versionDirs) {
                        if (bestVersion == null || compareVersions(v, bestVersion) > 0) {
                            bestVersion = v;
                            bestPath = dir.resolve(v).toString();
                        }
                    }
                } catch (IOException ignored) {
                }
            }
        }

        if (bestVersion != null) {
            // Determine the dotnet root path (parent of shared/Microsoft.NETCore.App)
            String dotnetRootPath = bestPath;
            for (int i = 0; i < 3; i++) {
                dotnetRootPath = new File(dotnetRootPath).getParent();
            }
            return new DotNetResult(true, bestVersion, dotnetRootPath, null);
        }

        return new DotNetResult(false, "未安装", "", "");
    }

    /**
     * Compare two semantic version strings (e.g. "8.0.29" vs "10.0.5").
     * Returns negative if v1 < v2, positive if v1 > v2, 0 if equal.
     * Strips any trailing qualifiers (e.g. "-preview.1") for comparison.
     */
    public static int compareVersions(String v1, String v2) {
        String[] parts1 = v1.split("-")[0].split("\\.");
        String[] parts2 = v2.split("-")[0].split("\\.");
        int len = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < len; i++) {
            int n1 = i < parts1.length ? parseIntSafe(parts1[i]) : 0;
            int n2 = i < parts2.length ? parseIntSafe(parts2[i]) : 0;
            if (n1 != n2) return n1 - n2;
        }
        return 0;
    }

    private static int parseIntSafe(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Find the path of the dotnet executable.
     */
    private static String findDotnetPath() {
        try {
            String cmd = isWindows() ? "where dotnet" : "which dotnet";
            ProcessBuilder pb = new ProcessBuilder(cmd.split(" "));
            pb.redirectErrorStream(true);
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String path = reader.readLine();
                if (path != null && !path.isEmpty()) {
                    return path.trim();
                }
            }
            process.waitFor();
        } catch (Exception ignored) {
        }

        // Fallback: common installation paths
        if (isWindows()) {
            String programFiles = System.getenv("ProgramFiles");
            if (programFiles != null) {
                File dotnetExe = new File(programFiles, "dotnet\\dotnet.exe");
                if (dotnetExe.exists()) return dotnetExe.getAbsolutePath();
            }
            String programFilesX86 = System.getenv("ProgramFiles(x86)");
            if (programFilesX86 != null) {
                File dotnetExe = new File(programFilesX86, "dotnet\\dotnet.exe");
                if (dotnetExe.exists()) return dotnetExe.getAbsolutePath();
            }
        } else {
            File usrShare = new File("/usr/share/dotnet/dotnet");
            if (usrShare.exists()) return usrShare.getAbsolutePath();

            String home = System.getProperty("user.home");
            if (home != null) {
                File localDotnet = new File(home, ".dotnet/dotnet");
                if (localDotnet.exists()) return localDotnet.getAbsolutePath();
            }
        }

        return "未知";
    }

    /**
     * Check Windows registry for .NET installation.
     */
    private static DotNetResult checkWindowsRegistry() {
        try {
            String[] commands = {
                "cmd", "/c", "reg", "query",
                "HKLM\\SOFTWARE\\dotnet\\Setup\\InstalledVersions\\x64",
                "/v", "Version"
            };
            ProcessBuilder pb = new ProcessBuilder(commands);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }
            process.waitFor();

            String result = output.toString();
            if (result.contains("Version")) {
                // Parse version from registry output: "Version    REG_SZ    8.0.100"
                String[] parts = result.split("REG_SZ");
                if (parts.length > 1) {
                    String version = parts[parts.length - 1].trim();
                    if (!version.isEmpty()) {
                        return new DotNetResult(true, version,
                            "C:\\Program Files\\dotnet", null);
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return new DotNetResult(false, "未安装", "", "");
    }

    /**
     * Check common Unix paths for .NET installation.
     */
    private static DotNetResult checkUnixPaths() {
        // Check /usr/share/dotnet/
        Path usrShare = Paths.get("/usr/share/dotnet");
        if (Files.isDirectory(usrShare)) {
            String version = readDotnetVersionFromFile(usrShare);
            if (version != null) {
                return new DotNetResult(true, version, "/usr/share/dotnet", null);
            }
        }

        // Check $HOME/.dotnet/
        String home = System.getProperty("user.home");
        if (home != null) {
            Path userDotnet = Paths.get(home, ".dotnet");
            if (Files.isDirectory(userDotnet)) {
                String version = readDotnetVersionFromFile(userDotnet);
                if (version != null) {
                    return new DotNetResult(true, version, userDotnet.toString(), null);
                }
            }
        }

        return new DotNetResult(false, "未安装", "", "");
    }

    /**
     * Try to read .NET version from SDK directory listing.
     */
    private static String readDotnetVersionFromFile(Path dotnetPath) {
        Path sdkPath = dotnetPath.resolve("sdk");
        if (Files.isDirectory(sdkPath)) {
            try {
                return Files.list(sdkPath)
                    .filter(Files::isDirectory)
                    .map(p -> p.getFileName().toString())
                    .filter(v -> v.matches("\\d+\\.\\d+\\.\\d+.*"))
                    .findFirst()
                    .orElse(null);
            } catch (IOException ignored) {
            }
        }
        return null;
    }

    private static boolean isWindows() {
        return OS_NAME.contains("win");
    }

    private static boolean isLinux() {
        return OS_NAME.contains("linux");
    }

    private static boolean isMac() {
        return OS_NAME.contains("mac");
    }

    /**
     * Get a human-readable OS name.
     */
    public static String getOsDisplayName() {
        if (isWindows()) {
            return "Windows " + System.getProperty("os.version", "");
        } else if (isLinux()) {
            return "Linux " + System.getProperty("os.version", "");
        } else if (isMac()) {
            return "macOS " + System.getProperty("os.version", "");
        }
        return OS_NAME + " " + System.getProperty("os.version", "");
    }
}
