package dev.skidfuscator.testclasses.evaluator.test.impl.annotation;

import dev.skidfuscator.testclasses.evaluator.EvaluatorMain;
import dev.skidfuscator.testclasses.evaluator.test.TestHandler;

@TestAnnotation(string = "Test", doubleValue = 0.36, intValue = 36)
public class AnnotationTest implements TestHandler {

    @Override
    public void handle() {
        Class<? extends AnnotationTest> clazz = AnnotationTest.class;

        if (clazz.isAnnotationPresent(TestAnnotation.class)) {
            TestAnnotation annotation = clazz.getAnnotation(TestAnnotation.class);

            String value = annotation.string();
            double doubleValue = annotation.doubleValue();
            int intValue = annotation.intValue();

            EvaluatorMain.LOG.println("Testing annotations");
            EvaluatorMain.LOG.println(String.format("%s, %s, %d", value, doubleValue, intValue));
        }
    }
}
