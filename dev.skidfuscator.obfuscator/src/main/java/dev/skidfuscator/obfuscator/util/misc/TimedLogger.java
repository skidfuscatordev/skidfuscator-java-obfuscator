package dev.skidfuscator.obfuscator.util.misc;

import org.apache.log4j.Logger;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayDeque;
import java.util.Deque;

public class TimedLogger {
    private final Logger logger;

    public TimedLogger(Logger logger) {
        this.logger = logger;
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
            logger.info(sectionText);
        } else {
            /* remove last section. */
            sections.pop();
            logger.info(String.format(endText, lap()));
            logger.info(sectionText);
        }

        /* push the new one. */
        sections.push(sectionText);
    }

    public void log(String text) {
        section0("...took %fs.", text);
    }

    public void post(String text) {
        logger.info(text);
    }

    public void warn(String text) {
        logger.warn(text);
    }

    public void error(String text, Throwable e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        String exceptionAsString = sw.toString();
        warn(text + "\n" + exceptionAsString);
        //logger.debug("[Repeat] " + text, e);
    }
}