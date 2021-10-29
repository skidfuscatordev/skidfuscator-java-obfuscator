package dev.skidfuscator.test;

import dev.skidfuscator.obf.Bootstrapper;
import org.junit.Test;
import org.mapleir.Boot;
import org.mapleir.Main;

/**
 * @author Ghast
 * @since 06/03/2021
 * SkidfuscatorV2 © 2021
 */

public class EntryPointTest {

    @Test
    public void test2() throws Exception {
        final String path = "src/test/resources/test.jar";
        Bootstrapper.main(new String[]{path});
        //Bootstrapper.main(new String[]{"C:\\Users\\sanja\\Documents\\GitHub\\SkidfuscatorV2\\dev.skidfuscator.obfuscator\\src\\test\\resources\\test.jar"});
    }

}
