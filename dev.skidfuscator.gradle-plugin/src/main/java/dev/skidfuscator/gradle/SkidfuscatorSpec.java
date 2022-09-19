package dev.skidfuscator.gradle;

import lombok.Builder;
import lombok.Getter;

import java.io.File;
import java.util.List;

@Builder
@Getter
public class SkidfuscatorSpec {
    private File input;
    private File output;
    private File[] libs;
    private File mappings;
    private File exempt;
    private File runtime;
    private List<String> excludes;
    private boolean phantom;
    private boolean jmod;
    private boolean fuckit;
    private boolean analytics;
}
