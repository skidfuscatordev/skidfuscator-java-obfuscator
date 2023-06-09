package dev.skidfuscator.obfuscator.attribute;

import java.util.HashMap;
import java.util.Map;

public class AttributeMap extends HashMap<AttributeKey, Attribute<Object>> {
    public AttributeMap(final Map<? extends AttributeKey, ? extends Attribute<Object>> m) {
        super(m);
    }

    public AttributeMap() {
    }

    public <T> T poll(final Object key) {
        final Object o = super.get(key);
        return o == null ? null : (T) ((StandardAttribute) o).getBase();
    }

    public AttributeMap copy() {
        final AttributeMap map = new AttributeMap();

        this.forEach((key, value) -> map.put(key, new StandardAttribute<>(value.getBase())));

        return map;
    }
}
