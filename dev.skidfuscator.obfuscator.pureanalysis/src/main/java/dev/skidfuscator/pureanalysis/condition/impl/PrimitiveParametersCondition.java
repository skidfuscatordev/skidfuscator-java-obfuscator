package dev.skidfuscator.pureanalysis.condition.impl;

import dev.skidfuscator.pureanalysis.PurityAnalyzer;
import dev.skidfuscator.pureanalysis.condition.PurityCondition;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class PrimitiveParametersCondition extends PurityCondition {
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

    public PrimitiveParametersCondition() {
        super("Primitive parameters");
    }

    @Override
    public boolean evaluate(MethodNode method, ClassNode classNode, PurityAnalyzer analyzer) {
        Type[] argumentTypes = Type.getArgumentTypes(method.desc);

        for (Type type : argumentTypes) {
            if (!isPrimitiveType(type)) {
                return false;
            }
        }

        // Also check return type
        Type returnType = Type.getReturnType(method.desc);
        if (!isPrimitiveType(returnType) && !returnType.getDescriptor().equals("V") && !analyzer.isPureClass(returnType.getInternalName())) {
            System.out.println(String.format("Return type %s is not a primitive type and not a pure class", returnType.getDescriptor()));
            return false;
        }

        return evaluateNested(method, classNode, analyzer);
    }

    private boolean isPrimitiveType(Type type) {
        // Handle arrays
        if (type.getSort() == Type.ARRAY) {
            // Get the element type of the array
            Type elementType = type.getElementType();
            return PRIMITIVE_DESCRIPTORS.contains(elementType.getDescriptor());
        }

        // Handle primitive types
        return PRIMITIVE_DESCRIPTORS.contains(type.getDescriptor());
    }
}