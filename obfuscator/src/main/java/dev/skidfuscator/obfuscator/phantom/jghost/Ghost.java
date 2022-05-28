package dev.skidfuscator.obfuscator.phantom.jghost;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Ghost {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .disableHtmlEscaping()
            .create();

    public static Gson gson() {
        return GSON;
    }


}
