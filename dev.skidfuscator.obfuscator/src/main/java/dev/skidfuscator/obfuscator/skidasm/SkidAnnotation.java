package dev.skidfuscator.obfuscator.skidasm;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.util.TypeUtil;
import dev.skidfuscator.obfuscator.util.misc.Parameter;
import lombok.Data;
import org.mapleir.asm.ClassNode;
import org.mapleir.asm.MethodNode;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Wrapper for the ASM annotations to allow support for annotation obfuscation. This is stored in
 * @see dev.skidfuscator.obfuscator.hierarchy.Hierarchy
 *
 * Cool stuff really. It allows to dynamically set the values directly without interacting with
 * ASM. This also stores important stuff such as object value, type, header name etc... in a
 * clean and elegant fashion.
 */
public class SkidAnnotation {
    private final AnnotationNode node;
    private final AnnotationType type;
    private final Skidfuscator skidfuscator;
    private final ClassNode parent;
    private final Map<String, AnnotationValue<?>> values = new HashMap<>();

    public SkidAnnotation(AnnotationNode node, AnnotationType type, Skidfuscator skidfuscator, ClassNode parent) {
        this.node = node;
        this.type = type;
        this.skidfuscator = skidfuscator;
        this.parent = parent;

        this.parse();
    }

    /**
     * @param name Name of the value sought out to be modified (eg @Value(value = "123) )
     *                                                                    ^^^^^ this bit
     * @param <T> Type of the value sought out to be modified
     * @return Annotation Value subclass with the getter and the setter
     */
    public <T> AnnotationValue<T> getValue(String name) {
        return (AnnotationValue) values.get(name);
    }

    /**
     * @param name Name of the value sought out to be modified (eg @Value(value = "123) )
     *                                                                    ^^^^^ this bit
     * @param <T> Type of the value sought out to be modified
     * @return Annotation Value subclass with the getter and the setter
     */
    public <T> void setValue(String name, T value) {
        int i;

        if (values.containsKey(name)) {
            i = values.get(name).index;
        } else {
            i = values.size();
        }

        final int finalI = i;
        final String finalName = name;
        final AnnotationValue<T> array = new AnnotationValue<>(
                name,
                finalI,
                new Consumer<T>() {
                    @Override
                    public void accept(T o) {
                        node.values.set(finalI, o);
                    }
                },
                new Supplier<T>(){
                    @Override
                    public T get() {
                        return (T) node.values.get(finalI);
                    }
                },
                parent.getMethods()
                        .stream()
                        .filter(e -> e.getName().equals(finalName))
                        .findFirst()
                        .orElseThrow(() -> {
                            return new IllegalStateException(
                                    "Failed to find method for "
                                            + finalName + " of value "
                                            + node.values.get(finalI)
                                            + " (parent: " + parent.getMethods().stream()
                                            .map(e -> e.getName() + "#" + e.getDesc())
                                            .collect(Collectors.joining("\n"))
                                            + ")"
                            );
                        }));
        array.set(value);
        values.put(name, array);
    }

    private void updateDesc() {
        if (values.size() == 1) {
            node.desc = null;
            return;
        }

        final Parameter parameter = new Parameter("()V");
        values.entrySet()
                .stream()
                .sorted(Comparator.comparingInt(e -> e.getValue().index))
                .collect(Collectors.toList());
    }

    /**
     * @return Annotation ASM node to be used
     */
    public AnnotationNode getNode() {
        return node;
    }

    /**
     * @return Annotation ASM type for debugging purposes (need to inform myself on the difference)
     */
    public AnnotationType getType() {
        return type;
    }

    /**
     * @return Value map with all the values and their headings
     */
    public Map<String, AnnotationValue<?>> getValues() {
        return values;
    }

    /**
     * @return Parent class which defines the annotation
     */
    public ClassNode getParent() {
        return parent;
    }

    /**
     * @deprecated PLEASE DO NOT USE THIS IT IS PISS POOR PRACTICE
     * @return Skidfuscator instance (bad bad bad practice to be using this)
     */
    @Deprecated
    public Skidfuscator getSkidfuscator() {
        return skidfuscator;
    }

    /**
     * Function which serves the purpose of parsing an annotation into values that
     * can directly virtually edit the annotation
     */
    private void parse() {
        if (node.values == null || node.values.size() == 0) {
            return;
        }

        String name = null;
        if (node.desc == null) {
            values.put("value", new AnnotationValue<>(
                    "value",
                    0,
                    new Consumer<Object>() {
                        @Override
                        public void accept(Object o) {
                            node.values.set(0, o);
                        }
                    },
                    new Supplier<Object>(){
                        @Override
                        public Object get() {
                            return node.values.get(0);
                        }
                    },
                    parent.getMethods().get(0)
            ));
            return;
        }

        for (int i = 0; i < this.node.values.size(); i++) {
            if (i % 2 == 0) {
                // This is the name
                name = (String) node.values.get(i);
            } else {
                final int finalI = i;
                String finalName = name;
                values.put(name, new AnnotationValue<>(
                        name,
                        finalI,
                        new Consumer<Object>() {
                            @Override
                            public void accept(Object o) {
                                node.values.set(finalI, o);
                            }
                        },
                        new Supplier<Object>(){
                            @Override
                            public Object get() {
                                return node.values.get(finalI);
                            }
                        },
                        parent.getMethods()
                                .stream()
                                .filter(e -> e.getName().equals(finalName))
                                .findFirst()
                                .orElseThrow(() -> {
                                    return new IllegalStateException(
                                            "Failed to find method for "
                                            + finalName + " of value "
                                            + node.values.get(finalI)
                                            + " (parent: " + parent.getMethods().stream()
                                                    .map(e -> e.getName() + "#" + e.getDesc())
                                                    .collect(Collectors.joining("\n"))
                                            + ")"
                                    );
                                })
                ));
            }
        }
    }

    public static class AnnotationValue<T> {
        private final String name;

        private final int index;
        private final Type type;
        private final Consumer<T> setter;
        private final Supplier<T> getter;
        private final MethodNode methodNode;

        public AnnotationValue(String name, int index, Consumer<T> setter, Supplier<T> getter, MethodNode methodNode) {
            this.name = name;
            this.index = index;
            this.setter = setter;
            this.getter = getter;
            this.methodNode = methodNode;
            this.type = get() == null ? TypeUtil.STRING_TYPE : Type.getType(get().getClass());
        }

        public String getName() {
            return name;
        }

        public int getIndex() {
            return index;
        }

        public Type getType() {
            return type;
        }

        public MethodNode getMethodNode() {
            return methodNode;
        }

        public void set(final T t) {
            setter.accept(t);
        }

        public T get() {
            return getter.get();
        }
    }

    public enum AnnotationType {
        VISIBLE(false),
        INVISIBLE(false),
        TYPE_VISIBLE(true),
        TYPE_INVISIBLE(true);

        private final boolean type;

        AnnotationType(boolean type) {
            this.type = type;
        }

        public boolean isType() {
            return type;
        }
    }
}
