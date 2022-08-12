package org.topdank.banalysis.gson;

import java.lang.reflect.Type;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodInsnNode;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 * User: Stl Date: 4/20/2014 Time: 1:50 PM Use:
 */
public class MethodInsnNodeSerializer implements JsonSerializer<MethodInsnNode>, JsonDeserializer<MethodInsnNode> {
	
	@Override
	public MethodInsnNode deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
		JsonObject jsonObject = (JsonObject) json;
		int opcode = jsonObject.get("opcode").getAsInt();
		String owner = jsonObject.get("owner").getAsString();
		String name = jsonObject.get("name").getAsString();
		String desc = jsonObject.get("desc").getAsString();
		return new MethodInsnNode(opcode, owner, name, desc, opcode == Opcodes.INVOKEINTERFACE);
	}
	
	@Override
	public JsonElement serialize(MethodInsnNode src, Type typeOfSrc, JsonSerializationContext context) {
		JsonObject object = new JsonObject();
		object.add("opcode", context.serialize(src.getOpcode()));
		object.add("owner", context.serialize(src.owner));
		object.add("name", context.serialize(src.name));
		object.add("desc", context.serialize(src.desc));
		return object;
	}
}
