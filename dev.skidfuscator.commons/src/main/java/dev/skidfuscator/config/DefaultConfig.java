package dev.skidfuscator.config;

import com.typesafe.config.Config;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

public class DefaultConfig {
    protected final String path;
    protected final Config config;
    protected final Map<String, Object> cache;

    public DefaultConfig(Config config, String path) {
        this.path = path.equals("") ? path : path + ".";
        this.config = config;
        this.cache = new HashMap<>();
    }

    public List<String> getExemptions() {
        return getStringList("exempt", Arrays.asList(DefaultExempts.DEFAULT_EXEMPTS));
    }

    public boolean getBoolean(String path, final boolean dflt) {
        return get(path, dflt, config::getBoolean);
    }

    public String getString(String path, final String dflt) {
        return get(path, dflt, config::getString);
    }

    public int getInt(String path, final int dflt) {
        return get(path, dflt, config::getInt);
    }

    public List<String> getStringList(String path, final List<String> dflt) {
        return get(path, dflt, config::getStringList);
    }

    public <T extends Enum<T>> T getEnum(String path, final Enum<T> dflt) {
        return (T) get(path, dflt, e -> config.getEnum(dflt.getDeclaringClass(), e));
    }

    public <T> T get(String path, final T dflt, final Function<String, T> supplier) {
        path = this.path + path;
        Object var = cache.get(path);

        if (var == null) {
            if (config.hasPath(path)) {
                var = supplier.apply(path);
            } else {
                var = dflt;
            }

            cache.put(path, var);
        }

        //assert var.getClass().isAssignableFrom(dflt.getClass()) : "Value loaded at path is not a type assigned! (" + var.getClass() + " vs " + dflt.getClass() + ")";

        return (T) var;
    }
}
