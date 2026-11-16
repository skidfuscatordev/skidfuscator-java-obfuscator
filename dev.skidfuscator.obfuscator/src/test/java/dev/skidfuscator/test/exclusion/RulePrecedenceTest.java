package dev.skidfuscator.test.exclusion;
import dev.skidfuscator.obfuscator.exempt.*;
import dev.skidfuscator.obfuscator.exempt.v2.ExclusionParser;
import org.junit.jupiter.api.Test;
import org.mapleir.asm.*;
import org.objectweb.asm.Opcodes;
import static org.junit.jupiter.api.Assertions.*;
class RulePrecedenceTest {
    ClassNode owner(String name) { var raw=new org.objectweb.asm.tree.ClassNode();raw.name=name;raw.access=Opcodes.ACC_PUBLIC;raw.superName="java/lang/Object";ClassNode owner=new ClassNode(); owner.node=raw; return owner; }
    MethodNode method(ClassNode owner,String name,String desc) { return new MethodNode(new org.objectweb.asm.tree.MethodNode(Opcodes.ACC_PUBLIC,name,desc,null,null),owner); }
    @Test void memberIncludesOverrideWithinScopeAndDoNotExcludeWholeClass() {
        ClassNode model=owner("sample/Model");
        Exclusion rule=ExclusionParser.parsePatternExclusion("""
            @class sample.Model {
                @method work*
                !@method workSafe()
                @field *
                !@field serialVersionUID
            }
            """);
        assertFalse(rule.test(model));
        assertTrue(rule.test(method(model,"work","()V")));
        assertFalse(rule.test(method(model,"workSafe","()V")));
        assertTrue(rule.test(method(model,"workSafe","(I)V")));
        assertFalse(rule.test(method(owner("other/Model"),"work","()V")));
        assertFalse(rule.test(new FieldNode(new org.objectweb.asm.tree.FieldNode(Opcodes.ACC_PRIVATE,"serialVersionUID","J",null,null),model)));
        assertTrue(rule.test(new FieldNode(new org.objectweb.asm.tree.FieldNode(Opcodes.ACC_PUBLIC,"value","I",null,null),model)));
    }
    @Test void classRulesAreAnchoredAndApplyToAllMembers() {
        Exclusion rule=ExclusionParser.parsePatternExclusion("@class sample.Model {}");
        assertTrue(rule.test(owner("sample/Model")));
        assertFalse(rule.test(owner("sample/ModelOther")));
        assertTrue(rule.test(method(owner("sample/Model"),"any","()V")));
    }
    @Test void addingRulesInvalidatesDecisionsWithoutLosingExplicitPreservation() {
        var analysis=new SimpleExemptAnalysis(null);
        ClassNode one=owner("sample/Model"),two=owner("other/Model");
        assertFalse(analysis.isExempt(one));analysis.add(two);
        analysis.add(ExclusionParser.parsePatternExclusion("@class sample.Model {}"));
        assertTrue(analysis.isExempt(one));assertTrue(analysis.isExempt(two));
    }
    @Test void malformedRulesFailRatherThanSilentlyWidenScope() {
        assertThrows(ExclusionParser.ExclusionParseException.class,()->ExclusionParser.parsePatternExclusion("@class sample.Model { @method work"));
    }
}
