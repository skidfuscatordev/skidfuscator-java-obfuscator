package dev.skidfuscator.obf.transform_legacy.mutate;

import org.objectweb.asm.Type;

/**
 * @author Cg.
 */
public interface Node {

    Type getType();

    void build(ExpressionContext context);
}
