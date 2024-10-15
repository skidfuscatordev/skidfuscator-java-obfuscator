package dev.skidfuscator.obfuscator.util;

import lombok.experimental.UtilityClass;

import java.text.DateFormat;
import java.time.Instant;
import java.util.Date;

@UtilityClass
public class LogoUtil {
    public static void printLogo() {
        /* Total number of processors or cores available to the JVM */
        final String processors =
                String.format("%19.19s", "Processors:")
                        + "   "
                        + String.format(
                        "%-19.19s",
                        Runtime.getRuntime().availableProcessors() + " cores"
                );

        final long freeMemory = Math.round(Runtime.getRuntime().freeMemory() / 1E6);
        final String memory =
                String.format("%19.19s", "Current Memory:")
                        + "   "
                        + String.format("%-19.19s", freeMemory + "mb");

        final long maxMemory = Math.round(Runtime.getRuntime().maxMemory() / 1E6);
        final String memoryString = (maxMemory == Long.MAX_VALUE
                ? ConsoleColors.GREEN + "no limit"
                : maxMemory + "mb"
        );
        String topMemory =
                String.format("%19.19s", "Max Memory:")
                        + "   "
                        + String.format("%-19.19s",
                        memoryString + (maxMemory > 1500 ? "" : " ⚠️")
                );

        topMemory = MiscUtil.replaceColor(
                topMemory,
                memoryString,
                maxMemory > 1500 ? ConsoleColors.GREEN_BRIGHT : ConsoleColors.RED_BRIGHT
        );
        // slight fix for thing
        topMemory = topMemory.replace("⚠️", "⚠️ ");

        final String[] logo = new String[] {
                "",
                "  /$$$$$$  /$$       /$$       /$$  /$$$$$$                                           /$$",
                " /$$__  $$| $$      |__/      | $$ /$$__  $$                                         | $$",
                "| $$  \\__/| $$   /$$ /$$  /$$$$$$$| $$  \\__//$$   /$$  /$$$$$$$  /$$$$$$$  /$$$$$$  /$$$$$$    /$$$$$$   /$$$$$$",
                "|  $$$$$$ | $$  /$$/| $$ /$$__  $$| $$$$   | $$  | $$ /$$_____/ /$$_____/ |____  $$|_  $$_/   /$$__  $$ /$$__  $$",
                " \\____  $$| $$$$$$/ | $$| $$  | $$| $$_/   | $$  | $$|  $$$$$$ | $$        /$$$$$$$  | $$    | $$  \\ $$| $$  \\__/",
                " /$$  \\ $$| $$_  $$ | $$| $$  | $$| $$     | $$  | $$ \\____  $$| $$       /$$__  $$  | $$ /$$| $$  | $$| $$",
                "|  $$$$$$/| $$ \\  $$| $$|  $$$$$$$| $$     |  $$$$$$/ /$$$$$$$/|  $$$$$$$|  $$$$$$$  |  $$$$/|  $$$$$$/| $$",
                " \\______/ |__/  \\__/|__/ \\_______/|__/      \\______/ |_______/  \\_______/ \\_______/   \\___/   \\______/ |__/",
                "",
                "                               ┌───────────────────────────────────────────┐",
                "                               │ "             + processors +            " │",
                "                               │ "               + memory +              " │",
                "                               │ "              + topMemory +            " │",
                "                               └───────────────────────────────────────────┘",
                "",
                "                      Author: Ghast     Version: 2.0.8     Today: "
                        + DateFormat.getDateTimeInstance().format(new Date(Instant.now().toEpochMilli())),
                ""
        };

        for (String s : logo) {
            System.out.println(s);
        }
    }
}
