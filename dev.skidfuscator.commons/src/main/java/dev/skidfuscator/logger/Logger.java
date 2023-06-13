package dev.skidfuscator.logger;

public interface Logger {
    void log(String text);

    void post(String text);

    void warn(String text);

    void debug(String text);

    void style(String text);

    void error(String text, Throwable e);
}
