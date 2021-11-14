package dev.skidfuscator.obf.skidasm;

import dev.skidfuscator.obf.yggdrasil.caller.CallerType;
import org.mapleir.asm.MethodNode;

import java.util.HashSet;
import java.util.Set;

public class NoNoSkidMethod extends SkidMethod{
    public NoNoSkidMethod() {
        super(new HashSet<>(), null, null);
    }
}
