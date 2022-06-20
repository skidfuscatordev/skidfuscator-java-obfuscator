package dev.skidfuscator.jghost.tree;

import com.google.gson.annotations.SerializedName;

import java.util.HashMap;
import java.util.Map;

public class GhostContents {
    @SerializedName("classes")
    private Map<String, GhostClassNode> classes = new HashMap<>();

    public Map<String, GhostClassNode> getClasses() {
        return classes;
    }

    public void setClasses(Map<String, GhostClassNode> classes) {
        this.classes = classes;
    }
}
