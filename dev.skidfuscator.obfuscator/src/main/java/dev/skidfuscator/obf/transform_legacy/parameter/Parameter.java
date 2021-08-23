package dev.skidfuscator.obf.transform_legacy.parameter;

import lombok.Setter;
import org.objectweb.asm.Type;

import java.util.*;

/**
 * @author Ghast
 * @since 08/03/2021
 * SkidfuscatorV2 © 2021
 */
@Setter
public class Parameter {
    private LinkedList<Type> args;
    private Type returnArg;

    public Parameter(String desc) {
        this.returnArg = Type.getReturnType(desc);
        this.args = new LinkedList<>(Arrays.asList(Type.getArgumentTypes(desc)));
    }

    public void addParameter(Type type) {
        this.args.add(type);
    }

    public void insertParameter(Type type, int index){
        this.args.add(index, type);
    }

    public String getDesc() {
        final StringBuilder builder = new StringBuilder();
        builder.append("(");
        for (Type arg : args) {
            builder.append(arg.getDescriptor());
        }
        builder.append(")");
        builder.append(returnArg.getDescriptor());
        return builder.toString();
    }
}
