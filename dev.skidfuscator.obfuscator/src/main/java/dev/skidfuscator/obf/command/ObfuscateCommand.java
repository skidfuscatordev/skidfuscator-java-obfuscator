package dev.skidfuscator.obf.command;

import dev.skidfuscator.obf.SkidInstance;
import dev.skidfuscator.obf.directory.SkiddedDirectory;
import dev.skidfuscator.obf.init.DefaultInitHandler;
import dev.skidfuscator.obf.init.SkidSession;
import dev.skidfuscator.obf.utils.MapleJarUtil;
import org.mapleir.deob.PassGroup;
import picocli.CommandLine;

import java.io.File;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.Callable;

/**
 * @author Ghast
 * @since 06/03/2021
 * SkidfuscatorV2 © 2021
 */

@CommandLine.Command(
        name = "obfuscate",
        mixinStandardHelpOptions = true,
        version = "obfuscate 1.0.0",
    description = "Obfuscates and runs a specific jar"
)
public class ObfuscateCommand implements Callable<Integer> {

    @CommandLine.Parameters(
            index = "0",
            description = "The file which will be obfuscated."
    )
    private File input;

    @CommandLine.Option(
            names = {"-rt", "--runtime"},
            description = "Path to the runtime jar"
    )
    private File runtime;

    @CommandLine.Option(
            names = {"-li", "--libs"},
            description = "Path to the libs folder"
    )
    private File libs;

    @CommandLine.Option(
            names = {"-o", "--output"},
            description = "Path to the output jar location"
    )
    private File output;

    @CommandLine.Option(
            names = {"-dmp", "--antidump"},
            description = "Option to add the injected antidump"
    )
    private boolean preventDump;

    @Override
    public Integer call()  {
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
                "                       Author: Ghast     Version: 1.0.8     Today: " + new Date(Instant.now().toEpochMilli()).toGMTString(),
                "",
                ""
        };

        for (String s : logo) {
            System.out.println(s);
        }

        if (output == null) {
            output = new File(input.getPath() + "-out.jar");
        }

        if (runtime == null) {
            runtime = new File(System.getProperty("java.home"), "lib/rt.jar");
        }

        final SkidInstance skidInstance = SkidInstance.builder()
                .input(input)
                .output(output)
                .libs(libs)
                .runtime(runtime)
                .preventDump(preventDump)
                .exclusions(Collections.emptyList())
                .build();

        start(skidInstance);
        return 0;
    }

    public static File start(final SkidInstance instance) {
        final SkiddedDirectory directory = new SkiddedDirectory(null);
        directory.init();

        final SkidSession session = new DefaultInitHandler().init(instance);
        try {
            MapleJarUtil.dumpJar(session.getClassSource(), session.getJarDownloader(), new PassGroup("Output"),
                    session.getOutputFile().getPath());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return instance.getOutput();
    }
}
