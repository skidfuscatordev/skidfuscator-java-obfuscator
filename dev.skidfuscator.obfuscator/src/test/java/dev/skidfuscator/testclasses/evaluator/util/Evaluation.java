package dev.skidfuscator.testclasses.evaluator.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.function.Consumer;

@Getter
@RequiredArgsConstructor
public class Evaluation<T extends Number> {
    private final T first, second;

    private final Consumer<T> evaluator;

}
