package org.topdank.banalysis.gson;

import java.lang.reflect.Type;

import org.objectweb.asm.tree.IincInsnNode;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 * User: Stl
 * Date: 4/20/2014
 * Time: 1:23 PM
 * Use:
 */
public class IincInsNodeSerializer implements JsonSerializer<IincInsnNode>, JsonDeserializer<IincInsnNode> {
    
	@Override
    public IincInsnNode deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = (JsonObject) json;
        int var, incr;
        var = jsonObject.get("var").getAsInt();
        incr = jsonObject.get("incr").getAsInt();
        return new IincInsnNode(var, incr);
    }

    @Override
    public JsonElement serialize(IincInsnNode src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("var", context.serialize(src.var, Integer.class));
        jsonObject.add("incr", context.serialize(src.incr, Integer.class));
        return jsonObject;
    }
}