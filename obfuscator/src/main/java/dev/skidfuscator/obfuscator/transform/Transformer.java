package dev.skidfuscator.obfuscator.transform;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.event.Listener;

import java.util.List;

public interface Transformer extends Listener  {
    /**
     * @return Name of the transformer (will be used for config)
     */
    String getName();

    /**
     * @return List of all the children transfomers to this transformer (for example flow
     * is dependent on a specific type of transformation)
     */
    List<Transformer> getChildren();

}
