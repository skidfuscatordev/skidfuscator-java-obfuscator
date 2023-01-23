package dev.skidfuscator.obfuscator.transform;

import dev.skidfuscator.config.DefaultTransformerConfig;
import dev.skidfuscator.obfuscator.event.Listener;

import java.util.List;

public interface Transformer extends Listener  {
    /**
     * @return Name of the transformer (will be used for config)
     */
    String getName();

    /**
     * @return Config of a transformer
     */
    <T extends DefaultTransformerConfig> T getConfig();

    /**
     * @return List of all the children transfomers to this transformer (for example flow
     * is dependent on a specific type of transformation)
     */
    List<Transformer> getChildren();

    /**
     * Registers the listener
     */
    void register();
}
