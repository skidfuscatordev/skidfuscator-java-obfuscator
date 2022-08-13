package dev.skidfuscator.testclasses.evaluator.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Log {
    private final List<String> logs = new ArrayList<>();

    public void exportLog() throws IOException {
        File output = new File("calculations.txt");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(output))) {
            logs.forEach(log -> {
                try {
                    boolean shouldPrintNewLine = !log.contains("\n");

                    if (shouldPrintNewLine)
                        writer.write("\n");

                    writer.write(log);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            writer.flush();
        }
    }

    public void addLog(String log) {
        logs.add(log);
    }

    public void println(String log) {
        addLog(log);

        System.out.println(log);
    }

    public void print(String log, Object... args) {
        addLog(String.format(log, args));

        System.out.printf(log, args);
    }
}
