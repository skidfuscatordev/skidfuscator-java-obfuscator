package dev.skidfuscator.obf.command;

import dev.skidfuscator.obf.SkidConfig;
import dev.skidfuscator.obf.SkidInstance;
import dev.skidfuscator.obf.directory.SkiddedDirectory;
import dev.skidfuscator.obf.init.DefaultInitHandler;
import dev.skidfuscator.obf.init.SkidSession;
import dev.skidfuscator.obf.utils.MapleJarUtil;
import org.bovinegenius.kurgan.ConfigLoader;
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
        name = "run",
        mixinStandardHelpOptions = true,
        version = "obfuscate 1.0.0",
    description = "Obfuscates and runs a specific jar"
)
public class ObfuscateWithConfigCommand implements Callable<Integer> {

    @CommandLine.Parameters(
            index = "0",
            description = "Config path of the file."
    )
    private File config;
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

        final SkidConfig skidConfig = ConfigLoader.getDefault().loadYaml(SkidConfig.class, config.getAbsolutePath());


        final SkidInstance skidInstance = SkidInstance.builder()
                .input(skidConfig.input().location())
                .output(skidConfig.output().location())
                .libs(skidConfig.libs().location())
                .runtime(skidConfig.runtime().location())
                .preventDump(skidConfig.preventDump().enabled())
                .exclusions(skidConfig.excludes())
                .build();

        if (skidInstance.getOutput() == null) {
            skidInstance.setOutput(new File(skidInstance.getInput().getPath() + "-out.jar"));
        }

        if (skidInstance.getRuntime() == null) {
            skidInstance.setRuntime(new File(System.getProperty("java.home"), "lib/rt.jar"));
        }

        if (skidInstance.getExclusions() == null) {
            skidInstance.setExclusions(Collections.emptyList());
        }

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
