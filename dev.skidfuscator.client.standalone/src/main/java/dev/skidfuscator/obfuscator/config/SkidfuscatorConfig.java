package dev.skidfuscator.obfuscator.config;

import com.typesafe.config.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Configuration generator for Skidfuscator transformers.
 * Handles HOCON configuration generation using Lightbend Config library.
 */
public class SkidfuscatorConfig {
    private final Map<String, ConfigValue> configMap = new HashMap<>();
    
    /**
     * Adds a transformer configuration block.
     * @param name Transformer name
     * @param enabled Enabled state
     * @param options Additional transformer options
     * @param exemptions List of exemption patterns
     */
    public void addTransformer(String name, boolean enabled, Map<String, Object> options, List<String> exemptions) {
        Map<String, Object> transformerConfig = new HashMap<>();
        transformerConfig.put("enabled", enabled);
        
        if (options != null && !options.isEmpty()) {
            transformerConfig.putAll(options);
        }
        
        if (exemptions != null && !exemptions.isEmpty()) {
            transformerConfig.put("exempt", exemptions);
        }
        
        configMap.put(name, ConfigValueFactory.fromMap(transformerConfig));
    }
    
    /**
     * Sets global exemptions for the obfuscator.
     * @param exemptions List of global exemption patterns
     */
    public void setGlobalExemptions(List<String> exemptions) {
        if (exemptions != null && !exemptions.isEmpty()) {
            configMap.put("exempt", ConfigValueFactory.fromIterable(exemptions));
        }
    }
    
    /**
     * Sets library dependencies for the obfuscator.
     * @param libraries List of library paths
     */
    public void setLibraries(List<String> libraries) {
        if (libraries != null && !libraries.isEmpty()) {
            configMap.put("libraries", ConfigValueFactory.fromIterable(libraries));
        }
    }
    
    /**
     * Generates the final Config object.
     * @return Config object containing all settings
     */
    public Config generateConfig() {
        return ConfigFactory.parseMap(configMap);
    }
    
    /**
     * Renders the configuration as a HOCON string.
     * @return Formatted HOCON configuration string
     */
    public String renderConfig() {
        return generateConfig().root().render(
            ConfigRenderOptions.defaults()
                .setOriginComments(false)
                .setComments(true)
                .setFormatted(true)
                .setJson(false)
        );
    }
}