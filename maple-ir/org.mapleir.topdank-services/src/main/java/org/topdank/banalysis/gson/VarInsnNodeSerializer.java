package org.topdank.banalysis.gson;

import java.lang.reflect.Type;

import org.objectweb.asm.tree.VarInsnNode;

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
 * Time: 1:53 PM
 * Use:
 */
public class VarInsnNodeSerializer implements JsonSerializer<VarInsnNode>, JsonDeserializer<VarInsnNode> {
    
	@Override
    public VarInsnNode deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
		JsonObject jsonObject = (JsonObject) json;
        int opcode = jsonObject.get("opcode").getAsInt();
        int var = jsonObject.get("var").getAsInt();
        return new VarInsnNode(opcode, var);
    }

    @Override
    public JsonElement serialize(VarInsnNode src, Type typeOfSrc, JsonSerializationContext context) {
    	JsonObject object = new JsonObject();
        object.add("opcode", context.serialize(src.getOpcode()));
        object.add("var", context.serialize(src.var));
        return object;
    }
}
