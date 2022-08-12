package org.topdank.banalysis.gson;

import java.lang.reflect.Type;

import org.objectweb.asm.tree.IntInsnNode;

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
 * Time: 1:33 PM
 * Use:
 */
public class IntInsnNodeSerializer implements JsonSerializer<IntInsnNode>, JsonDeserializer<IntInsnNode> {
    
	@Override
    public IntInsnNode deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = (JsonObject) json;
        int opcode, operand;
        opcode = jsonObject.get("opcode").getAsInt();
        operand = jsonObject.get("operand").getAsInt();
        return new IntInsnNode(opcode, operand);
    }

    @Override
    public JsonElement serialize(IntInsnNode src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("opcode", context.serialize(src.getOpcode(), Integer.class));
        jsonObject.add("operand", context.serialize(src.operand, Integer.class));
        return jsonObject;
    }
}
