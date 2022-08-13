package dev.skidfuscator.testclasses.evaluator.test.impl.flow;

import dev.skidfuscator.testclasses.evaluator.test.TestHandler;
import dev.skidfuscator.testclasses.evaluator.util.crypto.AES;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class OpaqueConditionTest implements TestHandler {

    private static final byte[] data = new byte[] {0, 1, 4, 3, 2};

    @Override
    public void handle() {
        int stage = 0;
        try {
            // Begin stage
            stage = 1;

            // Call a function capable of throwing an exception, then update stage
            AES.main(new String[0]);
            stage = 2;

            if (data[0] == 0) {
                switch (data[1]) {
                    case 0:
                        stage = -2;
                        break;
                    case 1:
                        stage = 3;
                        break;
                    default:
                        stage = -5;
                        break;
                }
            } else {
                throw new IllegalArgumentException("Failed test! Stage: " + stage);
            }

            self(stage);

            throw new IllegalArgumentException("Failed test! Stage: " + stage);
        } catch (IllegalStateException e) {
            stage = 4;
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException | InvalidKeyException e) {
            e.printStackTrace();

            stage = -1;
        }

        if (data[2] != stage) {
            throw new IllegalArgumentException("Failed test! Stage: " + stage);
        }
    }

    private void self(final int stage) {
        if (stage == 3)
            throw new IllegalStateException("stage=" + stage);
    }
}
