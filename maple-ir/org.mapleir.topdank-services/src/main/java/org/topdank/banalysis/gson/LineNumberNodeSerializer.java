package org.topdank.banalysis.gson;

import java.lang.reflect.Type;

import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;

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
public class LineNumberNodeSerializer implements JsonSerializer<LineNumberNode>, JsonDeserializer<LineNumberNode> {
   
	@Override
    public LineNumberNode deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
		JsonObject jsonObject = (JsonObject) json;
        int line = jsonObject.get("line").getAsInt();
        LabelNode start = context.deserialize(jsonObject.get("start"), LabelNode.class);
        return new LineNumberNode(line, start);
    }

    @Override
    public JsonElement serialize(LineNumberNode src, Type typeOfSrc, JsonSerializationContext context) {
    	JsonObject object = new JsonObject();
        object.add("line", context.serialize(src.line));
        object.add("start", context.serialize(src.start));
        return object;
    }
}
