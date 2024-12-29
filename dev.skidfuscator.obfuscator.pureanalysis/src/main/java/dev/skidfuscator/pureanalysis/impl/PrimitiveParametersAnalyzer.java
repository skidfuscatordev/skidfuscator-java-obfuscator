package dev.skidfuscator.pureanalysis.impl;

import dev.skidfuscator.pureanalysis.PurityAnalyzer;
import dev.skidfuscator.pureanalysis.Analyzer;
import dev.skidfuscator.pureanalysis.PurityContext;
import dev.skidfuscator.pureanalysis.PurityReport;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class PrimitiveParametersAnalyzer extends Analyzer {
    private static final Set<String> PRIMITIVE_DESCRIPTORS = new HashSet<>(Arrays.asList(
            "B", // byte
            "C", // char
            "D", // double
            "F", // float
            "I", // int
            "J", // long
            "S", // short
            "Z",  // boolean
            "Ljava/lang/String;"
    ));


    public PrimitiveParametersAnalyzer(PurityContext context, PurityAnalyzer analyzer) {
        super("Primitive Parameters", context, analyzer);
    }

    @Override
    public PurityReport analyze(Context ctx) {
        final MethodNode methodNode = ctx.method();
        Type[] argumentTypes = Type.getArgumentTypes(methodNode.desc);

        for (Type type : argumentTypes) {
            if (!isPrimitiveType(type)) {
                return impure(String.format(
                        "Argument %s is not a primitive type and not a pure class", type.getDescriptor()
                ));
            }
        }

        // Also check return type
        Type returnType = Type.getReturnType(methodNode.desc);
        if (!isPrimitiveType(returnType) && !returnType.getDescriptor().equals("V") && !context.isPure(returnType.getInternalName())) {
            return impure(
                    String.format("Return type %s is not a primitive type and not a pure class", returnType.getDescriptor())
            );
        }

        return pure();
    }

    private boolean isPrimitiveType(Type type) {
        // Handle arrays
        if (type.getSort() == Type.ARRAY) {
            // Do not support arrays just yet
            if (true) {
                return false;
            }
            // Get the element type of the array
            Type elementType = type.getElementType();
            return PRIMITIVE_DESCRIPTORS.contains(elementType.getDescriptor());
        }

        // Handle primitive types
        return PRIMITIVE_DESCRIPTORS.contains(type.getDescriptor());
    }
}