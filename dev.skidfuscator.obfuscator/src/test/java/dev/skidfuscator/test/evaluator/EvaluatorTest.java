package dev.skidfuscator.test.evaluator;

import dev.skidfuscator.core.SkidTest;
import dev.skidfuscator.testclasses.TestRun;
import dev.skidfuscator.testclasses.evaluator.EvaluatorMain;
import dev.skidfuscator.testclasses.evaluator.manager.TestManager;
import dev.skidfuscator.testclasses.evaluator.operation.DoubleMathOperation;
import dev.skidfuscator.testclasses.evaluator.operation.IntMathOperation;
import dev.skidfuscator.testclasses.evaluator.operation.Operation;
import dev.skidfuscator.testclasses.evaluator.test.TestHandler;
import dev.skidfuscator.testclasses.evaluator.test.impl.Test2;
import dev.skidfuscator.testclasses.evaluator.test.impl.annotation.AnnotationTest;
import dev.skidfuscator.testclasses.evaluator.test.impl.annotation.TestAnnotation;
import dev.skidfuscator.testclasses.evaluator.test.impl.flow.OpaqueConditionTest;
import dev.skidfuscator.testclasses.evaluator.test.impl.flow.WeirdLoopTest;
import dev.skidfuscator.testclasses.evaluator.test.impl.string.StringTest;
import dev.skidfuscator.testclasses.evaluator.util.Evaluation;
import dev.skidfuscator.testclasses.evaluator.util.Log;
import dev.skidfuscator.testclasses.evaluator.util.crypto.AES;
import dev.skidfuscator.testclasses.evaluator.util.crypto.Blowfish;
import dev.skidfuscator.testclasses.evaluator.util.stats.Calculations;

public class EvaluatorTest extends SkidTest {
    @Override
    public Class<? extends TestRun> getMainClass() {
        return EvaluatorMain.class;
    }

    @Override
    public Class<?>[] getClasses() {
        return new Class[] {
                EvaluatorMain.class,
                TestManager.class,
                DoubleMathOperation.class,
                IntMathOperation.class,
                Operation.class,
                TestHandler.class,
                Test2.class,
                AnnotationTest.class,
                TestAnnotation.class,
                OpaqueConditionTest.class,
                WeirdLoopTest.class,
                StringTest.class,
                AES.class,
                Blowfish.class,
                Calculations.class,
                Evaluation.class,
                Log.class
        };
    }
}
