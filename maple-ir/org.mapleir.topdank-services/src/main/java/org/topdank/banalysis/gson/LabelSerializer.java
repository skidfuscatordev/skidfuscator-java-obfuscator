package org.topdank.banalysis.gson;

import java.lang.reflect.Type;

import org.objectweb.asm.tree.LabelNode;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class LabelSerializer implements JsonSerializer<LabelNode>, JsonDeserializer<LabelNode>{

	@Override
	public JsonElement serialize(LabelNode src, Type typeOfT, JsonSerializationContext context) {
		return null;
	}
	
	@Override
	public LabelNode deserialize(JsonElement json, Type typeOfSrc, JsonDeserializationContext context) throws JsonParseException {
		return null;
	}
}