package dev.skidfuscator.testclasses.evaluator.util.stats;

import dev.skidfuscator.testclasses.evaluator.util.Log;
import lombok.Getter;

import java.util.LinkedList;
import java.util.List;
import java.util.OptionalDouble;

@Getter
public class Calculations {
    private final List<Integer> intCalculations = new LinkedList<>();
    private final List<Double> doubleCalculations = new LinkedList<>();

    public void run(Log log) {
        log.println("\nComputing statistics");
        OptionalDouble intAverage = intCalculations.stream()
                .mapToInt(i -> i)
                .average();

        if (!intAverage.isPresent())
            throw new AssertionError();

        OptionalDouble doubleAverage = doubleCalculations.stream()
                .filter(d -> d != 0)
                .mapToDouble(d -> d)
                .average();

        if (!doubleAverage.isPresent())
            throw new AssertionError();

        log.print("Averages for (i, d): %s, %s", intAverage.getAsDouble(), doubleAverage.getAsDouble());
    }

    public int store(int value) {
        intCalculations.add(value);

        return value;
    }

    public double store(double value) {
        doubleCalculations.add(value);

        return value;
    }
}
