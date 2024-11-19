package framework.testclasses;

import framework.Pure;

public class ImmutableTree<T extends Comparable<T>> {
    private final T value;
    private final ImmutableTree<T> left;
    private final ImmutableTree<T> right;

    @Pure(description = "Creates leaf node",
         because = {"Only initializes final fields", "Immutable structure"})
    public static <T extends Comparable<T>> ImmutableTree<T> leaf(T value) {
        return new ImmutableTree<>(value, null, null);
    }

    private ImmutableTree(T value, ImmutableTree<T> left, ImmutableTree<T> right) {
        this.value = value;
        this.left = left;
        this.right = right;
    }

    @Pure(description = "Inserts value maintaining immutability",
         because = {"Creates new tree", "No modification of original"})
    public ImmutableTree<T> insert(T newValue) {
        if (newValue.compareTo(value) < 0) {
            return new ImmutableTree<>(value,
                left == null ? leaf(newValue) : left.insert(newValue),
                right);
        } else {
            return new ImmutableTree<>(value,
                left,
                right == null ? leaf(newValue) : right.insert(newValue));
        }
    }

    @Pure(description = "Pure tree traversal",
         because = {"Only reads state", "Returns primitive"})
    public int countNodes() {
        return 1 + 
            (left == null ? 0 : left.countNodes()) +
            (right == null ? 0 : right.countNodes());
    }
}