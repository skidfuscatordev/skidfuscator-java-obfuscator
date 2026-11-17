package sdk;

import java.util.*;
import java.util.concurrent.atomic.AtomicReferenceArray;

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
        return comparisonLiteral(encoded, key).equals(argument);
    }

    public static boolean equalsConstantArgument(String receiver, String encoded, long key) {
        return receiver.equals(comparisonLiteral(encoded, key));
    }

    public static boolean equalsIgnoreCaseConstantReceiver(String encoded, String argument, long key) {
        return comparisonLiteral(encoded, key).equalsIgnoreCase(argument);
    }

    public static boolean equalsIgnoreCaseConstantArgument(String receiver, String encoded, long key) {
        return receiver.equalsIgnoreCase(comparisonLiteral(encoded, key));
    }

    /** Class identity includes its defining loader. No Class objects are retained. */
    public static boolean checkType(Object value, Class<?> target) {
        return target.isInstance(value);
    }

    // Fixed-size, lock-free cache. Collisions replace entries, never change results.
    // At most 256 literals of 1024 UTF-16 units each; no Class/ClassLoader keys.
    private static final AtomicReferenceArray<Literal> LITERALS = new AtomicReferenceArray<>(256);
    public static String comparisonLiteral(String encoded, long key) {
        if (encoded.length() > 1024) return maskComparisonLiteral(encoded, key);
        int slot = (encoded.hashCode() ^ (int) key ^ (int) (key >>> 32)) & 255;
        Literal found = LITERALS.get(slot);
        if (found != null && found.key == key && found.encoded.equals(encoded)) return found.decoded;
        String decoded = maskComparisonLiteral(encoded, key);
        LITERALS.set(slot, new Literal(encoded, key, decoded));
        return decoded;
    }

    private static final class Literal {
        final String encoded;
        final long key;
        final String decoded;
        Literal(String encoded, long key, String decoded) {
            this.encoded = encoded; this.key = key; this.decoded = decoded;
        }
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
