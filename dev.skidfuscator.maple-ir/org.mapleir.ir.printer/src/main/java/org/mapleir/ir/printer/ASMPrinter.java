package org.mapleir.ir.printer;

import static org.mapleir.ir.printer.Util.isNonEmpty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.mapleir.propertyframework.api.IProperty;
import org.mapleir.propertyframework.api.IPropertyDictionary;
import org.mapleir.propertyframework.impl.BooleanProperty;
import org.mapleir.propertyframework.impl.StringProperty;
import org.mapleir.stdlib.util.TabbedStringWriter;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;

public abstract class ASMPrinter<E> extends Printer<E> {

    public static final String PROP_EXPAND_ACCESS_FLAGS = "mapleir.ir.printer.expandAccessFlags";
    public static final String PROP_ACCESS_FLAG_VERBOSITY = "mapleir.ir.printer.accessFlagVerbosity";
    public static final String PROP_ACCESS_FLAG_SAFE = "mapleir.ir.printer.safeAccessFlags";

    public static final String OPT_VERBOSITY_PRETTY = "pretty";
    public static final String OPT_VERBOSITY_DEFAULT = OPT_VERBOSITY_PRETTY;
    public static final String OPT_VERBOSITY_FULL = "full";

    private final Map<Integer, String> accessFlagTags;
    protected final IProperty<Boolean> expandAccessFlags;
    protected final IProperty<String> accessFlagVerbosity;
    protected final IProperty<Boolean> safeAccessFlag;

    public ASMPrinter(TabbedStringWriter sw, IPropertyDictionary settingsDict,
            String[] opcodesFieldNames) {
        super(sw, settingsDict);

        this.accessFlagTags = Collections.unmodifiableMap(Util.decodeASMFlags(opcodesFieldNames));

        this.expandAccessFlags = new BooleanProperty(PROP_EXPAND_ACCESS_FLAGS, true);
        this.expandAccessFlags.tryLinkDictionary(settingsDict);

        this.accessFlagVerbosity = new StringProperty(PROP_ACCESS_FLAG_VERBOSITY,
                OPT_VERBOSITY_DEFAULT);
        this.accessFlagVerbosity.tryLinkDictionary(settingsDict);

        this.safeAccessFlag = new BooleanProperty(PROP_ACCESS_FLAG_SAFE, false);
        this.safeAccessFlag.tryLinkDictionary(settingsDict);
    }

    protected void emitAccessDirective(int access) {
        if (this.shouldExpandAccessFlags()) {
            this.emitRawDirective("access", this.compileAccessFlags(access));
        } else {
            this.emitDirective("access", access);
        }
    }

    protected boolean shouldExpandAccessFlags() {
        return this.expandAccessFlags.getValue();
    }

    public String compileAccessFlags(int flags) {
        return compileAccessFlags(flags, this.accessFlagTags.keySet(),
                this.getAccessTranslationFunction(this.accessFlagTags));
    }

    public static String compileAccessFlags(int flags, Set<Integer> keys,
            Function<Integer, String> tagNamer) {
        List<String> tags = new ArrayList<>();

        for (int k : keys) {
            if ((flags & k) != 0) {
                tags.add(tagNamer.apply(k));
            }
        }

        StringBuilder sb = new StringBuilder().append('(');

        Iterator<String> it = tags.iterator();
        while (it.hasNext()) {
            sb.append(it.next());

            if (it.hasNext()) {
                sb.append("|");
            }
        }

        return sb.append(')').toString();
    }

    protected Function<Integer, String> getAccessTranslationFunction(
            Map<Integer, String> translationTable) {
        return getAccessTranslationFunction(translationTable,
                this.accessFlagVerbosity.getValue().equals(OPT_VERBOSITY_FULL),
                this.safeAccessFlag.getValue());
    }

    protected static Function<Integer, String> getAccessTranslationFunction(
            Map<Integer, String> translationTable, boolean verbose, boolean safe) {
        return new Function<Integer, String>() {
            @Override
            public String apply(Integer t) {
                String tag = translationTable.get(t);
                if (tag == null) {
                    throw new UnsupportedOperationException(
                            String.format("No tag for: 0x%s", Integer.toHexString(t)));
                }
                if (!verbose) {
                    tag = tag.substring(4);
                }
                if (safe) {
                    tag = "I\"" + tag + "\"";
                }
                return tag;
            }
        };
    }

    public void emitNodeAttributes(List<Attribute> attrs) {
        for (int i = 0; i < attrs.size(); i++) {
            Attribute attr = attrs.get(i);

            if (i < (attrs.size() - 1)) {
                Attribute next = attrs.get(i + 1);
            }
        }

        this.emitDirective("attrs", attrs);
    }

    @Override
    public void emitLiteral(Object o) {
        if (o instanceof AnnotationNode) {
            AnnotationNode an = (AnnotationNode) o;
            Map<Object, Object> map = new HashMap<>();
            map.put("desc", an.desc);

            if (isNonEmpty(an.values)) {
                List<Object> valuesList = new ArrayList<>();

                List<Object> realValues = an.values;
                if ((realValues.size() % 2) != 0) {
                    throw new IllegalStateException(String.format("AN values: %s", realValues));
                }

                for (int i = 0; i < realValues.size() / 2; i++) {
                    int j = (i * 2);
                    String key = (String) realValues.get(j);
                    Object val = realValues.get(j + 1);

                    Map<String, Object> innerMap = new HashMap<>();
                    innerMap.put(key, val);
                    valuesList.add(innerMap);
                }
                map.put("values", valuesList);
            }
            this.emitDirectiveValue(map);
        } else if (o instanceof Attribute) {
            Attribute attr = (Attribute) o;

            Map<String, Object> map = new HashMap<>();
            map.put("type", attr.type);
            this.emitDirectiveValue(map);
            throw new UnsupportedOperationException("TODO");
        } else if (o instanceof Type) {
            this.sw.print("T\"").print(o.toString()).print("\"");
        } else {
            super.emitLiteral(o);
        }
    }
}
