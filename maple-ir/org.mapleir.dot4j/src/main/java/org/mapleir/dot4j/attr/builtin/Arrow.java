package org.mapleir.dot4j.attr.builtin;

import static org.mapleir.dot4j.attr.Attrs.*;

import org.mapleir.dot4j.attr.Attr;
import org.mapleir.dot4j.attr.Attrs;

public class Arrow extends Attr<String> {
    public enum DirType {
        FORWARD, BACK, BOTH, NONE
    }

    public static final Arrow
            BOX = new Arrow("box"),
            CROW = new Arrow("crow"),
            CURVE = new Arrow("curve"),
            DIAMOND = new Arrow("diamond"),
            DOT = new Arrow("dot"),
            ICURVE = new Arrow("icurve"),
            INV = new Arrow("inv"),
            NONE = new Arrow("none"),
            NORMAL = new Arrow("normal"),
            TEE = new Arrow("tee"),
            VEE = new Arrow("vee");

    private Arrow(String key, String value) {
        super(key, value);
    }

    private Arrow(String value) {
        super("arrowhead", value);
    }

    public Arrow tail() {
        return key("arrowtail");
    }

    public Arrow open() {
        return value(value.charAt(0) == 'o' ? value : ("o" + value));
    }

    public Arrow left() {
        return arrowDir("l");
    }

    public Arrow right() {
        return arrowDir("r");
    }

    public Arrow and(Arrow arrow) {
        return value(arrow.value + value);
    }

    public Attrs size(double size) {
        return config(size, null);
    }

    public Attrs dir(DirType type) {
        return config(0, type);
    }

    public Attrs config(double size, DirType type) {
    	Attrs a = this;
        if (size > 0) {
            a = attrs(a, attr("arrowsize", size));
        }
        if (type != null) {
            a = attrs(a, attr("dir", type.name().toLowerCase()));
        }
        return a;
    }

    private Arrow arrowDir(String dir) {
        switch (value.charAt(0)) {
            case 'l':
            case 'r':
                return value(dir + value.substring(1));
            case 'o':
                final char s = value.charAt(1);
                return value("o" + dir + (s == 'r' || s == 'l' ? value.substring(2) : value.substring(1)));
            default:
                return value(dir + value);
        }
    }
}
