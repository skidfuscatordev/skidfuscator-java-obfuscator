package dev.skidfuscator.jghost.tree;

import com.google.gson.annotations.SerializedName;

public class GhostLibrary {
    @SerializedName("name")
    private String name;

    @SerializedName("md5")
    private String md5;

    @SerializedName("sha2")
    private String sha2;

    @SerializedName("sha256")
    private String sha256;

    @SerializedName("contents")
    private GhostContents contents = new GhostContents();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public String getSha2() {
        return sha2;
    }

    public void setSha1(String sha2) {
        this.sha2 = sha2;
    }

    public String getSha256() {
        return sha256;
    }

    public void setSha256(String sha256) {
        this.sha256 = sha256;
    }

    public GhostContents getContents() {
        return contents;
    }

    public void setContents(GhostContents contents) {
        this.contents = contents;
    }

    public void merge(final GhostLibrary library) {
        this.getContents().getClasses().putAll(library.getContents().getClasses());
    }
}
