package sdk;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SDK {
    public static String hash(String s) {
        return LongHashFunction.xx3().hashChars(s) + "";
    }

    /** Reversible UTF-16 masking for comparison literals; this is obfuscation, not a secret key. */
    public static String maskComparisonLiteral(String value, long key) {
        char[] characters = value.toCharArray();
        for (int i = 0; i < characters.length; i++) {
            long mask = Long.rotateRight(key, (i & 3) * 16) ^ (i * 0x9e37) ^ 0x5a5a;
            characters[i] = (char) (characters[i] ^ mask);
        }
        return new String(characters);
    }

    public static boolean equalsConstantReceiver(String encoded, Object argument, long key) {
        return maskComparisonLiteral(encoded, key).equals(argument);
    }

    public static boolean equalsConstantArgument(String receiver, String encoded, long key) {
        return receiver.equals(maskComparisonLiteral(encoded, key));
    }

    public static boolean equalsIgnoreCaseConstantReceiver(String encoded, String argument, long key) {
        return maskComparisonLiteral(encoded, key).equalsIgnoreCase(argument);
    }

    public static boolean equalsIgnoreCaseConstantArgument(String receiver, String encoded, long key) {
        return receiver.equalsIgnoreCase(maskComparisonLiteral(encoded, key));
    }

    private static final Map<Class<?>, String> TYPE_HASHES = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Set<String>> HIERARCHY_HASHES = new ConcurrentHashMap<>();

    public static boolean checkType(Object obj, String typeHash, int seed) {
        if (obj == null) {
            return false;
        }

        Class<?> objClass = obj.getClass();

        // Get or compute the full hierarchy hash set for this class
        Set<String> hierarchyHashes = HIERARCHY_HASHES.computeIfAbsent(
                objClass,
                e -> computeHierarchyHashes(objClass, seed)
        );

        return hierarchyHashes.contains(typeHash);
    }

    private static Set<String> computeHierarchyHashes(Class<?> cls, int seed) {
        Set<String> hashes = new HashSet<>();
        Queue<Class<?>> toProcess = new LinkedList<>();
        Set<Class<?>> processed = new HashSet<>();

        toProcess.add(cls);

        while (!toProcess.isEmpty()) {
            Class<?> current = toProcess.poll();

            if (current == null || !processed.add(current)) {
                continue;
            }

            // Add hash for current class
            hashes.add(TYPE_HASHES.computeIfAbsent(current,
                    c -> "" + LongHashFunction.xx3(seed)
                            .hashChars(c.getName().replace('.', '/'))));

            // Add superclass to process
            Class<?> superClass = current.getSuperclass();
            if (superClass != null) {
                toProcess.add(superClass);
            }

            // Add all interfaces to process (including inherited interfaces)
            toProcess.addAll(Arrays.asList(current.getInterfaces()));
        }

        return Collections.unmodifiableSet(hashes);
    }

    public static boolean checkEnumName(Enum<?> enumValue, String hashedName, int seed) {
        if (enumValue == null) {
            return false;
        }

        // Hash the enum's name and compare with the provided hash
        String actualHash = LongHashFunction.xx3(seed).hashChars(enumValue.name()) + "";
        return actualHash.equals(hashedName);
    }
}
