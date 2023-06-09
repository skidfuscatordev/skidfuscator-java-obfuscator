package dev.skidfuscator.obfuscator.attribute;

public interface Attributable {
    <T> Attribute<T> getAttribute(final AttributeKey attributeKey);
    <T> void addAttribute(final AttributeKey key, final Attribute<T> attribute);
    <T> void removeAttribute(final AttributeKey attributeKey);
}
