package org.mapleir.dot4j.attr.builtin;

import static java.util.stream.Collectors.joining;
import static org.mapleir.dot4j.attr.Attrs.*;

import java.util.Arrays;

import org.mapleir.dot4j.attr.Attrs;

public class Records {

    private static final String SHAPE = "shape";
    private static final String RECORD = "record";
    private static final String LABEL = "label";

    private Records() {
    }

    public static Attrs label(String label) {
        return attrs(attr(SHAPE, RECORD), attr(LABEL, label));
    }

    public static Attrs mLabel(String label) {
        return attrs(attr(SHAPE, "Mrecord"), attr(LABEL, label));
    }

    public static Attrs of(String... recs) {
        return attrs(attr(SHAPE, RECORD), attr(LABEL, Arrays.stream(recs).collect(joining("|"))));
    }

    public static Attrs mOf(String... recs) {
        return attrs(attr(SHAPE, "Mrecord"), attr(LABEL, Arrays.stream(recs).collect(joining("|"))));
    }

    public static String rec(String tag, String label) {
        return "<" + tag + ">" + rec(label);
    }

    public static String rec(String label) {
        return label.replace("{", "\\{").replace("}", "\\}")
                .replace("<", "\\<").replace(">", "\\>")
                .replace("|", "\\|").replace(" ", "\\ ");
    }

    public static String turn(String... records) {
        return "{" + Arrays.stream(records).collect(joining("|")) + "}";
    }
}
