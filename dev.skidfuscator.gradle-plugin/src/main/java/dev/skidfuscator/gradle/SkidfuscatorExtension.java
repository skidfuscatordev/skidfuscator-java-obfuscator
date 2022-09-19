package dev.skidfuscator.gradle;

import lombok.Getter;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

import javax.inject.Inject;

@Getter
public class SkidfuscatorExtension {

    private final Property<String> version;
    private final Property<Boolean> phantom;
    private final Property<Boolean> notrack;
    private final Property<Boolean> fuckit;
    private final Property<String> classifier;
    private final ListProperty<String> excludes;
    private final RegularFileProperty runtime;
    private final RegularFileProperty exemptionFile;

    @Inject
    public SkidfuscatorExtension(ObjectFactory objectFactory) {
        this.version = objectFactory.property(String.class).convention("2.0.0-SNAPSHOT"); // automatically fetching?
        this.phantom = objectFactory.property(Boolean.class).convention(false);
        this.notrack = objectFactory.property(Boolean.class).convention(false);
        this.fuckit = objectFactory.property(Boolean.class).convention(false);
        this.runtime = objectFactory.fileProperty();
        this.classifier = objectFactory.property(String.class).convention("-obfuscated");
        this.excludes = objectFactory.listProperty(String.class).empty();
        this.exemptionFile = objectFactory.fileProperty();
    }

    public void exclude(String... exludes) {
        this.excludes.addAll(exludes);
    }

    public void phantom() {
        this.phantom.set(true);
    }

    public void notrack() {
        this.notrack.set(true);
    }

    public void fuckit() {
        this.fuckit.set(true);
    }

}
