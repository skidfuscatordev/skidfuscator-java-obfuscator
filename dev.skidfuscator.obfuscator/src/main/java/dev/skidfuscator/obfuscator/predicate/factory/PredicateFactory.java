package dev.skidfuscator.obfuscator.predicate.factory;

public interface PredicateFactory<O, P> {
    O build(final P p);
}
