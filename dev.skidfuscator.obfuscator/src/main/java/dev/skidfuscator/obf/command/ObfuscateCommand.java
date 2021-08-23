package dev.skidfuscator.obf.command;

import picocli.CommandLine;

import java.util.concurrent.Callable;

/**
 * @author Ghast
 * @since 06/03/2021
 * SkidfuscatorV2 © 2021
 */

@CommandLine.Command(name = "obfuscate", mixinStandardHelpOptions = true, version = "obfuscate 1.0.0",
    description = "Obfuscates and runs a specific jar")
public class ObfuscateCommand implements Callable<Integer> {
    @Override
    public Integer call()  {
        return null;
    }
}
