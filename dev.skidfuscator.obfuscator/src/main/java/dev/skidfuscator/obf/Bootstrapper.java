package dev.skidfuscator.obf;

import dev.skidfuscator.obf.command.HelpCommand;
import dev.skidfuscator.obf.command.ObfuscateCommand;
import dev.skidfuscator.obf.command.ObfuscateWithConfigCommand;
import org.mapleir.cli.cmd.RunCommand;
import picocli.CommandLine;

/**
 * @author Ghast
 * @since 21/01/2021
 * SkidfuscatorV2 © 2021
 */
public class Bootstrapper {
    public static void main(String[] args) {
        final int exitCode = new CommandLine(
                new HelpCommand()
        )
                .addSubcommand("run", new ObfuscateWithConfigCommand())
                .addSubcommand("obfuscate", new ObfuscateCommand())
                .execute(args);
    }
}
