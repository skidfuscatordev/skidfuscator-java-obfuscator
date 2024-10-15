package dev.skidfuscator.obfuscator;

import dev.skidfuscator.obfuscator.command.HelpCommand;
import dev.skidfuscator.obfuscator.command.MappingsCommand;
import dev.skidfuscator.obfuscator.command.ObfuscateCommand;
import dev.skidfuscator.obfuscator.util.LogoUtil;
import lombok.SneakyThrows;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.TerminalBuilder;
import picocli.CommandLine;

import java.io.File;

public class SkidfuscatorMain {

    @SneakyThrows
    public static void main(String[] args) {

        if (args.length == 1 && args[0].equalsIgnoreCase("cli")) {
            final LineReader reader = LineReaderBuilder
                    .builder()
                    .terminal(TerminalBuilder.terminal())
                    .appName("Skidfuscator")
                    .parser(new DefaultParser())
                    .build();

            while (true) {
                String line = null;

                try {
                    line = reader.readLine("> ");
                } catch (UserInterruptException e) {
                    // Ignore
                    break;
                } catch (EndOfFileException e) {
                    return;
                }

                if (line == null)
                    continue;

                if (line.contains("obfuscate")) {
                    final String input = line.split(" ")[1];
                    final String output = line.split(" ")[2];

                    final SkidfuscatorSession session = new SkidfuscatorSession(
                            new File(input),
                            new File(output),
                            null,
                            null,
                            null,
                            null,
                            new File(System.getProperty("java.home"), "lib/rt.jar"),
                            false,
                            false,
                            false,
                            false,
                            false,
                            false,
                            false,
                            false,
                            false
                    );

                    final Skidfuscator skidfuscator = new Skidfuscator(session);
                    skidfuscator.run();
                }
            }

        } else {
            LogoUtil.printLogo();
            new CommandLine(new HelpCommand())
                    .addSubcommand("obfuscate", new ObfuscateCommand())
                    .addSubcommand("mappings", new MappingsCommand())
                    .execute(args);
        }
    }
}
