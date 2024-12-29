package dev.skidfuscator.obfuscator.command;

import dev.skidfuscator.jghost.GhostHelper;
import dev.skidfuscator.jghost.tree.GhostLibrary;
import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.util.misc.SkidTimedLogger;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import picocli.CommandLine;

import java.io.File;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

@CommandLine.Command(
        aliases = "mappings",
        mixinStandardHelpOptions = true,
        version = "1.0.0",
        description = "Creates a collated mappings file given a specific directory"
)
public class MappingsCommand implements Callable<Integer> {

    @CommandLine.Parameters(
            index = "0",
            description = "The directory which will be used to create the mappings file"
    )
    private File input;

    @CommandLine.Option(
            names = {"-o", "--output"},
            description = "Path to the output mappings file location"
    )
    private File output = new File("compressed-mappings.json");

    @Override
    public Integer call() throws Exception {
        if (input == null) {
            System.out.println("Invalid input file");
            return 1;
        }

        if (!input.getPath().endsWith(".jar") && !input.isDirectory()) {
            System.err.println("Invalid input file. Must be a jar file or a directory");
            return 1;
        }

        if (output == null) {
            System.err.println("Invalid output file");
            return 1;
        }

        final Logger log = LogManager.getLogger(Skidfuscator.class);
        final SkidTimedLogger logger = new SkidTimedLogger(true, log);

        AtomicReference<GhostLibrary> ghostLibrary = new AtomicReference<>(null);
        iterateFolder(input, logger, ghostLibrary);

        GhostHelper.saveLibraryFile(logger, ghostLibrary.get(), output);
        logger.style("Successfully created mappings file");

        return 0;
    }

    private void iterateFolder(File file, SkidTimedLogger logger, AtomicReference<GhostLibrary> ghostLibrary) {
        if (file.isDirectory()) {
            for (File files : file.listFiles()) {
                iterateFolder(files, logger, ghostLibrary);
            }
        } else {
            if (file.getAbsolutePath().endsWith(".jar") || file.getAbsolutePath().endsWith(".jmod")) {
                final GhostLibrary ghost = GhostHelper.createFromLibraryFile(logger, file);
                logger.style("Creating mappings for " + file.getAbsolutePath() + "...\n");
                if (ghostLibrary.get() == null) {
                    ghostLibrary.set(ghost);
                } else {
                    ghostLibrary.get().merge(ghost);
                }
            }
        }
    }
}
