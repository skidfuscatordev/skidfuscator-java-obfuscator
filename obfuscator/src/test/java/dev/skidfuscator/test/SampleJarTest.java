package dev.skidfuscator.test;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.SkidfuscatorSession;
import org.junit.Test;

import java.io.File;

/**
 * @author Ghast
 * @since 06/03/2021
 * SkidfuscatorV2 © 2021
 */

public class SampleJarTest {

    @Test
    public void test2() throws Exception {
        final File input = new File("src/test/resources/test.jar");
        final File output = new File("src/test/resources/test-out.jar");
        final SkidfuscatorSession session = SkidfuscatorSession
                .builder()
                .input(input)
                .output(output)
                .runtime(new File(System.getProperty("java.home"), "lib/rt.jar"))
                .phantom(true)
                .build();

        final Skidfuscator skidfuscator = new Skidfuscator(session);
        skidfuscator.run();
        //Bootstrapper.main(new String[]{"C:\\Users\\sanja\\Documents\\GitHub\\SkidfuscatorV2\\dev.skidfuscator.obfuscator\\src\\test\\resources\\test.jar"});
    }

}
