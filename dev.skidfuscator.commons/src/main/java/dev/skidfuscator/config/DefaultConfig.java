package dev.skidfuscator.config;

import com.typesafe.config.Config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        return getStringList(path + "exempt", new ArrayList<>());
    }

    public boolean getBoolean(String path, final boolean dflt) {
        return get(path, dflt, config::getBoolean);
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

        assert dflt.getClass().isAssignableFrom(var.getClass()) : "Value loaded at path is not a type assigned!";

        return (T) var;
    }
}
