package dev.skidfuscator.obfuscator.dependency;

import dev.skidfuscator.obfuscator.dependency.matcher.DependencyMatcher;

import java.util.List;

public enum CommonDependency {
    BUKKIT("https://raw.githubusercontent.com/skidfuscatordev/mappings/6887752ccaa209f19fcb86b44b44f51720d72009/spigot/1.21/paper-1.21.zip", "3e7498d83e0124b796af1503483d8e3435f7a5d57ce0b9681ea4287654451eb7", new DependencyMatcher() {
        @Override
        public boolean test(List<String> strings) {
            return strings.stream().anyMatch(e -> e.contains("org/bukkit"));
        }
    }),
    CONSCRYPT("https://raw.githubusercontent.com/skidfuscatordev/mappings/6887752ccaa209f19fcb86b44b44f51720d72009/conscrypt/2.5.2/org.conscrypt.conscrypt-openjdk-uber-2.5.2.json", "5999f18a4e52b012270ce744ac715b00fc23e582816f836f19d0e35a68abae37", new DependencyMatcher() {
        @Override
        public boolean test(List<String> strings) {
            return strings.stream().anyMatch(e -> e.contains("org/conscrypt"));
        }
    })
    ;

    private final String url;
    private final String sha256;
    public String getSha256() { return sha256; }
    private final DependencyMatcher matcher;

    CommonDependency(String url, String sha256, DependencyMatcher matcher) {
        this.url = url;
        this.sha256 = sha256;
        this.matcher = matcher;
    }

    public String getUrl() {
        return url;
    }

    public DependencyMatcher getMatcher() {
        return matcher;
    }
}
