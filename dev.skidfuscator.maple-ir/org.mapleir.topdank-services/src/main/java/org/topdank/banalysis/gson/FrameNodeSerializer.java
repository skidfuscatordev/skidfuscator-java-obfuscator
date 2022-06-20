package org.topdank.banalysis.gson;

import java.lang.reflect.Type;
import java.util.List;

import org.objectweb.asm.tree.FrameNode;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 * User: Stl Date: 4/20/2014 Time: 1:08 PM Use:
 */
public class FrameNodeSerializer implements JsonSerializer<FrameNode>, JsonDeserializer<FrameNode> {

	@Override
	public FrameNode deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
		JsonObject jsonObject = (JsonObject) json;
		int opcode;
		int type;
		List<?> local = null, stack = null;
		opcode = jsonObject.get("opcode").getAsInt();
		type = jsonObject.get("type").getAsInt();
		local = context.deserialize(jsonObject.get("local"), List.class);
		stack = context.deserialize(jsonObject.get("stack"), List.class);
		FrameNode node = new FrameNode(opcode, local.size(), local.toArray(), stack.size(), stack.toArray());
		node.type = type;
		return node;
	}

	@Override
	public JsonElement serialize(FrameNode src, Type typeOfSrc, JsonSerializationContext context) {
		JsonObject object = new JsonObject();
		object.add("opcode", context.serialize(src.getOpcode()));
		object.add("type", context.serialize(src.type));
		object.add("local", context.serialize(src.local));
		object.add("stack", context.serialize(src.stack));
		return object;
	}
}
