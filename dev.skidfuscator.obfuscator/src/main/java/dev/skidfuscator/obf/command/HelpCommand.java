package dev.skidfuscator.obf.command;

import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "help",
        mixinStandardHelpOptions = true,
        version = "help 1.0.0",
        description = "Help with skidfooscator"
)
public class HelpCommand implements Callable<Integer> {
    @Override
    public Integer call() throws Exception {
        System.out.println("Run with java -jar <skidfuscator.jar> obfuscate <settings>");
        return 0;
    }
}
