package dev.skidfuscator.obfuscator.util.progress;

import org.jline.utils.WCWidth;
class StringDisplayUtils {
    StringDisplayUtils() {
    }

    static int getCharDisplayLength(char c) {
        return Math.max(WCWidth.wcwidth(c), 0);
    }

    static int getStringDisplayLength(String s) {
        int displayWidth = 0;

        for(int i = 0; i < s.length(); ++i) {
            displayWidth += getCharDisplayLength(s.charAt(i));
        }

        return displayWidth;
    }

    static String trimDisplayLength(String s, int maxDisplayLength) {
        if (maxDisplayLength <= 0) {
            return "";
        } else {
            int totalLength = 0;

            for(int i = 0; i < s.length(); ++i) {
                totalLength += getCharDisplayLength(s.charAt(i));
                if (totalLength > maxDisplayLength) {
                    return s.substring(0, i);
                }
            }

            return s;
        }
    }
}