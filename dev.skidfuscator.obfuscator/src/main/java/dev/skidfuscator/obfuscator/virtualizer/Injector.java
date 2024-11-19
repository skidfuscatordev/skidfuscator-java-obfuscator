package dev.skidfuscator.obfuscator.virtualizer;

import dev.skidfuscator.obfuscator.transform.impl.string.generator.v3.AbstractEncryptionGeneratorV3;

import java.util.HashMap;
import java.util.Map;

public class Injector {
    public Injector() {
        read();
    }

    private final InjectMapping methodMapping = new InjectMapping();
    private final InjectMapping fieldMapping = new InjectMapping();

    private void read() {

    }

    public @interface InjectMethod {
        String value();
    }

    public @interface InjectField {
        String value();
    }

    public enum InjectType {
        RANDOMIZE_NAME
    }

    static class InjectMapping {
        private final Map<String, String> fieldMap = new HashMap<>();

        public void mapInject(String original, String mapped) {
            fieldMap.put(original, mapped);
        }

        public String getMapping(String original) {
            return fieldMap.get(original);
        }
    }
}
