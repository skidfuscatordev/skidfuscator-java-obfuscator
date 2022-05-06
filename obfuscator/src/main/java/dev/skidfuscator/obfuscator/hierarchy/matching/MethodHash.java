package dev.skidfuscator.obfuscator.hierarchy.matching;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author Ghast
 * @since 08/03/2021
 * SkidfuscatorV2 © 2021
 */

@Data
@EqualsAndHashCode
public class MethodHash {
    private final String name;
    private final String desc;
}