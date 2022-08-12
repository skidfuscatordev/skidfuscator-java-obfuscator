package org.topdank.banalysis.gson;

import java.lang.reflect.Type;

import org.objectweb.asm.tree.TypeInsnNode;

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
public class TypeInsnNodeSerializer implements JsonSerializer<TypeInsnNode>, JsonDeserializer<TypeInsnNode> {
    
	@Override
    public TypeInsnNode deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
		JsonObject jsonObject = (JsonObject) json;
        int opcode = jsonObject.get("opcode").getAsInt();
        String desc  = jsonObject.get("desc").getAsString();
        return new TypeInsnNode(opcode, desc);
    }

    @Override
    public JsonElement serialize(TypeInsnNode src, Type typeOfSrc, JsonSerializationContext context) {
    	JsonObject object = new JsonObject();
        object.add("opcode", context.serialize(src.getOpcode()));
        object.add("desc", context.serialize(src.desc));
        return object;
    }
}
