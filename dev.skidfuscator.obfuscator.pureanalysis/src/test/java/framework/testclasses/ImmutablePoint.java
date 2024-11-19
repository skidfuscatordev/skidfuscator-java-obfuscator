package framework.testclasses;

import framework.Pure;

public class ImmutablePoint {
    private final int x;
    private final int y;

    @Pure(description = "Pure constructor",
         because = {"Only initializes final fields", "No side effects"})
    public static ImmutablePoint create(int x, int y) {
        return new ImmutablePoint(x, y);
    }

    private ImmutablePoint(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Pure(description = "Pure calculation method", 
         because = {"Uses only local state", "Returns new object"})
    public static ImmutablePoint add(ImmutablePoint p1, ImmutablePoint p2) {
        return new ImmutablePoint(p1.x + p2.x, p1.y + p2.y);
    }

    @Pure(description = "Pure distance calculation",
         because = {"Uses Math functions", "No state modification"})
    public double distanceTo(ImmutablePoint other) {
        double dx = x - other.x;
        double dy = y - other.y;
        return Math.sqrt(dx * dx + dy * dy);
    }
}