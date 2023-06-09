package dev.skidfuscator.obfuscator.exempt;

import dev.skidfuscator.obfuscator.util.OpcodeUtil;
import dev.skidfuscator.obfuscator.util.match.Match;
import lombok.experimental.UtilityClass;
import org.mapleir.asm.ClassNode;
import org.mapleir.asm.FieldNode;
import org.mapleir.asm.MethodNode;

import java.util.Locale;
import java.util.regex.Pattern;

@UtilityClass
public class ExclusionHelper {
    public Exclusion renderExclusion(final String pattern) {
        final char[] characters = pattern.toCharArray();
        int index = 0;
        StringBuilder padded = new StringBuilder();

        ExclusionMap map = new ExclusionMap();
        ExclusionType type = null;

        while (index < characters.length) {
            final char c = characters[index];

            if (type == null) {
                /* Skip over white spaces */
                if (c == ' ') {
                    index++;
                    continue;
                }

                /* If we encounter another parse type, open it */
                if (c == '{') {
                    switch (padded.toString().toLowerCase(Locale.ROOT)) {
                        case "class": {
                            type = ExclusionType.CLASS;
                            break;
                        }

                        case "method": {
                            type = ExclusionType.METHOD;
                            break;
                        }

                        case "field": {
                            type = ExclusionType.FIELD;
                            break;
                        }
                    }

                    padded = new StringBuilder();

                } else {
                    padded.append(c);
                }
            }

            else {
                if (c == '}') {
                    final String matcher = padded.toString();
                    final String[] split = matcher.contains(" ")
                            ? matcher.split(" ")
                            : new String[]{matcher};
                    final String parsed = split[split.length - 1];

                    switch (type) {
                        case CLASS: {
                            final Pattern regex = Pattern.compile(parsed);
                            map.put(type, new ExclusionTester<ClassNode>() {
                                @Override
                                public boolean test(ClassNode var) {
                                    final boolean initialMatch = Match
                                            .of(matcher)
                                            .match("static", var.isStatic())
                                            .match("public", var.isPublic())
                                            .match("protected", var.isProtected())
                                            .match("private", var.isPrivate())
                                            .check();

                                    assert initialMatch : "Failed initial match: " + parsed + " got:" + var;
                                    assert !var.getName().contains(".") : "Got weird name: " + var.getName();

                                    final boolean ret =  initialMatch
                                            && (regex.matcher(var.getName()).find() || parsed.equals(var.getName()));

                                    if (var.getName().equals("jda")) {
                                        System.out.println("JDA! " + var.getName() + " --> " + ret);
                                    }

                                    //assert var.getName().contains("jda") == ret : "name: " + var.getName() + " parser: " + parsed;
                                    return ret;
                                }

                                @Override
                                public String toString() {
                                    return regex.pattern();
                                }
                            });

                            break;
                        }

                        case METHOD: {
                            final String[] splitz = parsed.split("#");
                            final String method = splitz[0];
                            final String desc = splitz.length > 1 ? splitz[1] : null;
                            final Pattern regexMethod = Pattern.compile(method);
                            final Pattern descRegex = desc == null ? null : Pattern.compile(desc);

                            map.put(type, new ExclusionTester<MethodNode>() {
                                @Override
                                public boolean test(MethodNode var) {
                                    final boolean initialMatch = Match
                                            .of(matcher)
                                            .match("static", var.isStatic())
                                            .match("public", var.isPublic())
                                            .match("protected", var.isProtected())
                                            .match("private", var.isPrivate())
                                            .check();

                                    if (!initialMatch) {
                                        return false;
                                    }

                                    if (descRegex != null && !descRegex.matcher(var.getDesc()).lookingAt()) {
                                        return false;
                                    }

                                    return regexMethod.matcher(var.getName()).lookingAt() || parsed.equals(var.getName());
                                            //&& regexClazz.matcher(var.owner.getDisplayName()).matches();
                                }

                                @Override
                                public String toString() {
                                    return regexMethod.toString();
                                }
                            });
                            break;
                        }

                        case FIELD: {
                            final String clazz = parsed.split("#")[0];
                            final String method = parsed.split("#")[1];

                            final Pattern regexClazz = Pattern.compile(clazz);
                            final Pattern regexField = Pattern.compile(method);

                            map.put(type, new ExclusionTester<FieldNode>() {
                                @Override
                                public boolean test(FieldNode var) {
                                    final boolean initialMatch = Match
                                            .of(matcher)
                                            .match("static", var.isStatic())
                                            .match("public", var.isPublic())
                                            .match("protected", var.isProtected())
                                            .match("private", var.isPrivate())
                                            .check();

                                    return initialMatch
                                            && regexField.matcher(var.getDisplayName()).lookingAt()
                                            && regexClazz.matcher(var.owner.getDisplayName()).lookingAt();
                                }
                            });
                            break;
                        }
                    }

                    padded = new StringBuilder();
                } else {
                    padded.append(c);
                }
            }

            index++;
        }

        if (!map.containsKey(ExclusionType.CLASS)) {
            map.put(ExclusionType.CLASS, new ExclusionTester<ClassNode>() {
                @Override
                public boolean test(ClassNode var) {
                    return false;
                }

                @Override
                public String toString() {
                    return "ExclusionTester={DefaultExemptTester}";
                }
            });
        }

        if (!map.containsKey(ExclusionType.METHOD)) {
            map.put(ExclusionType.METHOD, new ExclusionTester<MethodNode>() {
                @Override
                public boolean test(MethodNode var) {
                    return false;
                }

                @Override
                public String toString() {
                    return "ExclusionTester={DefaultExemptTester}";
                }
            });
        }

        return new Exclusion(map);
    }
}