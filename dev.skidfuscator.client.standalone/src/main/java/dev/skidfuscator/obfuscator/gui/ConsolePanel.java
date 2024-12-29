package dev.skidfuscator.obfuscator.gui;

import dev.skidfuscator.obfuscator.gui.ansi.AnsiParser;
import dev.skidfuscator.obfuscator.gui.ansi.UnicodeBoxChars;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.text.*;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import javax.swing.text.Style;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConsolePanel extends JPanel implements SkidPanel {
    private final JTextPane consoleOutput;
    private final SimpleDateFormat timeFormat;
    private final StyledDocument doc;
    private final PrintStream originalOut;
    private final PrintStream originalErr;
    private final Style baseStyle;
    private final Map<String, Style> cachedStyles;

    public ConsolePanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createCompoundBorder(
                // Outer titled border
                BorderFactory.createTitledBorder(
                        BorderFactory.createEtchedBorder(EtchedBorder.RAISED), // Rounded line border with increased arc
                        "Console",
                        javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                        javax.swing.border.TitledBorder.DEFAULT_POSITION,
                        new Font("Segoe UI", Font.BOLD, 16)
                ),
                // Inner empty border for padding
                BorderFactory.createEmptyBorder(20, 0, 0, 0)
        ));
        cachedStyles = new HashMap<>();

        // Initialize console output
        consoleOutput = new JTextPane();
        consoleOutput.setEditable(false);
        consoleOutput.setBackground(new Color(30, 30, 30));
        consoleOutput.setFont(new Font("JetBrains Mono", Font.PLAIN, 8));

        // Create scroll pane
        JScrollPane scrollPane = new JScrollPane(consoleOutput);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        add(scrollPane, BorderLayout.CENTER);

        // Initialize document and styles
        doc = consoleOutput.getStyledDocument();
        baseStyle = initializeBaseStyle();

        // Initialize other components
        timeFormat = new SimpleDateFormat("HH:mm:ss.SSS");
        originalOut = System.out;
        originalErr = System.err;

        // Setup styles
        initializeStyles();

        // Redirect system output
        redirectSystemStreams();

        // Add control panel
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(e -> clearConsole());
        controlPanel.add(clearButton);
        add(controlPanel, BorderLayout.SOUTH);
    }

    private Style initializeBaseStyle() {
        Style style = consoleOutput.addStyle("base", null);
        StyleConstants.setFontFamily(style, "JetBrains Mono");
        StyleConstants.setFontSize(style, 8);
        StyleConstants.setForeground(style, new Color(200, 200, 200));
        return style;
    }

    private void initializeStyles() {
        // Progress styles
        addStyle("progress", new Color(85, 255, 255));
        addStyle("success", new Color(85, 255, 85));
        addStyle("error", new Color(255, 85, 85));
        addStyle("warning", new Color(255, 255, 85));
        addStyle("box", new Color(150, 150, 150));
        addStyle("stats", new Color(255, 255, 255));
    }

    private void addStyle(String name, Color color) {
        Style style = consoleOutput.addStyle(name, baseStyle);
        StyleConstants.setForeground(style, color);
        cachedStyles.put(name, style);
    }

    private int lastLineStart = 0;
    private final StringBuilder lineBuffer = new StringBuilder();
    private boolean inProgress = false;

    private class ConsoleOutputStream extends OutputStream {
        private final ByteArrayOutputStream buffer;
        private int currentLine;

        public ConsoleOutputStream() {
            this.buffer = new ByteArrayOutputStream();
            this.currentLine = 0;
        }

        @Override
        public void write(int b) {
            if (b == '\r') {
                // Handle carriage return by removing current line
                SwingUtilities.invokeLater(() -> {
                    try {
                        Element root = doc.getDefaultRootElement();
                        Element line = root.getElement(currentLine);
                        int start = line.getStartOffset();
                        int end = line.getEndOffset();
                        if (end - start > 0) {
                            doc.remove(start, end - start);
                        } else {
                            // Remove last line if it's empty
                            Element prevLine = root.getElement(currentLine - 1);
                            doc.remove(prevLine.getStartOffset(), prevLine.getEndOffset());
                        }
                    } catch (BadLocationException e) {
                        e.printStackTrace(originalErr);
                    }
                });
                buffer.reset();
                return;
            }

            buffer.write(b);

            if (b == '\n') {
                // Process complete line
                String content = new String(buffer.toByteArray(), StandardCharsets.UTF_8);
                processLine(content);
                buffer.reset();
                currentLine++;
            }
        }

        private void processLine(String content) {
            SwingUtilities.invokeLater(() -> {
                try {
                    // Add timestamp
                    String timestamp = "[" + timeFormat.format(new Date()) + "] ";
                    doc.insertString(doc.getLength(), timestamp, baseStyle);

                    // Parse and apply ANSI segments
                    List<AnsiParser.AnsiSegment> segments = AnsiParser.parse(content);
                    for (AnsiParser.AnsiSegment segment : segments) {
                        Style style = createSegmentStyle(segment);
                        doc.insertString(doc.getLength(), segment.getText(), style);
                    }

                    // Keep console scrolled to bottom
                    consoleOutput.setCaretPosition(doc.getLength());
                } catch (BadLocationException e) {
                    e.printStackTrace(originalErr);
                }
            });
        }
    }

    private Style createSegmentStyle(AnsiParser.AnsiSegment segment) {
        // Create a unique style for each segment to prevent interference
        Style style = consoleOutput.addStyle("segment-" + System.nanoTime(), baseStyle);

        if (segment.getForeground() != null) {
            StyleConstants.setForeground(style, segment.getForeground());
        }
        if (segment.getBackground() != null) {
            StyleConstants.setBackground(style, segment.getBackground());
        }
        StyleConstants.setBold(style, segment.isBold());
        StyleConstants.setItalic(style, segment.isItalic());
        StyleConstants.setUnderline(style, segment.isUnderline());
        StyleConstants.setStrikeThrough(style, segment.isStrikethrough());

        return style;
    }

    private void redirectSystemStreams() {
        ConsoleOutputStream out = new ConsoleOutputStream();
        System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(out, true, StandardCharsets.UTF_8));

    }

    public void clearConsole() {
        SwingUtilities.invokeLater(() -> {
            try {
                doc.remove(0, doc.getLength());
            } catch (BadLocationException e) {
                e.printStackTrace(originalErr);
            }
        });
    }

    public void restoreSystemStreams() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }
}
