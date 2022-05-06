package dev.skidfuscator.obf.exclusion;

import dev.skidfuscator.obf.utils.ClassUtil;
import dev.skidfuscator.obf.utils.Match;
import dev.skidfuscator.obf.utils.MatchPair;
import dev.skidfuscator.obf.utils.OpcodeUtil;
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
            } else {
                if (c == '}') {
                    final String matcher = padded.toString().toLowerCase(Locale.ROOT);
                    final String[] split = matcher.contains(" ") ? matcher.split(" ") : new String[]{matcher};
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
                                            .match("public", OpcodeUtil.isPublic(var))
                                            .match("protected", OpcodeUtil.isProtected(var))
                                            .match("private", OpcodeUtil.isPrivate(var))
                                            .check();

                                    return initialMatch && regex.matcher(var.getDisplayName()).matches();
                                }
                            });
                        }

                        case METHOD: {
                            final String clazz = parsed.split("#")[0];
                            final String method = parsed.split("#")[1];

                            final Pattern regexClazz = Pattern.compile(clazz);
                            final Pattern regexMethod = Pattern.compile(method);

                            map.put(type, new ExclusionTester<MethodNode>() {
                                @Override
                                public boolean test(MethodNode var) {
                                    final boolean initialMatch = Match
                                            .of(matcher)
                                            .match("static", var.isStatic())
                                            .match("public", OpcodeUtil.isPublic(var))
                                            .match("protected", OpcodeUtil.isProtected(var))
                                            .match("private", OpcodeUtil.isPrivate(var))
                                            .check();

                                    return initialMatch
                                            && regexMethod.matcher(var.getDisplayName()).matches()
                                            && regexClazz.matcher(var.owner.getDisplayName()).matches();
                                }
                            });
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
                                            .match("static", OpcodeUtil.isStatic(var))
                                            .match("public", OpcodeUtil.isPublic(var))
                                            .match("protected", OpcodeUtil.isProtected(var))
                                            .match("private", OpcodeUtil.isPrivate(var))
                                            .check();

                                    return initialMatch
                                            && regexField.matcher(var.getDisplayName()).matches()
                                            && regexClazz.matcher(var.owner.getDisplayName()).matches();
                                }
                            });
                        }
                    }

                    padded = new StringBuilder();
                } else {
                    padded.append(c);
                }
            }

            index++;
        }

        return new Exclusion(map);
    }
}
