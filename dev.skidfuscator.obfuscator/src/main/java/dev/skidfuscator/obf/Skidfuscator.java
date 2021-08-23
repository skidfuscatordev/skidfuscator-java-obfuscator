package dev.skidfuscator.obf;

import dev.skidfuscator.obf.init.DefaultInitHandler;
import dev.skidfuscator.obf.init.SkidSession;
import dev.skidfuscator.obf.utils.MapleJarUtil;
import org.mapleir.deob.PassGroup;

import java.io.File;

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


    public void init() {
        if (args.length < 1) {
            System.out.println("Not valid command bro");
            System.exit(1);
            return;
        }

        final File file = new File(args[0]);

        final SkidSession session = new DefaultInitHandler().init(file, new File(file.getPath() + "-out.jar"));
        try {
            MapleJarUtil.dumpJar(session.getClassSource(), session.getJarDownloader(), new PassGroup("Output"),
                    session.getOutputFile().getPath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
