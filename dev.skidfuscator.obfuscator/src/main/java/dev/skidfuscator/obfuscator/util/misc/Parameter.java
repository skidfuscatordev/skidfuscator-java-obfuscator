package dev.skidfuscator.obfuscator.util.misc;

import lombok.Setter;
import org.objectweb.asm.Type;

import java.util.Arrays;
import java.util.LinkedList;

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

    public LinkedList<Type> getArgs() {
        return args;
    }

    public Type getArg(final int index) {
        return args.get(index);
    }

    public Type getReturnType() {
        return returnArg;
    }

    public int computeSize() {
        int size = 0;

        for (Type arg : args) {
            size++;

            if (arg.equals(Type.DOUBLE_TYPE) || arg.equals(Type.LONG_TYPE))
                size++;
        }

        return size;
    }

    public int computeSize(final int untilIndex) {
        int size = 0;

        for (int i = 0; i < untilIndex; i++) {
            size++;

            final Type type = args.get(i);
            if (type.equals(Type.DOUBLE_TYPE) || type.equals(Type.LONG_TYPE))
                size++;
        }

        return size;
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

    public Parameter clone() {
        return new Parameter(this.getDesc());
    }
}
