package dev.skidfuscator.obfuscator.skidasm;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.predicate.opaque.MethodOpaquePredicate;
import lombok.Data;
import org.mapleir.asm.MethodNode;
import org.mapleir.ir.code.expr.invoke.InvocationExpr;

import java.util.ArrayList;
import java.util.List;

@Data
public class SkidGroup {
    private final List<MethodNode> methodNodeList;
    private final Skidfuscator skidfuscator;
    private final MethodOpaquePredicate predicate;
    private final List<SkidInvocation> invokers;

    private boolean annotation;
    private boolean statical;
    private String name;
    private String desc;

    // TODO: Add parameter and parameter compilation

    public SkidGroup(List<MethodNode> methodNodeList, Skidfuscator skidfuscator) {
        this.methodNodeList = methodNodeList;
        this.skidfuscator = skidfuscator;
        this.predicate = skidfuscator
                .getPredicateAnalysis()
                .getMethodPredicate(this);
        this.invokers = new ArrayList<>();
    }

    public void setStatical(boolean statical) {
        assert !statical || methodNodeList.size() == 1 : "Method group has multiple methods despite being static";

        this.statical = statical;
    }

    public MethodNode first() {
        return methodNodeList.iterator().next();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SkidGroup skidGroup = (SkidGroup) o;

        return methodNodeList.equals(skidGroup.methodNodeList);
    }

    @Override
    public int hashCode() {
        return methodNodeList.hashCode();
    }
}
