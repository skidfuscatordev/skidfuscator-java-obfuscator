package dev.skidfuscator.obfuscator.command;

import dev.skidfuscator.migration.ExemptToConfigMigration;
import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.SkidfuscatorSession;
import dev.skidfuscator.obfuscator.ReliabilityOptions;
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
        version = "Skidfuscator " + Skidfuscator.VERSION,
    description = "Obfuscates and runs a specific jar"
)
public class ObfuscateCommand implements Callable<Integer> {
    @CommandLine.Parameters(
            index = "0",
            description = "The file which will be obfuscated."
    )
    public File input;

    @CommandLine.Option(
            names = {"-rt", "--runtime"},
            description = "Target JDK jmods directory, lib/modules image or Java 8 rt.jar"
    )
    public File runtime;

    @CommandLine.Option(
            names = {"-li", "--libs"},
            description = "Path to the libs folder"
    )
    public File libFolder;

    @CommandLine.Option(
            names = {"-ex", "--exempt"},
            description = "Path to the exempt file"
    )
    public File exempt;

    @CommandLine.Option(
            names = {"-o", "--output"},
            description = "Path to the output jar location"
    )
    public File output;

    @CommandLine.Option(
            names = {"-cfg", "--config"},
            description = "Path to the config file"
    )
    public File config;

    @CommandLine.Option(
            names = {"-ph", "--phantom"},
            description = "Declare if phantom computation should be used"
    )
    public boolean phantom;

    @CommandLine.Option(
            names = {"-fuckit", "--fuckit"},
            description = "Do not use!"
    )
    public boolean fuckit;

    @CommandLine.Option(
            names = {"-dbg", "--debug"},
            description = "Do not use!"
    )
    public boolean debug;

    @CommandLine.Option(
            names = {"-notrack", "--notrack"},
            description = "If you do not wish to be part of analytics!"
    )
    public boolean notrack;


    @CommandLine.Option(names="--seed", description="Seed recorded in the run report") public Long seed;
    @CommandLine.Option(names="--report", description="JSON run report (default: output.report.json)") public File report;
    @CommandLine.Option(names="--report-summary", description="Keep counts and diagnostics, omit individual symbol decisions") public boolean summaryReport;
    @CommandLine.Option(names="--fail-on-duplicate-classes", description="Reject ambiguous application/library class names") public boolean failOnDuplicateClasses;
    @CommandLine.Option(names="--dry-run", description="Plan exclusions without transforming or writing a JAR") public boolean dryRun;
    @CommandLine.Option(names="--offline", description="Never download missing dependencies") public boolean offline;
    @CommandLine.Option(names="--non-interactive", description="Explicit automation mode; never prompt") public boolean nonInteractive;
    @CommandLine.Option(names="--analytics", description="Opt in to usage analytics") public boolean analytics;
    @CommandLine.Option(names="--profile", defaultValue="BALANCED", description="CONSERVATIVE, BALANCED or STRONG") public ReliabilityOptions.Profile profile;
    @CommandLine.Option(names="--max-duration-ms", defaultValue="300000") public long maxDuration;
    @CommandLine.Option(names="--max-input-bytes", defaultValue="536870912") public long maxInput;
    @CommandLine.Option(names="--max-output-bytes", defaultValue="536870912") public long maxOutput;
    @CommandLine.Option(names="--max-growth-ratio", defaultValue="0") public double maxGrowth;
    @CommandLine.Option(names="--max-method-instructions", defaultValue="0") public int maxInstructions;

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
            if (!runtime.exists() && MiscUtil.getJavaVersion() > 8) runtime = new File(home, "lib/modules");
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

        ReliabilityOptions options = ReliabilityOptions.builder().profile(profile).seed(seed).report(report)
                .dryRun(dryRun).summaryReport(summaryReport).failOnDuplicateClasses(failOnDuplicateClasses).offline(offline).maxDurationMillis(maxDuration).maxOutputBytes(maxOutput)
                .maxInputBytes(maxInput).maxGrowthRatio(maxGrowth).maxMethodInstructions(maxInstructions).build();
        options.validate();
        final File[] libs;
        if (libFolder != null) {
            libs = libFolder.listFiles(file -> file.isFile() && file.getName().endsWith(".jar"));
            if (libs == null) throw new IllegalArgumentException("Library directory cannot be read: " + libFolder);
            java.util.Arrays.sort(libs, java.util.Comparator.comparing(File::getName));
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
                .jmod(runtime.isDirectory())
                .fuckit(fuckit)
                .config(config)
                .debug(debug)
                .renamer(false)
                .analytics(analytics && !notrack)
                .reliability(options)
                .build();

        final Skidfuscator skidfuscator = new Skidfuscator(skidInstance);
        skidfuscator.run();

        return 0;
    }


}
