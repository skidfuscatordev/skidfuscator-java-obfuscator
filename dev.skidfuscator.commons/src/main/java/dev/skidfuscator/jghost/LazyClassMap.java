package dev.skidfuscator.jghost;

import dev.skidfuscator.jghost.tree.GhostClassNode;
import org.mapleir.asm.ClassHelper;
import org.mapleir.asm.ClassNode;
import java.util.*;

/** Materialize only requested classes; release their serialized metadata immediately. */
final class LazyClassMap extends AbstractMap<String, ClassNode> {
    private final Map<String, GhostClassNode> pending = new HashMap<>();
    private final Map<String, ClassNode> loaded = new HashMap<>();
    private final Set<String> names = new TreeSet<>();
    LazyClassMap(Collection<GhostClassNode> classes) {
        for (GhostClassNode node : classes) {
            if (!names.add(node.getName())) throw new IllegalArgumentException("Duplicate metadata class: " + node.getName());
            pending.put(node.getName(), node);
        }
    }
    @Override public int size() { return names.size(); }
    @Override public boolean containsKey(Object name) { return names.contains(name); }
    @Override public Set<String> keySet() { return Collections.unmodifiableSet(names); }
    @Override public ClassNode get(Object name) {
        ClassNode value = loaded.get(name);
        if (value == null) {
            GhostClassNode metadata = pending.get(name);
            if (metadata != null) {
                value = ClassHelper.create(metadata.read());
                loaded.put((String) name, value); pending.remove(name);
            }
        }
        return value;
    }
    @Override public ClassNode put(String name, ClassNode value) {
        ClassNode previous = get(name); pending.remove(name); names.add(name); loaded.put(name,value); return previous;
    }
    @Override public void clear() { pending.clear(); loaded.clear(); names.clear(); }
    @Override public Set<Entry<String, ClassNode>> entrySet() {
        return new AbstractSet<Entry<String, ClassNode>>() {
            public int size() { return names.size(); }
            public Iterator<Entry<String, ClassNode>> iterator() {
                Iterator<String> keys = names.iterator();
                return new Iterator<Entry<String, ClassNode>>() {
                    public boolean hasNext() { return keys.hasNext(); }
                    public Entry<String, ClassNode> next() { String name = keys.next(); return new SimpleImmutableEntry<>(name,get(name)); }
                };
            }
        };
    }
}
