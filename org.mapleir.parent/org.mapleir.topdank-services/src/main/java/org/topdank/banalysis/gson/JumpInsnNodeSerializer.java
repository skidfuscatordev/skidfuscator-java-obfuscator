package org.topdank.banalysis.gson;

import java.lang.reflect.Type;
import java.util.List;

import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;

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
 * Time: 1:48 PM
 * Use:
 */
public class JumpInsnNodeSerializer implements JsonSerializer<JumpInsnNode>, JsonDeserializer<JumpInsnNode> {
    
	@Override
    public JumpInsnNode deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = (JsonObject) json;
        int opcode = jsonObject.get("opcode").getAsInt();
        LabelNode labelNode = context.deserialize(jsonObject.get("local"), List.class);
        return new JumpInsnNode(opcode, labelNode);
    }

    @Override
    public JsonElement serialize(JumpInsnNode src, Type typeOfSrc, JsonSerializationContext context) {
    	JsonObject object = new JsonObject();
        object.add("opcode", context.serialize(src.getOpcode()));
        object.add("label", context.serialize(src.label));
        return object;
    }
}
