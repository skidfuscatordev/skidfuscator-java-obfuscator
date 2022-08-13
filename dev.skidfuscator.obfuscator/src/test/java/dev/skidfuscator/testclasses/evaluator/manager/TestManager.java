package dev.skidfuscator.testclasses.evaluator.manager;

import dev.skidfuscator.testclasses.evaluator.EvaluatorMain;
import dev.skidfuscator.testclasses.evaluator.test.TestHandler;
import dev.skidfuscator.testclasses.evaluator.test.impl.annotation.AnnotationTest;
import dev.skidfuscator.testclasses.evaluator.test.impl.flow.OpaqueConditionTest;
import dev.skidfuscator.testclasses.evaluator.test.impl.flow.WeirdLoopTest;
import dev.skidfuscator.testclasses.evaluator.test.impl.string.StringTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestManager {
    private static final List<Class<? extends TestHandler>> TEST_CLASSES = Arrays.asList(
            AnnotationTest.class,
            OpaqueConditionTest.class,
            WeirdLoopTest.class,
            StringTest.class
    );

    private final List<TestHandler> tests = new ArrayList<>();

    public TestManager() {
        TEST_CLASSES.forEach(clazz -> {
            try {
                tests.add(clazz.newInstance());
            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }
        });

        EvaluatorMain.LOG.println(String.format("Loaded %d tests", tests.size()));
    }

    public void handleTests() {
        tests.forEach(TestHandler::handle);
    }
}
