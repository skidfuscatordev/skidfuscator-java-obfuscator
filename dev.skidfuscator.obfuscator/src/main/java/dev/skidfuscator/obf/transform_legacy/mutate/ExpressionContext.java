package dev.skidfuscator.obf.transform_legacy.mutate;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.objectweb.asm.Type;

/**
 * @author Cg.
 */
public class ExpressionContext {
    private Map<Type, Set<Operand<?>>> operandPool;

    public Map<Type, Set<Operand<?>>> getOperandPool() {
        return operandPool;
    }

    public Set<Operand<?>> getAllOperands() {
        return operandPool.values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
    }

    public void addOperandCandidate(Type type, Operand<?> operand) {
        operandPool.computeIfAbsent(type, k -> new HashSet<>()).add(operand);
    }
}
