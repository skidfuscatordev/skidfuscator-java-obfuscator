package dev.skidfuscator.obfuscator.util.misc;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class Pair<A, B> {
    A a;
    B b;
}
