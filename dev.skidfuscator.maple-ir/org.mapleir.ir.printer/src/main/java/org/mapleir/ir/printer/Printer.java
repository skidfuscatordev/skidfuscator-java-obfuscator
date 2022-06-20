package org.mapleir.ir.printer;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.mapleir.propertyframework.api.IPropertyDictionary;
import org.mapleir.stdlib.util.TabbedStringWriter;

public abstract class Printer<E> {

    protected final TabbedStringWriter sw;
    protected final IPropertyDictionary settingsDict;

    public Printer(TabbedStringWriter sw, IPropertyDictionary settingsDict) {
        this.sw = sw;
        this.settingsDict = settingsDict;
    }

    public abstract void print(E e);

    protected void emitRawDirective(String key, String value) {
        this.sw.newline().print(".set ").print(key).print(" ").print(value);
    }

    public void emitDirective(String key, Object value) {
        this.sw.newline().print(".set ").print(key).print(" ");
        this.emitDirectiveValue(value, false);
    }

    public void emitDirectiveValue(Object value) {
        this.emitDirectiveValue(value, true);
    }

    public void emitDirectiveMap(Map<?, ?> map, boolean mapNewLine) {
        if (map.size() > 0) {
            if (map.size() == 1) {
                Entry<?, ?> e = map.entrySet().iterator().next();
                this.sw.print('{');
                this.sw.print(String.valueOf(e.getKey())).print(" = ");
                this.emitDirectiveValue(e.getValue());
                this.sw.print('}');
            } else {
                if (mapNewLine) {
                    this.sw.tab().newline();
                }

                this.sw.print("{").tab();
                Iterator<?> it = map.entrySet().iterator();
                while (it.hasNext()) {
                    Entry<?, ?> e = (Entry<?, ?>) it.next();
                    this.sw.newline().print(String.valueOf(e.getKey())).print(" = ");
                    this.emitDirectiveValue(e.getValue());

                    if (it.hasNext()) {
                        this.sw.print(", ");
                    }
                }
                this.sw.untab().newline().print('}');

                if (mapNewLine) {
                    this.sw.untab();
                }
            }
        } else {
            this.sw.print(" {}");
        }
    }

    private void emitCollection(Iterator<?> it) {
        this.sw.print("[");
        int startLine = this.sw.getLineCount();

        while (it.hasNext()) {
            this.emitDirectiveValue(it.next());

            if (it.hasNext()) {
                this.sw.print(", ");
            }
        }

        if (this.sw.getLineCount() > startLine) {
            this.sw.newline();
        }
        this.sw.print("]");
    }

    public void emitArray(Object[] arr) {
        this.sw.print("[");
        int startLine = this.sw.getLineCount();

        for (int i = 0; i < arr.length; i++) {
            Object e = arr[i];
            this.emitDirectiveValue(e);

            if (i < (arr.length - 1)) {
                this.sw.print(", ");
            }
        }

        if (this.sw.getLineCount() > startLine) {
            this.sw.newline();
        }
        this.sw.print("]");
    }

    public void emitDirectiveValue(Object value, boolean mapNewLine) {
        if (value == null) {
            this.emitLiteral(value);
        } else if (value instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) value;
            this.emitDirectiveMap(map, mapNewLine);
        } else if (value instanceof Collection) {
            this.emitCollection(((Collection<?>) value).iterator());
        } else if (value.getClass().isArray()) {
            this.emitArray((Object[]) value);
        } else {
            this.emitLiteral(value);
        }
    }

    public void emitLiteral(Object o) {
        if (o == null) {
            this.sw.print("null");
        } else if (o instanceof String) {
            this.sw.print('"').print(o.toString()).print('"');
        } else {
            this.sw.print(o.toString());
        }
    }
}
