package dev.skidfuscator.obfuscator.order;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.skidasm.SkidGroup;
import dev.skidfuscator.obfuscator.skidasm.SkidMethodNode;
import org.mapleir.asm.MethodNode;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SimpleOrderAnalysis implements OrderAnalysis {
    private final Skidfuscator skidfuscator;


    public SimpleOrderAnalysis(Skidfuscator skidfuscator) {
        this.skidfuscator = skidfuscator;
    }



    @Override
    public MethodType getType(SkidMethodNode methodNode) {
        return null;
    }

}
