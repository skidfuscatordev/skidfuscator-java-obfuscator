package org.mapleir.ir.printer;

import java.util.Iterator;

import org.mapleir.propertyframework.api.IPropertyDictionary;
import org.mapleir.stdlib.util.TabbedStringWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import static org.mapleir.ir.printer.Util.isNonEmpty;

@SuppressWarnings("unused")
public class ClassPrinter extends ASMPrinter<ClassNode> {

    private static final String[] CLASS_ATTR_FLAG_NAMES = Util.asOpcodesAccessFieldFormat(
            new String[] { "public", "private", "protected", "final", "super", "interface",
                    "abstract", "synthetic", "annotation", "enum", "deprecated" });

    private final Printer<FieldNode> fieldPrinter;
    private final Printer<MethodNode> methodPrinter;

    public ClassPrinter(TabbedStringWriter sw, IPropertyDictionary settingsDict,
            Printer<FieldNode> fieldPrinter, Printer<MethodNode> methodPrinter) {
        super(sw, settingsDict, CLASS_ATTR_FLAG_NAMES);

        this.fieldPrinter = fieldPrinter;
        this.methodPrinter = methodPrinter;
    }

    @Override
    public void print(ClassNode e) {
        this.sw.print(".class ").print(e.name).print(" {").tab();

        this.emitDirective("superName", e.superName);
        this.emitDirective("interfaces", e.interfaces);
        this.emitDirective("version", e.version);
        this.emitAccessDirective(e.access);
        
        this.emitIfNonNull("sourceFile", e.sourceFile);
        this.emitIfNonNull("sourceFile", e.sourceDebug);
        this.emitIfNonNull("outerClass", e.outerClass);
        this.emitIfNonNull("outerMethod", e.outerMethod);
        this.emitIfNonNull("outerMethodDesc", e.outerMethodDesc);
        
        if(isNonEmpty(e.visibleAnnotations)) {
            this.emitDirective("visibleAnnotations", e.visibleAnnotations);
        }
        if(isNonEmpty(e.invisibleAnnotations)) {
            this.emitDirective("invisibleAnnotations", e.invisibleAnnotations);
        }
        if(isNonEmpty(e.visibleTypeAnnotations)) {
            this.emitDirective("visibleTypeAnnotations", e.visibleTypeAnnotations);
        }
        if(isNonEmpty(e.invisibleTypeAnnotations)) {
            this.emitDirective("invisibleTypeAnnotations", e.invisibleTypeAnnotations);
        }
        if(isNonEmpty(e.attrs)) {
            this.emitNodeAttributes(e.attrs);
        }

        this.emit(e.fields.iterator(), this.fieldPrinter);
        this.emit(e.methods.iterator(), this.methodPrinter);

        this.sw.untab().newline().print("}");
    }
    
    private void emitIfNonNull(String name, Object val) {
        if(val != null) {
            this.emitDirective(name, val);
        }
    }

    private <E> void emit(Iterator<E> it, Printer<E> printer) {
        if (it.hasNext()) {
            this.sw.newline();
        }

        while (it.hasNext()) {
            E e = it.next();
            printer.print(e);
        }
    }
}
