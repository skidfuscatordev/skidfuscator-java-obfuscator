package org.topdank.banalysis.gson;

import java.lang.reflect.Type;
import java.util.List;

import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;

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
 * Time: 1:50 PM
 * Use:
 */
public class LookupSwitchInsnNodeSerializer implements JsonSerializer<LookupSwitchInsnNode>, JsonDeserializer<LookupSwitchInsnNode> {
    
	@Override
    public LookupSwitchInsnNode deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
		JsonObject jsonObject = (JsonObject) json;
        LabelNode dflt = context.deserialize(jsonObject.get("dflt"), LabelNode.class);
        List<Integer> keysList = context.deserialize(jsonObject.get("keys"), List.class);
        List<LabelNode> labelsList = context.deserialize(jsonObject.get("labels"), List.class);
        int[] keys = new int[keysList.size()];
        for(int i=0; i < keys.length; i++){
        	keys[i] = keysList.get(i);
        }
        LabelNode[] labels = new LabelNode[labelsList.size()];
        for(int i=0; i < labels.length; i++){
        	labels[i] = labelsList.get(i);
        }
        return new LookupSwitchInsnNode(dflt, keys, labels);
    }

    @Override
    public JsonElement serialize(LookupSwitchInsnNode src, Type typeOfSrc, JsonSerializationContext context) {
    	JsonObject object = new JsonObject();
        object.add("dflt", context.serialize(src.dflt));
        object.add("key", context.serialize(src.keys));
        object.add("labels", context.serialize(src.labels));
        return object;
    }
}
