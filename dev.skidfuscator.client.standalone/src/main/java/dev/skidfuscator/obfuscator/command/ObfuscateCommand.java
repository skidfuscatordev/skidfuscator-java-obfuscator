package dev.skidfuscator.obfuscator.command;

import dev.skidfuscator.migration.ExemptToConfigMigration;
import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.SkidfuscatorSession;
import dev.skidfuscator.obfuscator.util.ConsoleColors;
import dev.skidfuscator.obfuscator.util.LogoUtil;
import dev.skidfuscator.obfuscator.util.MiscUtil;
import picocli.CommandLine;

import java.io.File;
import java.text.DateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.Callable;

/**
 * @author Ghast
 * @since 06/03/2021
 * SkidfuscatorV2 © 2021
 */

@CommandLine.Command(
        aliases = "obfuscate",
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
    private File libFolder;

    @CommandLine.Option(
            names = {"-ex", "--exempt"},
            description = "Path to the exempt file"
    )
    private File exempt;

    @CommandLine.Option(
            names = {"-o", "--output"},
            description = "Path to the output jar location"
    )
    private File output;

    @CommandLine.Option(
            names = {"-cfg", "--config"},
            description = "Path to the config file"
    )
    private File config;

    @CommandLine.Option(
            names = {"-ph", "--phantom"},
            description = "Declare if phantom computation should be used"
    )
    private boolean phantom;

    @CommandLine.Option(
            names = {"-fuckit", "--fuckit"},
            description = "Do not use!"
    )
    private boolean fuckit;

    @CommandLine.Option(
            names = {"-dbg", "--debug"},
            description = "Do not use!"
    )
    private boolean debug;

    @CommandLine.Option(
            names = {"-notrack", "--notrack"},
            description = "If you do not wish to be part of analytics!"
    )
    private boolean notrack;


    @Override
    public Integer call()  {

        if (input == null) {
            return -1;
        }

        if (output == null) {
            output = new File(input.getPath() + "-out.jar");
        }

        if (runtime == null) {
            final String home = System.getProperty("java.home");
            runtime = new File(
                    home,
                    MiscUtil.getJavaVersion() > 8
                            ? "jmods"
                            : "lib/rt.jar"
            );
        }

        if (exempt != null) {
            final File converted = new File(new File(exempt.getAbsolutePath()).getParentFile().getAbsolutePath(), "config.hocon");
            final String warning = "\n" + ConsoleColors.YELLOW
                    + "██╗    ██╗ █████╗ ██████╗ ███╗   ██╗██╗███╗   ██╗ ██████╗ \n"
                    + "██║    ██║██╔══██╗██╔══██╗████╗  ██║██║████╗  ██║██╔════╝ \n"
                    + "██║ █╗ ██║███████║██████╔╝██╔██╗ ██║██║██╔██╗ ██║██║  ███╗\n"
                    + "██║███╗██║██╔══██║██╔══██╗██║╚██╗██║██║██║╚██╗██║██║   ██║\n"
                    + "╚███╔███╔╝██║  ██║██║  ██║██║ ╚████║██║██║ ╚████║╚██████╔╝\n"
                    + " ╚══╝╚══╝ ╚═╝  ╚═╝╚═╝  ╚═╝╚═╝  ╚═══╝╚═╝╚═╝  ╚═══╝ ╚═════╝ \n"
                    + "\n"
                    + "⚠️  Warning! Skidfuscator has deprecated the exempt file!\n"
                    + ConsoleColors.RESET
                    + "\n  Launching migrator service..."
                    + "\n  Config will be found at " + converted
                    + "\n";
            Skidfuscator.LOGGER.post(warning);
            new ExemptToConfigMigration().migrate(exempt, converted);

            config = converted;
        }

        final File[] libs;
        if (libFolder != null) {
            libs = libFolder.listFiles();
        } else {
            libs = new File[0];
        }

        final SkidfuscatorSession skidInstance = SkidfuscatorSession.builder()
                .input(input)
                .output(output)
                .libs(libs)
                .runtime(runtime)
                .exempt(exempt)
                .phantom(phantom)
                .jmod(MiscUtil.getJavaVersion() > 8)
                .fuckit(fuckit)
                .config(config)
                .debug(debug)
                .renamer(false)
                .analytics(!notrack)
                .build();

        final Skidfuscator skidfuscator = new Skidfuscator(skidInstance);
        skidfuscator.run();

        return 0;
    }


}