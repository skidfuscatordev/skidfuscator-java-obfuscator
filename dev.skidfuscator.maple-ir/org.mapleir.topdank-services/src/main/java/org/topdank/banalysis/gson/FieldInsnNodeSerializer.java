package org.topdank.banalysis.gson;

import java.lang.reflect.Type;

import org.objectweb.asm.tree.FieldInsnNode;

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
 * Time: 12:57 PM
 * Use:
 */
public class FieldInsnNodeSerializer implements JsonSerializer<FieldInsnNode>, JsonDeserializer<FieldInsnNode> {

    @Override
    public FieldInsnNode deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        int opcode;
        String owner = null, name = null, desc = null;
        JsonObject object = (JsonObject) json;
        opcode = object.get("opcode").getAsInt();
        owner = object.get("owner").getAsString();
        name = object.get("name").getAsString();
        desc = object.get("desc").getAsString();
        if(owner == null || name == null || desc == null)
            throw new JsonParseException("Could not parse FieldInsnNode");
        return new FieldInsnNode(opcode, owner, name, desc);
    }

    @Override
    public JsonElement serialize(FieldInsnNode src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("opcode", context.serialize(src.getOpcode(), Integer.class));
        jsonObject.add("owner", context.serialize(src.owner, String.class));
        jsonObject.add("name", context.serialize(src.name, String.class));
        jsonObject.add("desc", context.serialize(src.desc, String.class));
        return jsonObject;
    }
}
