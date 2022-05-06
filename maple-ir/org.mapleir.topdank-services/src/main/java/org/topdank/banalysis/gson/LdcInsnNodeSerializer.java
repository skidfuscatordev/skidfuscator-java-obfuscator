package org.topdank.banalysis.gson;

import java.lang.reflect.Type;

import org.objectweb.asm.tree.LdcInsnNode;

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
 * Time: 1:49 PM
 * Use:
 */
public class LdcInsnNodeSerializer implements JsonSerializer<LdcInsnNode>, JsonDeserializer<LdcInsnNode> {
    
	@Override
    public LdcInsnNode deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
		JsonObject jsonObject = (JsonObject) json;
        Object cst = context.deserialize(jsonObject.get("cst"), Object.class);
        return new LdcInsnNode(cst);
    }

    @Override
    public JsonElement serialize(LdcInsnNode src, Type typeOfSrc, JsonSerializationContext context) {
    	JsonObject object = new JsonObject();
        object.add("cst", context.serialize(src.cst));
        return object;
    }
}
