package com.goblincoders;

import java.io.PrintStream;

import com.goblincoders.checker.DotNetCheckerImpl;
import com.goblincoders.checker.DotNetCheckerImpl.DotNetResult;

/**
 * Main checker class for .NET Runtime detection.
 * Formats and outputs detection results.
 */
public class DotNetChecker {

    private static final String PREFIX = "[dotNetChecker] ";
    private static final String SEPARATOR = PREFIX + "============================================";

    /**
     * Perform the .NET Runtime check.
     *
     * @return true if .NET is installed, false otherwise
     */
    public static boolean check() {
        return check(System.out);
    }

    /**
     * Perform the .NET Runtime check with custom output stream.
     *
     * @param out the output stream to write results to
     * @return true if .NET is installed, false otherwise
     */
    public static boolean check(PrintStream out) {
        DotNetResult result = DotNetCheckerImpl.check();

        out.println(SEPARATOR);
        out.println(PREFIX + ".NET Runtime Check");
        out.println(SEPARATOR);
        out.println(PREFIX + "OS:    " + DotNetCheckerImpl.getOsDisplayName());
        out.println(PREFIX + "Ver:   " + result.version);
        out.println(PREFIX + "Path:  " + result.path);
        out.println(SEPARATOR);

        if (result.installed) {
            out.println(PREFIX + "Status: INSTALLED");
            out.println(SEPARATOR);
            return true;
        } else {
            out.println(PREFIX + "WARNING: .NET Runtime not found!");
            out.println(SEPARATOR);
            out.println(PREFIX + "  .NET 8.0 or later is required.");
            out.println(PREFIX + "  Download: https://dotnet.microsoft.com/download/dotnet/8.0");
            out.println(SEPARATOR);
            out.println(PREFIX + "BLOCKED: Game launch prevented.");
            out.println(SEPARATOR);
            return false;
        }
    }

    /**
     * Standalone test entry point.
     * Run with: java -cp dotNetChecker-*.jar com.goblincoders.DotNetChecker
     */
    public static void main(String[] args) {
        boolean installed = check();
        System.exit(installed ? 0 : 1);
    }
}
