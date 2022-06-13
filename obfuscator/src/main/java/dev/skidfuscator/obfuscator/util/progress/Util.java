package dev.skidfuscator.obfuscator.util.progress;

import me.tongfei.progressbar.ConsoleProgressBarConsumer;
import me.tongfei.progressbar.InteractiveConsoleProgressBarConsumer;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;

class Util {
    static ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1, (runnable) -> {
        Thread thread = Executors.defaultThreadFactory().newThread(runnable);
        thread.setName("ProgressBar");
        thread.setDaemon(true);
        return thread;
    });

    Util() {
    }

    static ConsoleProgressBarConsumer createConsoleConsumer(int predefinedWidth) {
        PrintStream real = new PrintStream(new FileOutputStream(FileDescriptor.err));
        return createConsoleConsumer(real, predefinedWidth);
    }

    static ConsoleProgressBarConsumer createConsoleConsumer(PrintStream out) {
        return createConsoleConsumer(out, -1);
    }

    static ConsoleProgressBarConsumer createConsoleConsumer(PrintStream out, int predefinedWidth) {
        return (ConsoleProgressBarConsumer)(TerminalUtils.hasCursorMovementSupport() ? new InteractiveConsoleProgressBarConsumer(out, predefinedWidth) : new ConsoleProgressBarConsumer(out, predefinedWidth));
    }

    static String repeat(char c, int n) {
        if (n <= 0) {
            return "";
        } else {
            char[] s = new char[n];

            for(int i = 0; i < n; ++i) {
                s[i] = c;
            }

            return new String(s);
        }
    }

    static String formatDuration(Duration d) {
        long s = d.getSeconds();
        return String.format("%d:%02d:%02d", s / 3600L, s % 3600L / 60L, s % 60L);
    }

    static long getInputStreamSize(InputStream is) {
        try {
            if (is instanceof FileInputStream) {
                return ((FileInputStream)is).getChannel().size();
            }

            int available = is.available();
            if (available > 0) {
                return (long)available;
            }
        } catch (IOException var2) {
        }

        return -1L;
    }
}