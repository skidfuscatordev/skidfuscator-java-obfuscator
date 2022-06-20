package dev.skidfuscator.obfuscator.util.progress;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Stream;

import me.tongfei.progressbar.ProgressBarConsumer;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp.Capability;

public class TerminalUtils {
    static final char CARRIAGE_RETURN = '\r';
    static final char ESCAPE_CHAR = '\u001b';
    static final int DEFAULT_TERMINAL_WIDTH = 80;
    private static Terminal terminal = null;
    private static boolean cursorMovementSupported = false;
    static Queue<ProgressBarConsumer> activeConsumers = new ConcurrentLinkedQueue();

    public TerminalUtils() {
    }

    static synchronized int getTerminalWidth() {
        Terminal terminal = getTerminal();
        int width = terminal.getWidth();
        return width >= 10 ? width : 80;
    }

    static boolean hasCursorMovementSupport() {
        if (terminal == null) {
            terminal = getTerminal();
        }

        return cursorMovementSupported;
    }

    static synchronized void closeTerminal() {
        try {
            if (terminal != null) {
                terminal.close();
                terminal = null;
            }
        } catch (IOException var1) {
        }

    }

    static <T extends ProgressBarConsumer> Stream<T> filterActiveConsumers(Class<T> clazz) {
        Stream var10000 = activeConsumers.stream();
        clazz.getClass();
        var10000 = var10000.filter(clazz::isInstance);
        clazz.getClass();
        return var10000.map(clazz::cast);
    }

    static String moveCursorUp(int count) {
        return "\u001b[" + count + "A" + '\r';
    }

    static String moveCursorDown(int count) {
        return "\u001b[" + count + "B" + '\r';
    }

    static Terminal getTerminal() {
        if (terminal == null) {
            try {
                terminal = TerminalBuilder.builder().dumb(true).build();
                cursorMovementSupported = terminal.getStringCapability(Capability.cursor_up) != null && terminal.getStringCapability(Capability.cursor_down) != null;
            } catch (IOException var1) {
                throw new RuntimeException("This should never happen! Dumb terminal should have been created.");
            }
        }

        return terminal;
    }
}