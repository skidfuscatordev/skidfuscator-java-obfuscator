package dev.skidfuscator.test.exclusion;

import dev.skidfuscator.core.TestSkidfuscator;
import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.testclasses.exclusion.ObfuscatedTestClass;
import dev.skidfuscator.testclasses.exclusion.UnobfuscatedTestClass;
import org.junit.jupiter.api.Test;
import org.topdank.byteengineer.commons.data.JarClassData;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ExclusionTest {
    private Skidfuscator skidfuscator;

    @Test
    public void testExcludeAnnotation() {
        skidfuscator = new TestSkidfuscator(
                new Class[]{
                        ObfuscatedTestClass.class,
                        UnobfuscatedTestClass.class
                },
                this::callback
        );

        skidfuscator.run();
    }

    private void callback(List<Map.Entry<String, byte[]>> entries) {
        final Map<String, JarClassData> storage = skidfuscator
                .getJarContents()
                .getClassContents()
                .namedMap();

        for (Map.Entry<String, byte[]> entry : entries) {
            final String name = entry.getKey();
            final byte[] obfed = entry.getValue();

            final byte[] stored = storage
                    .get(name + ".class")
                    .getData();

            switch (name) {
                case "dev/skidfuscator/testclasses/exclusion/ObfuscatedTestClass": {
                    assert !Arrays.equals(stored, obfed)
                            : "Obfuscated class's data was not modified!";
                    break;
                }

                case "dev/skidfuscator/testclasses/exclusion/UnobfuscatedTestClass.java":
                    assert Arrays.equals(stored, obfed)
                            : "Excluded class's data was modified!";
                    break;
            }
        }
    }
}
