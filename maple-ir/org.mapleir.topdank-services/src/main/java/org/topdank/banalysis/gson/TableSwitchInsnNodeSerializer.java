package org.topdank.banalysis.gson;

import java.lang.reflect.Type;
import java.util.List;

import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;

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
 * Time: 1:52 PM
 * Use:
 */
public class TableSwitchInsnNodeSerializer implements JsonSerializer<TableSwitchInsnNode>, JsonDeserializer<TableSwitchInsnNode> {
    
	@Override
    public TableSwitchInsnNode deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
		JsonObject jsonObject = (JsonObject) json;
        int min = jsonObject.get("min").getAsInt();
        int max = jsonObject.get("max").getAsInt();
        LabelNode dflt = context.deserialize(jsonObject.get("dflt"), LabelNode.class);
        List<LabelNode> labelList = context.deserialize(jsonObject.get("labels"), List.class);
        LabelNode[] labels = new LabelNode[labelList.size()];
        for(int i=0; i < labels.length; i++){
        	labels[i] = labelList.get(i);
        }
        return new TableSwitchInsnNode(min, max, dflt, labels);
    }

    @Override
    public JsonElement serialize(TableSwitchInsnNode src, Type typeOfSrc, JsonSerializationContext context) {
    	JsonObject object = new JsonObject();
        object.add("min", context.serialize(src.min));
        object.add("max", context.serialize(src.max));
        object.add("dflt", context.serialize(src.dflt));
        object.add("labels", context.serialize(src.labels));
        return object;
    }
}
