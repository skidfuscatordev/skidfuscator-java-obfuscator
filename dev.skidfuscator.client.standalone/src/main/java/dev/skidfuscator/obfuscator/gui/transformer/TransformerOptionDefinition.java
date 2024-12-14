package dev.skidfuscator.obfuscator.gui.transformer;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class TransformerOptionDefinition {
    private String key;
    private String label;
    private TransformerOptionType type;
    private Object defaultValue;
    private List<String> enumValues;  // Used for ENUM type
    private String description;
}