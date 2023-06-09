package dev.skidfuscator.test;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.SkidfuscatorSession;
import dev.skidfuscator.obfuscator.transform.impl.SwitchTransformer;
import dev.skidfuscator.obfuscator.transform.impl.flow.driver.DriverTransformer;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigTest {


    @Test
    public void testDefaultConfig() {
        final TestConfigSkidfuscator skidfuscator = new TestConfigSkidfuscator(
                SkidfuscatorSession.builder()
                        .config(new File(this.getClass().getResource("/config/config.hocon").getFile()))
                        .build()
        );

        skidfuscator._importConfig();

        // General exemption check
        assertArrayEquals(
                new String[] {"class{^dev\\/skidfuscator\\/test}"},
                skidfuscator.getConfig().getExemptions().toArray(new String[0])
        );

        // General driver check
        assertEquals(
                true,
                skidfuscator.getConfig().isDriver()
        );

        // Specific enabled check
        // This is *stated* to be enabled
        final DriverTransformer driverTransformer = new DriverTransformer(
                skidfuscator
        );
        assertEquals(
                true,
                skidfuscator.getTsConfig().hasPath("driver")
        );
        assertEquals(
                true,
                skidfuscator.getTsConfig().hasPath("driver.enabled")
        );
        assertEquals(
                true,
                skidfuscator.getTsConfig().getBoolean("driver.enabled")
        );
        assertEquals(
                true,
                driverTransformer.getConfig().isEnabled()
        );

        // General enabled check
        // This is enabled by *default*
        final SwitchTransformer switchTransformer = new SwitchTransformer(
                skidfuscator
        );
        assertEquals(
                true,
                switchTransformer.getConfig().isEnabled()
        );

    }

    static class TestConfigSkidfuscator extends Skidfuscator {
        public TestConfigSkidfuscator(SkidfuscatorSession session) {
            super(session);
        }

        @Override
        public void _importConfig() {
            super._importConfig();
        }
    }
}
