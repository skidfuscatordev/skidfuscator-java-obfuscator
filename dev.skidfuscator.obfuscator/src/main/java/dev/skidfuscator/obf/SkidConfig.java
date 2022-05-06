package dev.skidfuscator.obf;

import org.bovinegenius.kurgan.KurganField;
import org.bovinegenius.kurgan.Tester;

import java.io.File;
import java.util.List;

public interface SkidConfig {
    @KurganField("input")
    Tester.Resource<File> input();

    @KurganField("runtime")
    Tester.Resource<File> runtime();

    @KurganField("libraries")
    Tester.Resource<File> libs();

    @KurganField("output")
    Tester.Resource<File> output();

    @KurganField("anti-dump")
    AntiDump preventDump();

    @KurganField("exclusions")
    List<String> excludes();

    interface AntiDump extends Module { }

    interface Module {
        @KurganField("enabled")
        boolean enabled();
    }
}
