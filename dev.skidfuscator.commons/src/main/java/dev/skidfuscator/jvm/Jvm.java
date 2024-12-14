package dev.skidfuscator.jvm;

import lombok.experimental.UtilityClass;

import java.io.File;

@UtilityClass
public class Jvm {

    public int getJavaVersion() {
        String version = System.getProperty("java.version");
        return decodeJvmVersion(version);
    }

    public String getJavaVersionString() {
        return System.getProperty("java.version");
    }

    public String getJvmLibs() {
        return System.getProperty("java.home");
    }

    public int decodeJvmVersion(String version) {
        if(version.startsWith("1.")) {
            version = version.substring(2, 3);
        } else {
            int dot = version.indexOf(".");
            if(dot != -1) { version = version.substring(0, dot); }
        } return Integer.parseInt(version);
    }

    public boolean isJmod() {
        return getJavaVersion() > 8;
    }

    public String getLibsPath() {
        final String home = System.getProperty("java.home");
        return home + File.separator + (getJavaVersion() > 8 ? "jmods" : "lib/rt.jar");
    }
}
