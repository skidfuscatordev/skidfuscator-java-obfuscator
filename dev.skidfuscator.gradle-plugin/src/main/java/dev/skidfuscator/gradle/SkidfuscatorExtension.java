package dev.skidfuscator.gradle;

import lombok.Getter;
import org.gradle.api.file.RegularFile;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;

import javax.inject.Inject;

@Getter
public class SkidfuscatorExtension {

    private final Property<Boolean> phantom;
    private final Property<Boolean> notrack;
    private final Property<Boolean> fuckit;
    private final Property<String> classifier;
    private final Property<String> exemptionString;
    private final Property<RegularFile> runtime;
    private final Property<RegularFile> exemption;

    @Inject
    public SkidfuscatorExtension(ObjectFactory objectFactory) {
        this.phantom = objectFactory.property(Boolean.class).convention(false);
        this.notrack = objectFactory.property(Boolean.class).convention(false);
        this.fuckit = objectFactory.property(Boolean.class).convention(false);
        this.runtime = objectFactory.property(RegularFile.class);
        this.classifier = objectFactory.property(String.class).convention("-obfuscated");
        this.exemptionString = objectFactory.property(String.class);
        this.exemption = objectFactory.property(RegularFile.class);
    }

}
