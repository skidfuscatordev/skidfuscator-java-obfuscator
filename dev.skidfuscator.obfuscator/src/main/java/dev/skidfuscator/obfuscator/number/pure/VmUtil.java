package dev.skidfuscator.obfuscator.number.pure;

import dev.xdark.ssvm.VirtualMachine;
import dev.xdark.ssvm.invoke.Argument;
import lombok.experimental.UtilityClass;
import org.mapleir.ir.code.expr.ConstantExpr;
import org.objectweb.asm.Type;

import java.nio.charset.StandardCharsets;

@UtilityClass
public class VmUtil {
    public Argument getArgument(final VirtualMachine vm, final Object cnst, final Type argType) {
        if (cnst == null) {
            return Argument.reference(vm.getMemoryManager().nullValue());
        }

        switch (argType.getSort()) {
            case Type.OBJECT:
                if (cnst == null) {
                    return Argument.reference(vm.getMemoryManager().nullValue());
                }
                if (cnst instanceof String) {
                    final String utfed = convertUtf16ToUtf8((String) cnst);
                    return Argument.reference(
                            vm.getOperations().newUtf8(utfed)
                    );
                }

                throw new IllegalStateException(String.format("Unsupported type: %s", cnst.getClass()));
            case Type.BOOLEAN:
                if (cnst instanceof Boolean) {
                    return Argument.int32((Boolean) cnst ? 1 : 0);
                }

                // check that value is 0 or 1, if not throw exception
                if (cnst instanceof Number) {
                    final int value = ((Number) cnst).intValue();
                    if (value != 0 && value != 1) {
                        throw new IllegalStateException(String.format(
                                "Unsupported boolean value: %d",
                                value
                        ));
                    }
                } else {
                    throw new IllegalStateException(String.format(
                            "Unsupported boolean value: %s",
                            cnst.getClass().getName()
                    ));
                }

                return Argument.int32(((Number) cnst).intValue());
                case Type.CHAR:
                // TODO: Investigate why constant computes type as byte but is truly a char
                final Type computeType = ConstantExpr.computeType(cnst);

                // Debug char value to int value vs vmcharacter
                //System.out.println("Char: " + cnst + " -> " + ((int) (char) cnst));
                //System.out.println("VmChar: " + cnst + " -> " + VmCharacter.toDigit((Character) cnst));

                switch (computeType.getSort()) {
                    case Type.BYTE:
                    case Type.SHORT:
                    case Type.INT:
                        return Argument.int32(((Number) cnst).intValue());
                    case Type.CHAR:
                        return Argument.int32((char) cnst);
                }

                throw new IllegalStateException(String.format(
                            "Unsupported type: %s",
                            cnst.getClass().getName()
                ));
            case Type.BYTE:
            case Type.SHORT:
            case Type.INT:
                // failsafe for miscalculations
                final Type computedType = ConstantExpr.computeType(cnst);

                // check if computed type is char
                if (computedType.getSort() == Type.CHAR) {
                    return Argument.int32((char) cnst);
                }

                return Argument.int32(cnst instanceof Boolean ? ((Boolean) cnst ? 1 : 0) : ((Number) cnst).intValue());
            case Type.FLOAT:
                return Argument.float32((float) cnst);
            case Type.LONG:
                return Argument.int64((long) cnst);
            case Type.DOUBLE:
                return Argument.float64((double) cnst);
            default:
                throw new IllegalStateException(String.format(
                    "Unsupported type: %s",
                    cnst.getClass().getName()
                ));
        }
    }

    /**
     * Converts a String encoded in UTF-16 to a String encoded in UTF-8.
     *
     * @param utf16String The input String object containing UTF-16 encoded data.
     * @return A new String object representing the same text encoded in UTF-8.
     */
    public String convertUtf16ToUtf8(String utf16String) {
        // Since Java strings are internally UTF-16, we directly use the input string
        // to convert it to a byte array in UTF-8, then construct a new String with UTF-8 encoding
        byte[] utf8Bytes = utf16String.getBytes(StandardCharsets.UTF_8);

        // Return a new String object, note: this step is somewhat artificial in this context
        // as Java's String are inherently UTF-16. This merely demonstrates conversion to UTF-8 bytes
        // and then constructing a String for demonstration.
        return new String(utf8Bytes, StandardCharsets.UTF_8);
    }

    /**
     * Converts a String encoded in UTF-16 to a String encoded in UTF-8.
     *
     * @param utf16String The input String object containing UTF-16 encoded data.
     * @return A new String object representing the same text encoded in UTF-8.
     */
    public String convertUtf8ToUtf16(String utf16String) {
        // Since Java strings are internally UTF-16, we directly use the input string
        // to convert it to a byte array in UTF-8, then construct a new String with UTF-8 encoding
        byte[] utf8Bytes = utf16String.getBytes(StandardCharsets.UTF_16);

        // Return a new String object, note: this step is somewhat artificial in this context
        // as Java's String are inherently UTF-16. This merely demonstrates conversion to UTF-8 bytes
        // and then constructing a String for demonstration.
        return new String(utf8Bytes, StandardCharsets.UTF_16);
    }
}
