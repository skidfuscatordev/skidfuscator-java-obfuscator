package dev.skidfuscator.obfuscator.reliability;

import com.typesafe.config.*;
import dev.skidfuscator.obfuscator.ReliabilityOptions.Profile;
import java.util.*;

public final class Profiles {
    private Profiles() {}
    public static Config apply(Config explicit, Profile profile) {
        String defaults = "ahegao.enabled=false\nfileCrasher.enabled=false\n";
        if (profile == Profile.CONSERVATIVE) defaults += "flowException.enabled=false\nflowRange.enabled=false\nflowSwitch.enabled=false\npureEncryption.enabled=false\ninterprocedural.enabled=false\n";
        else if (profile == Profile.BALANCED) defaults += "flowException.strength=GOOD\n";
        Config result = explicit.withFallback(ConfigFactory.parseString(defaults)).resolve();
        Set<String> roots = new LinkedHashSet<>(Arrays.asList("exempt","exclude","libs","libraries","sdk",
                "stringEncryption","numberEncryption","intAnnotationEncryption","stringAnnotationEncryption",
                "exceptionReturn","flowCondition","flowException","flowRange","flowFactoryMaker","flowSwitch",
                "outliner","ahegao","native","driver","reference","fileCrasher","classRenamer","methodRenamer",
                "fieldRenamer","interprocedural","randomInit","pureEncryption","pureHash","stringEqualsHash",
                "stringEqualsIgnoreCaseHash","stringEqigcaseHash","instanceOfHash","interproceduralHarden"));
        for (String root : explicit.root().keySet()) if (!roots.contains(root))
            throw new IllegalArgumentException("Unknown configuration section: " + root);
        for (Map.Entry<String,ConfigValue> entry : result.entrySet())
            if (entry.getKey().endsWith(".enabled") && entry.getValue().valueType()!=ConfigValueType.BOOLEAN)
                throw new IllegalArgumentException("Expected boolean at " + entry.getKey());
        return result;
    }
}
