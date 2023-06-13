package dev.skidfuscator.obfuscator.util.misc;

import dev.skidfuscator.logger.TimedLogger;
import org.apache.log4j.Logger;
import org.fusesource.jansi.Ansi;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayDeque;
import java.util.Deque;

import static org.fusesource.jansi.Ansi.ansi;

public class SkidTimedLogger implements TimedLogger {
    private boolean debug;
    private final Logger logger;

    public SkidTimedLogger(boolean debug, Logger logger) {
        this.debug = debug;
        this.logger = logger;
    }

    @Override
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    private static Deque<String> sections = new ArrayDeque<>();
    private static long timer;

    private double lap() {
        long now = System.nanoTime();
        long delta = now - timer;
        timer = now;
        return (double)delta / 1_000_000_000L;
    }

    private void section0(String endText, String sectionText) {
        if(sections.isEmpty()) {
            lap();
            if (debug) logger.info(sectionText);
            else logger.debug(sectionText);
        } else {
            /* remove last section. */
            sections.pop();
            if (debug) {
                logger.info(String.format(endText, lap()));
                logger.info(sectionText);
            } else {
                logger.debug(String.format(endText, lap()));
                logger.debug(sectionText);
            }
        }

        /* push the new one. */
        sections.push(sectionText);
    }

    @Override
    public void log(String text) {
        section0("...took %fs.", text);
    }

    @Override
    public void post(String text) {
        if (debug) logger.info(text);
        else logger.debug(text);
    }

    @Override
    public void warn(String text) {
        System.out.print(ansi()
                        .cursorUpLine()
                        .eraseLine(Ansi.Erase.ALL)
        );
        logger.warn(text);
    }

    @Override
    public void style(String text) {
        System.out.print(ansi()
                .cursorUpLine()
                .eraseLine(Ansi.Erase.ALL)
        );
        logger.info(text);
    }

    @Override
    public void debug(String text) {
        logger.debug(text);
    }

    @Override
    public void error(String text, Throwable e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        String exceptionAsString = sw.toString();
        warn(text + "\n" + exceptionAsString);
        //logger.debug("[Repeat] " + text, e);
    }
}