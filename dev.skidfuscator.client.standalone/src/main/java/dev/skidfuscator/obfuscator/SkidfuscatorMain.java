package dev.skidfuscator.obfuscator;

import com.formdev.flatlaf.intellijthemes.FlatDarkPurpleIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatGradiantoMidnightBlueIJTheme;
import dev.skidfuscator.obfuscator.command.HelpCommand;
import dev.skidfuscator.obfuscator.command.MappingsCommand;
import dev.skidfuscator.obfuscator.command.ObfuscateCommand;
import dev.skidfuscator.obfuscator.gui.MainFrame;
import dev.skidfuscator.obfuscator.util.LogoUtil;
import lombok.SneakyThrows;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.TerminalBuilder;
import picocli.CommandLine;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class SkidfuscatorMain {

    @SneakyThrows
    public static void main(String[] args) {

        if (args.length == 0) {
            // MacOS menu bar
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Skidfuscator");
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            SwingUtilities.invokeLater(() -> {
                try {
                    FlatDarkPurpleIJTheme.setup();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                final int option = JOptionPane.showOptionDialog(
                        null,
                        "You are running Skidfuscator Community! Whilst this project is free and open-source, " +
                                "we heavily encourage commercial users to support the project by purchasing enterprise" +
                                " edition available on our website",
                        "Notice!",
                        JOptionPane.DEFAULT_OPTION,
                        JOptionPane.INFORMATION_MESSAGE,
                        UIManager.getIcon("OptionPane.informationIcon"),
                        new Object[]{"OK", "Buy Enterprise", "Join the Discord"},
                        "OK"
                );

                if (option == 0) {
                    new MainFrame().setVisible(true);
                    return;
                }

                final String url = switch (option) {
                    case 1 -> "https://skidfuscator.dev/pricing";
                    case 2 -> "https://discord.gg/QJC9g8fBU9";
                    default -> throw new IllegalStateException("Impossible");
                };

                CompletableFuture.delayedExecutor(1, TimeUnit.SECONDS).execute(() -> {
                    try {
                        Desktop.getDesktop().browse(new URI(url));
                    } catch (IOException | URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                });
                main(args);
                return;

            });
            return;
        }

        LogoUtil.printLogo();

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
            new CommandLine(new HelpCommand())
                    .addSubcommand("obfuscate", new ObfuscateCommand())
                    .addSubcommand("mappings", new MappingsCommand())
                    .execute(args);
        }
    }
}
