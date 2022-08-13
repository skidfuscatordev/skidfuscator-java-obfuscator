package dev.skidfuscator.testclasses.evaluator;

import dev.skidfuscator.testclasses.TestRun;
import dev.skidfuscator.testclasses.evaluator.manager.TestManager;
import dev.skidfuscator.testclasses.evaluator.operation.DoubleMathOperation;
import dev.skidfuscator.testclasses.evaluator.operation.IntMathOperation;
import dev.skidfuscator.testclasses.evaluator.operation.Operation;
import dev.skidfuscator.testclasses.evaluator.util.Evaluation;
import dev.skidfuscator.testclasses.evaluator.util.Log;
import dev.skidfuscator.testclasses.evaluator.util.crypto.Blowfish;
import dev.skidfuscator.testclasses.evaluator.util.stats.Calculations;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class EvaluatorMain implements TestRun {
    private static final SecureRandom RANDOM = new SecureRandom();
    public static final Log LOG = new Log();

    @Override
    public void run() {
        int[] intValues = new int[] { 813,432,784,409,600,552,923,51,275,988,774,74,693,892,957,398,636,530,472,769,106,259,450,893,355 };
        double[] doubleValues = new double[] { 15.354279285687706,5.797782664265068,8.683696317015794,1.9817656768587806,3.535287429360438,4.220760053178631,10.807260410843776,9.79012425459241,9.862795945665074,0.74113233949422,2.422188626186955,9.624071224255548,0.21480131492236743,10.736554500849767,2.7573095161824757,16.295928424685112,1.5007304056520934,11.312333434915566,0.2805255257633217,2.158320252411026,0.0,8.556101546454709,1.1028629585647993,15.846849796405586,5.932633085882487 };

        LOG.println(String.format("Today's date is %s", LocalDate.now()));

        Supplier<Integer> randomInt = () -> 1 + RANDOM.nextInt(20);
        Supplier<Integer> randomIntMathOp = () -> RANDOM.nextInt(IntMathOperation.values().length);

        LOG.println("Performing small int test...");

        Calculations calculations = new Calculations();

        List<Evaluation<Integer>> evaluations = Stream.generate(() -> new Evaluation<>(randomInt.get(), randomInt.get(), n -> {
            IntMathOperation op = IntMathOperation.values()[RANDOM.nextInt(IntMathOperation.values().length)];

            int first = randomInt.get();
            int second = randomInt.get();

            LOG.print("%s %s %s = %s\n", first, op.getDesc(), second, calculations.store(op.evaluate(first, second)));
        })).limit(5 + randomInt.get()).collect(Collectors.toList());

        evaluations.forEach(e -> e.getEvaluator().accept(randomIntMathOp.get()));

        LOG.println("\nPerforming random math operations...");
        IntStream.range(0, 5 + RANDOM.nextInt(20))
                .forEach(i -> {
                    Operation operation = RANDOM.nextBoolean() ? Operation.INT : Operation.DOUBLE;

                    switch (operation) {
                        case INT: {
                            IntMathOperation op = IntMathOperation.values()[RANDOM.nextInt(IntMathOperation.values().length)];

                            LOG.print("Performing int test %s. Operation name is %s\n", i, op.getDesc());

                            int first = intValues[RANDOM.nextInt(intValues.length)];
                            int second = intValues[RANDOM.nextInt(intValues.length)];

                            LOG.print("%s %s %s = %s\n", first, op.getDesc(), second, calculations.store(op.evaluate(first, second)));
                            break;
                        }

                        case DOUBLE: {
                            DoubleMathOperation op = DoubleMathOperation.values()[RANDOM.nextInt(DoubleMathOperation.values().length)];

                            LOG.print("Performing double test %s. Operation name is %s\n", i, op.getDesc());

                            double first = doubleValues[RANDOM.nextInt(doubleValues.length)];
                            double second = doubleValues[RANDOM.nextInt(doubleValues.length)];

                            LOG.print("%s %s %s = %s\n", first, op.getDesc(), second, calculations.store(op.evaluate(first, second)));
                            break;
                        }

                        default:
                            System.out.println("Unsupported");
                            break;
                    }
                });

        calculations.run(LOG);

        TestManager testManager = new TestManager();

        testManager.handleTests();

        LOG.println("\nTesting cryptography (Blowfish)");
        Blowfish blowfish = new Blowfish("jHASf72183hjASf123");

        // Result should be: "hello world 123 " + System.currentTimeMillis() (1605479835458)
        String encryptedString = "5f45e43ca774e1d2611c6fc31c7e4b11ef4780e0ba9ba304b9da8b28bc86582e6745624bb00bfbc7a71f97a1e708e13bcfd6f700d77216680a52dc1d16e3a9dc2747a26466eb273d";
        String decryptedString = blowfish.decryptString(encryptedString);

        String test = "5f45e43ca774e1d2611c6fc31c7e4b11ef4780e0ba9ba304b9da8b28bc86582e6745624bb00bfbc7a71f97a1e708e13bcfd6f700d77216680a52dc1d16e3a9dc2747a26466eb273d5f45e43ca774e1d2611c6fc31c7e4b11ef4780e0ba9ba304b9da8b28bc86582e6745624bb00bfbc7a71f97a1e708e13bcfd6f700d77216680a52dc1d16e3a9dc2747a26466eb273d";
        System.out.println("Testing large string");

        boolean successfulStringCompare = test.equals(encryptedString + encryptedString);

        LOG.println(successfulStringCompare ? "Successfully compared strings" : "Unable to compare strings");

        boolean successfulDecrypt = decryptedString.equals("hello world 123 1605479835458");

        LOG.println(successfulDecrypt ? "Successfully decrypted " + decryptedString : "Failed to decrypt " + decryptedString);
    }
}
