package dev.skidfuscator.testclasses.evaluator.operation;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DoubleMathOperation {
    ADD("+") {
        @Override
        public double evaluate(double first, double second) {
            return first + second;
        }
    },
    SUB("-") {
        @Override
        public double evaluate(double first, double second) {
            return first - second;
        }
    },
    DIV("/") {
        @Override
        public double evaluate(double first, double second) {
            return first / second;
        }
    },
    REM("%") {
        @Override
        public double evaluate(double first, double second) {
            return first % second;
        }
    },
    MUL("*") {
        @Override
        public double evaluate(double first, double second) {
            return first * second;
        }
    };

    private final String desc;

    public abstract double evaluate(double first, double second);
}

