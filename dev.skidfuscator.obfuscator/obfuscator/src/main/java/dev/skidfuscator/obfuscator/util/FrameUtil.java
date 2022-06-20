package dev.skidfuscator.obfuscator.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class FrameUtil {
    public final int SAME_FRAME = 0;
    public final int SAME_LOCALS_1_STACK_ITEM_FRAME = 64;
    public final int RESERVED = 128;
    public final int SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED = 247;
    public final int CHOP_FRAME = 248;
    public final int SAME_FRAME_EXTENDED = 251;
    public final int APPEND_FRAME = 252;
    public final int FULL_FRAME = 255;

    public final int ITEM_TOP = 0;
    public final int ITEM_INTEGER = 1;
    public final int ITEM_FLOAT = 2;
    public final int ITEM_DOUBLE = 3;
    public final int ITEM_LONG = 4;
    public final int ITEM_NULL = 5;
    public final int ITEM_UNINITIALIZED_THIS = 6;
    public final int ITEM_OBJECT = 7;
    public final int ITEM_UNINITIALIZED = 8;
    // Additional, ASM specific constants used in abstract types below.
    public final int ITEM_ASM_BOOLEAN = 9;
    public final int ITEM_ASM_BYTE = 10;
    public final int ITEM_ASM_CHAR = 11;
    public final int ITEM_ASM_SHORT = 12;

    // The size and offset in bits of each field of an abstract type.

    public final int DIM_SIZE = 6;
    public final int KIND_SIZE = 4;
    public final int FLAGS_SIZE = 2;
    public final int VALUE_SIZE = 32 - DIM_SIZE - KIND_SIZE - FLAGS_SIZE;

    public final int DIM_SHIFT = KIND_SIZE + FLAGS_SIZE + VALUE_SIZE;
    public final int KIND_SHIFT = FLAGS_SIZE + VALUE_SIZE;
    public final int FLAGS_SHIFT = VALUE_SIZE;

    // Bitmasks to get each field of an abstract type.

    public final int DIM_MASK = ((1 << DIM_SIZE) - 1) << DIM_SHIFT;
    public final int KIND_MASK = ((1 << KIND_SIZE) - 1) << KIND_SHIFT;
    public final int VALUE_MASK = (1 << VALUE_SIZE) - 1;

    // Constants to manipulate the DIM field of an abstract type.

    /** The constant to be added to an abstract type to get one with one more array dimension. */
    public final int ARRAY_OF = +1 << DIM_SHIFT;

    /** The constant to be added to an abstract type to get one with one less array dimension. */
    public final int ELEMENT_OF = -1 << DIM_SHIFT;

    // Possible values for the KIND field of an abstract type.

    public final int CONSTANT_KIND = 1 << KIND_SHIFT;
    public final int REFERENCE_KIND = 2 << KIND_SHIFT;
    public final int UNINITIALIZED_KIND = 3 << KIND_SHIFT;
    public final int LOCAL_KIND = 4 << KIND_SHIFT;
    public final int STACK_KIND = 5 << KIND_SHIFT;

    // Possible flags for the FLAGS field of an abstract type.

    /**
     * A flag used for LOCAL_KIND and STACK_KIND abstract types, indicating that if the resolved,
     * concrete type is LONG or DOUBLE, TOP should be used instead (because the value has been
     * partially overridden with an xSTORE instruction).
     */
    public final int TOP_IF_LONG_OR_DOUBLE_FLAG = 1 << FLAGS_SHIFT;

    // Useful predefined abstract types (all the possible CONSTANT_KIND types).

    public final int TOP = CONSTANT_KIND | ITEM_TOP;
    public final int BOOLEAN = CONSTANT_KIND | ITEM_ASM_BOOLEAN;
    public final int BYTE = CONSTANT_KIND | ITEM_ASM_BYTE;
    public final int CHAR = CONSTANT_KIND | ITEM_ASM_CHAR;
    public final int SHORT = CONSTANT_KIND | ITEM_ASM_SHORT;
    public final int INTEGER = CONSTANT_KIND | ITEM_INTEGER;
    public final int FLOAT = CONSTANT_KIND | ITEM_FLOAT;
    public final int LONG = CONSTANT_KIND | ITEM_LONG;
    public final int DOUBLE = CONSTANT_KIND | ITEM_DOUBLE;
    public final int NULL = CONSTANT_KIND | ITEM_NULL;
    public final int UNINITIALIZED_THIS = CONSTANT_KIND | ITEM_UNINITIALIZED_THIS;
}
