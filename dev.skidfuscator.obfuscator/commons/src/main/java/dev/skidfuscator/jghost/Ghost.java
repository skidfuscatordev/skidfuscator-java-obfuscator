package dev.skidfuscator.jghost;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.objectweb.asm.Type;

import java.io.IOException;

public class Ghost {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .serializeSpecialFloatingPointValues()
            .registerTypeAdapter(Type.class, new TypeSerializer())
            .create();

    public static Gson gson() {
        return GSON;
    }

    static class TypeSerializer extends TypeAdapter<Type> {
        @Override
        public void write(JsonWriter out, Type value) throws IOException {
            out     .beginObject()
                    .name("itrlNme").value(value.getInternalName())
                    .endObject();
        }

        @Override
        public Type read(JsonReader in) throws IOException {
            in.beginObject();
            in.nextName();
            Type type = Type.getType(in.nextString());
            in.endObject();
            return type;
        }
    }

}
