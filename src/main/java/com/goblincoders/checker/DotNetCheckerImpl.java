package com.goblincoders.checker;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

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
        // Priority 1: Try 'dotnet --version' command
        DotNetResult commandResult = checkDotnetCommand();
        if (commandResult.installed) {
            return commandResult;
        }

        // Priority 2: Platform-specific fallback checks
        if (isWindows()) {
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
