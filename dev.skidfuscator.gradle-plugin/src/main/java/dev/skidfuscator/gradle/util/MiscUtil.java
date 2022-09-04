package dev.skidfuscator.gradle.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class MiscUtil {

    public int getJavaVersion() {
        String version = System.getProperty("java.version");
        return decodeJvmVersion(version);
    }

    public int decodeJvmVersion(String version) {
        if(version.startsWith("1.")) {
            version = version.substring(2, 3);
        } else {
            int dot = version.indexOf(".");
            if(dot != -1) { version = version.substring(0, dot); }
        } return Integer.parseInt(version);
    }

}
