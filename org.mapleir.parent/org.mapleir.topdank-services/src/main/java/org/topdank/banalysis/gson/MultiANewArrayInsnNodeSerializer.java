package org.topdank.banalysis.gson;

import java.lang.reflect.Type;

import org.objectweb.asm.tree.MultiANewArrayInsnNode;

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
 * Time: 1:51 PM
 * Use:
 */
public class MultiANewArrayInsnNodeSerializer implements JsonSerializer<MultiANewArrayInsnNode>, JsonDeserializer<MultiANewArrayInsnNode> {
    
	@Override
    public MultiANewArrayInsnNode deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
		JsonObject jsonObject = (JsonObject) json;
		String desc = jsonObject.get("desc").getAsString();
		int dims = jsonObject.get("dims").getAsInt();
        return new MultiANewArrayInsnNode(desc, dims);
    }

    @Override
    public JsonElement serialize(MultiANewArrayInsnNode src, Type typeOfSrc, JsonSerializationContext context) {
    	JsonObject object = new JsonObject();
        object.add("desc", context.serialize(src.desc));
        object.add("dims", context.serialize(src.dims));
        return object;
    }
}
