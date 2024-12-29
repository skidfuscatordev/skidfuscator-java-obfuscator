package dev.skidfuscator.obfuscator.transform.impl.string.generator;

import dev.skidfuscator.obfuscator.skidasm.SkidClassNode;
import dev.skidfuscator.obfuscator.skidasm.SkidMethodNode;
import dev.skidfuscator.obfuscator.skidasm.cfg.SkidBlock;
import org.mapleir.ir.code.Expr;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.HashMap;
import java.util.Map;

public interface EncryptionGeneratorV3 {
    Expr encrypt(String input, final SkidMethodNode node, final SkidBlock block);

    String decrypt(DecryptorDictionary input, int key);

    void visitPre(final SkidClassNode node);

    default void visitPost(final SkidClassNode node) {};

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface InjectMethod {
        // empty
        String value();
        InjectMethodTag[] tags() default {};
    }

    enum InjectMethodTag {
        RANDOM_NAME
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface InjectField {
        // empty
        String value();
        InjectFieldTag[] tags() default {};
    }

    enum InjectFieldTag {
        RANDOM_NAME,
        FINAL,
        NO_INTERFACE_COMPAT
    }

    class DecryptorDictionary {
        private final Map<String, DecryptorItem<?>> items;

        public DecryptorDictionary(Map<String, DecryptorItem<?>> items) {
            this.items = items;
        }

        public <T> T get(String key) {
            return (T) items.get(key).getKey();
        }

        public DecryptorDictionary of(String key, Object value) {
            items.put(key, new DecryptorItem<>(key, value));
            return this;
        }

        public static DecryptorDictionary create() {
            return new DecryptorDictionary(new HashMap<>());
        }
    }

    class DecryptorItem<T> {
        private final String key;
        private final T value;

        public DecryptorItem(String key, T value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public T getValue() {
            return value;
        }
    }
}
