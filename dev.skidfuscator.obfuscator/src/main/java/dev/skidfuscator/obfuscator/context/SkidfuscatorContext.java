package dev.skidfuscator.obfuscator.context;

import org.mapleir.app.service.ApplicationClassSource;

public class SkidfuscatorContext {
    private ApplicationClassSource mainSource;

    public SkidfuscatorContext(ApplicationClassSource applicationClassSource) {
        this.mainSource = applicationClassSource;
    }

    public void resolveJdk() {

    }
}
