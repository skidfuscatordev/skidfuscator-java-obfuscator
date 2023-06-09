package dev.skidfuscator.obfuscator.attribute;

import lombok.Data;

import java.util.UUID;

/**
 * @author Ghast
 * @since 12/02/2021
 * Artemis © 2021
 */

@Data
public class AttributeKey {
    private final String name;
    private final UUID uuid;

    public AttributeKey(final String name) {
        this(name, UUID.randomUUID());
    }

    public AttributeKey(final String name, final UUID uuid) {
        this.name = name;
        this.uuid = uuid;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final AttributeKey that = (AttributeKey) o;

        if (!name.equals(that.name)) return false;
        return uuid.equals(that.uuid);
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + uuid.hashCode();
        return result;
    }
}
