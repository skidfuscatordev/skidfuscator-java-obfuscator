package framework.testclasses;

import framework.Impure;
import framework.Pure;

public class WeirdPureObject {
    public static int xx;

    public int x;
    public int y;

    @Pure(
            description = ""
    )
    public static WeirdPureObject create() {
        return new WeirdPureObject();
    }

    @Pure
    public static WeirdPureObject create(int x, int y) {
        final WeirdPureObject obj = new WeirdPureObject();
        obj.x = x;
        obj.y = y;
        return obj;
    }

    @Pure
    public void set(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Pure
    public void setX(int x) {
        this.x = x;
    }

    @Pure
    public void setY(int y) {
        this.y = y;
    }

    @Impure
    public void setXx() {
        xx = x;
    }

    @Pure
    public String toString() {
        return "heeeheee";
    }
}
