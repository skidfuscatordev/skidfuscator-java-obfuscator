package dev.skidfuscator.pureanalysis;

import dev.skidfuscator.pureanalysis.impl.*;
import org.objectweb.asm.tree.*;

import java.util.HashSet;
import java.util.Set;

public class PurityAnalyzer {
    private final Set<Analyzer> analyzers = new HashSet<>();
    private final PurityContext context;
    private final ClassHierarchyAnalyzer hierarchyAnalyzer;

    public PurityAnalyzer(ClassHierarchyAnalyzer hierarchyAnalyzer) {
        this.hierarchyAnalyzer = hierarchyAnalyzer;
        this.context = new PurityContext(this);
        initializeAnalyzers();
    }

    public PurityContext getContext() {
        return context;
    }

    private void initializeAnalyzers() {
        analyzers.add(new TypeInstructionAnalyzer(context, this));
        analyzers.add(new MethodInstructionAnalyzer(context, this));
        analyzers.add(new FieldInstructionAnalyzer(context, this));
        analyzers.add(new DynamicInstructionAnalyzer(context, this));
        analyzers.add(new NativeMethodAnalyzer(context, this));
        analyzers.add(new PrimitiveParametersAnalyzer(context, this));
    }

    public PurityReport analyzeMethodPurity(MethodNode method, ClassNode classNode) {
        final PurityReport report = new PurityReport(true, "Method Analysis", null, null);
        final Analyzer.Context methodCtx = new Analyzer.Context(
                method,
                classNode
        );

        for (Analyzer analyzer : analyzers) {
            report.addNested(analyzer.analyze(methodCtx));
        }
        
        return report;
    }

    public ClassHierarchyAnalyzer getHierarchyAnalyzer() {
        return hierarchyAnalyzer;
    }
}