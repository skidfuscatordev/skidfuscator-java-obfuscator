package dev.skidfuscator.obfuscator.command;

import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(
        aliases = "help",
        mixinStandardHelpOptions = true,
        version = "help 1.0.0",
        description = "Shows all the options"
)
public class HelpCommand implements Callable<Integer> {
    @Override
    public Integer call() throws Exception {
        System.out.println("Please run 'java -jar skidfuscator.jar obfuscate <input jar> [options here]' instead!");
        return 0;
    }
}
