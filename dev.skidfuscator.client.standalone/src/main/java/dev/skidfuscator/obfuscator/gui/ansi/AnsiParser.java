package dev.skidfuscator.obfuscator.gui.ansi;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AnsiParser {
    private static final Pattern ANSI_PATTERN = Pattern.compile("\u001B\\[([\\d;]*)([a-zA-Z])");
    private static final Map<Integer, Color> FOREGROUND_COLORS = new HashMap<>();
    private static final Map<Integer, Color> BACKGROUND_COLORS = new HashMap<>();

    static {
        // Standard colors (30-37: foreground, 40-47: background)
        initializeColorMaps();
    }

    private static void initializeColorMaps() {
        // Standard foreground colors (30-37)
        FOREGROUND_COLORS.put(30, Color.BLACK);
        FOREGROUND_COLORS.put(31, new Color(205, 49, 49));
        FOREGROUND_COLORS.put(32, new Color(13, 188, 121));
        FOREGROUND_COLORS.put(33, new Color(229, 229, 16));
        FOREGROUND_COLORS.put(34, new Color(36, 114, 200));
        FOREGROUND_COLORS.put(35, new Color(188, 63, 188));
        FOREGROUND_COLORS.put(36, new Color(17, 168, 205));
        FOREGROUND_COLORS.put(37, new Color(229, 229, 229));

        // Bright foreground colors (90-97)
        FOREGROUND_COLORS.put(90, new Color(102, 102, 102));
        FOREGROUND_COLORS.put(91, new Color(241, 76, 76));
        FOREGROUND_COLORS.put(92, new Color(35, 209, 139));
        FOREGROUND_COLORS.put(93, new Color(245, 245, 67));
        FOREGROUND_COLORS.put(94, new Color(59, 142, 234));
        FOREGROUND_COLORS.put(95, new Color(214, 112, 214));
        FOREGROUND_COLORS.put(96, new Color(41, 184, 219));
        FOREGROUND_COLORS.put(97, new Color(255, 255, 255));

        // Standard background colors (40-47)
        BACKGROUND_COLORS.put(40, Color.BLACK);
        BACKGROUND_COLORS.put(41, new Color(205, 49, 49));
        BACKGROUND_COLORS.put(42, new Color(13, 188, 121));
        BACKGROUND_COLORS.put(43, new Color(229, 229, 16));
        BACKGROUND_COLORS.put(44, new Color(36, 114, 200));
        BACKGROUND_COLORS.put(45, new Color(188, 63, 188));
        BACKGROUND_COLORS.put(46, new Color(17, 168, 205));
        BACKGROUND_COLORS.put(47, new Color(229, 229, 229));

        // Bright background colors (100-107)
        BACKGROUND_COLORS.put(100, new Color(102, 102, 102));
        BACKGROUND_COLORS.put(101, new Color(241, 76, 76));
        BACKGROUND_COLORS.put(102, new Color(35, 209, 139));
        BACKGROUND_COLORS.put(103, new Color(245, 245, 67));
        BACKGROUND_COLORS.put(104, new Color(59, 142, 234));
        BACKGROUND_COLORS.put(105, new Color(214, 112, 214));
        BACKGROUND_COLORS.put(106, new Color(41, 184, 219));
        BACKGROUND_COLORS.put(107, new Color(255, 255, 255));
    }

    public static List<AnsiSegment> parse(String text) {
        List<AnsiSegment> segments = new ArrayList<>();
        AnsiState currentState = new AnsiState();

        Matcher matcher = ANSI_PATTERN.matcher(text);
        int lastEnd = 0;

        while (matcher.find()) {
            // Add text before the ANSI sequence
            if (matcher.start() > lastEnd) {
                String textSegment = text.substring(lastEnd, matcher.start());
                segments.add(new AnsiSegment(textSegment, currentState));
            }

            // Parse the ANSI sequence
            String[] codes = matcher.group(1).split(";");
            String command = matcher.group(2);

            // Handle the ANSI sequence
            handleAnsiSequence(codes, command, currentState);

            lastEnd = matcher.end();
        }

        // Add remaining text after last ANSI sequence
        if (lastEnd < text.length()) {
            String textSegment = text.substring(lastEnd);
            segments.add(new AnsiSegment(textSegment, currentState));
        }

        return segments;
    }

    private static void handleAnsiSequence(String[] codes, String command, AnsiState state) {
        if (command.equals("m")) {
            for (String code : codes) {
                if (code.isEmpty()) continue;
                int codeNum = Integer.parseInt(code);

                switch (codeNum) {
                    case 0: // Reset
                        state.reset();
                        break;
                    case 1: // Bold
                        state.setBold(true);
                        break;
                    case 3: // Italic
                        state.setItalic(true);
                        break;
                    case 4: // Underline
                        state.setUnderline(true);
                        break;
                    case 9: // Strikethrough
                        state.setStrikethrough(true);
                        break;
                    case 22: // Normal intensity (not bold)
                        state.setBold(false);
                        break;
                    case 23: // Not italic
                        state.setItalic(false);
                        break;
                    case 24: // Not underlined
                        state.setUnderline(false);
                        break;
                    case 29: // Not strikethrough
                        state.setStrikethrough(false);
                        break;
                    default:
                        if (codeNum >= 30 && codeNum <= 37 || codeNum >= 90 && codeNum <= 97) {
                            state.setForeground(FOREGROUND_COLORS.get(codeNum));
                        } else if (codeNum >= 40 && codeNum <= 47 || codeNum >= 100 && codeNum <= 107) {
                            state.setBackground(BACKGROUND_COLORS.get(codeNum));
                        } else if (codeNum == 38 || codeNum == 48) {
                            // Handle RGB and indexed colors in the future
                        }
                        break;
                }
            }
        }
    }

    public static class AnsiSegment {
        private final String text;
        private final AnsiState state;

        public AnsiSegment(String text, AnsiState state) {
            this.text = text;
            try {
                this.state = (AnsiState) state.clone();
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public String toString() {
            return "AnsiSegment{" +
                    "text='" + text + '\'' +
                    '}';
        }

        public String getText() { return text; }
        public Color getForeground() { return state.getForeground(); }
        public Color getBackground() { return state.getBackground(); }
        public boolean isBold() { return state.isBold(); }
        public boolean isItalic() { return state.isItalic(); }
        public boolean isUnderline() { return state.isUnderline(); }
        public boolean isStrikethrough() { return state.isStrikethrough(); }
    }
}
