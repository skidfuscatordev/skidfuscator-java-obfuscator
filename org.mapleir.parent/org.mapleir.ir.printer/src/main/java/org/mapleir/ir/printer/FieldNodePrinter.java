package org.mapleir.ir.printer;

import static org.mapleir.ir.printer.Util.isNonEmpty;

import org.mapleir.propertyframework.api.IPropertyDictionary;
import org.mapleir.stdlib.util.TabbedStringWriter;
import org.objectweb.asm.tree.FieldNode;

@SuppressWarnings("unused")
public class FieldNodePrinter extends ASMPrinter<FieldNode> {

    private static final String[] FIELD_ATTR_FLAG_NAMES = Util
            .asOpcodesAccessFieldFormat(new String[] { "public", "private", "protected", "static",
                    "final", "volatile", "transient", "synthetic", "enum", "deprecated" });

    public FieldNodePrinter(TabbedStringWriter sw, IPropertyDictionary settingsDict) {
        super(sw, settingsDict, FIELD_ATTR_FLAG_NAMES);
    }

    @Override
    public void print(FieldNode fn) {
        this.sw.newline().print(".field ").print(fn.desc).print(" ").print(fn.name);

        if (fn.value != null) {
            this.sw.print(" = ");
            this.emitLiteral(fn.value);
        }

        if (this.hasFieldAttributes(fn)) {
            this.sw.print(" {").tab();
            this.emitFieldAttributes(fn);
            this.sw.untab().newline().print("}");
        }
    }

    private boolean hasFieldAttributes(FieldNode fn) {
        return fn.access != 0 || fn.signature != null || isNonEmpty(fn.visibleAnnotations)
                || isNonEmpty(fn.invisibleAnnotations) || isNonEmpty(fn.visibleTypeAnnotations)
                || isNonEmpty(fn.invisibleTypeAnnotations) || isNonEmpty(fn.attrs);
    }

    private void emitFieldAttributes(FieldNode fn) {
        if (fn.access != 0) {
            this.emitAccessDirective(fn.access);
        }
        if (fn.signature != null) {
            this.emitDirective("signature", fn.signature);
        }
        if (isNonEmpty(fn.visibleAnnotations)) {
            this.emitDirective("visibleAnnotations", fn.visibleAnnotations);
        }
        if (isNonEmpty(fn.invisibleAnnotations)) {
            this.emitDirective("invisibleAnnotations", fn.invisibleAnnotations);
        }
        if (isNonEmpty(fn.visibleTypeAnnotations)) {
            this.emitDirective("visibleTypeAnnotations", fn.visibleTypeAnnotations);
        }
        if (isNonEmpty(fn.invisibleTypeAnnotations)) {
            this.emitDirective("invisibleTypeAnnotations", fn.invisibleTypeAnnotations);
        }
        if (isNonEmpty(fn.attrs)) {
            this.emitNodeAttributes(fn.attrs);
        }
    }
}
