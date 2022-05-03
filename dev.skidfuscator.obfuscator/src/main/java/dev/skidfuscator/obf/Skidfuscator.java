package dev.skidfuscator.obf;

import dev.skidfuscator.obf.directory.SkiddedDirectory;
import dev.skidfuscator.obf.init.DefaultInitHandler;
import dev.skidfuscator.obf.init.SkidSession;
import dev.skidfuscator.obf.utils.MapleJarUtil;
import org.mapleir.deob.PassGroup;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import org.apache.commons.cli.*;

/**
 * @author Ghast
 * @since 21/01/2021
 * SkidfuscatorV2 © 2021
 */
public class Skidfuscator {
    public static Skidfuscator INSTANCE;

    private String[] args;


    public static Skidfuscator init(String[] args) {
        return new Skidfuscator(args);
    }

    public Skidfuscator(String[] args) {
        INSTANCE = this;
        this.args = args;
        this.init();
    }

    public Skidfuscator() {
        this(new String[0]);
    }

    // Temp workaround
    public static boolean preventDump;
    public List<String> exclusions = new ArrayList<>();
    Options options = new Options();
    CommandLineParser cmdParser = new DefaultParser();
    HelpFormatter formatter = new HelpFormatter();

    public void init() {
        Option input = new Option("i", "input", true, "input file");
        input.setRequired(true);
        options.addOption(input);

        Option antidump = new Option("a","antidump", false, "prevent dumping");
        options.addOption(antidump);

        Option exclusion = new Option("e", "exclude", true, "file containing exclusions");
        options.addOption(exclusion);

        CommandLine cmdLine = null;

        try {
            cmdLine = cmdParser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("skidfuscator", options);
            System.exit(1);
        }

        preventDump = cmdLine.hasOption("antidump");
        if (cmdLine.hasOption("exclude"))
            exclusions = readExclusions(cmdLine.getOptionValue("exclude"));


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


        final File file = new File(cmdLine.getOptionValue("input"));
        start(file);
    }

    public static File start(final File file) {
        final SkiddedDirectory directory = new SkiddedDirectory(null);
        directory.init();

        final File out = new File(file.getPath() + "-out.jar");
        final SkidSession session = new DefaultInitHandler().init(file, out);
        try {
            MapleJarUtil.dumpJar(session.getClassSource(), session.getJarDownloader(), new PassGroup("Output"),
                    session.getOutputFile().getPath());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return out;
    }
    private List readExclusions(String excludeName) {
        List<String> exclusions = new ArrayList<>();
        File file = new File(excludeName);
        if (!file.exists()) {
            System.out.println("Cannot find exclusion file");
            formatter.printHelp("skidfuscator", options);
            System.exit(1);
        }
        try  {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String exclusion;
            while ((exclusion = br.readLine()) != null) {
                exclusions.add(exclusion);
            }
        }
        catch (IOException ex) {
            System.out.println("Error reading exclusions file");
            formatter.printHelp("skidfuscator", options);
            System.exit(1);
        }
        return exclusions;
    }
    public boolean matchesExclusion(String name) {
        for (String exclusion : exclusions) {
            String regex = exclusion.replaceAll(".", "[$0]").replace("[*]", ".*");
            if (name.matches(regex)) {
                return true;
            }
        }
        return false;
    }
}
