package dev.skidfuscator.discord.utils;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Ghast
 * @since 06-Mar-20
 */

@Getter
public class EvictingArrayList<T> extends ArrayList<T> {

    private final int max;

    public EvictingArrayList(int max) {
        super();
        this.max = max;
    }


    @Override
    public boolean add(T t) {
        if (size() >= max) {
            remove(0);
        }
        return super.add(t);
    }

    @Override
    public void add(int index, T element) {
        if (size() >= max) {
            super.remove(0);
        }
        super.add(index, element);
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        if ((size() + c.size()) > max) {
            for (int i = 0; i < c.size(); i++) {
                super.remove((int) i);
            }
        }
        return super.addAll(c);
    }

    @Override
    public boolean addAll(int index, Collection<? extends T> c) {
        throw new IllegalStateException("EvictingArrayList#addAll(int, Collection<? extends T>");
    }
}
