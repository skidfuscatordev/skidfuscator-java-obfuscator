package dev.skidfuscator.test;

import dev.skidfuscator.obfuscator.exempt.v2.ExclusionParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ExclusionParserTest {

    @Test
    @DisplayName("Test basic class pattern")
    void testBasicClassPattern() {
        String input = """
                @class public com.example.MyClass {
                    @method getName
                    @method setName
                    @field name
                }
                """;

        List<String> results = ExclusionParser.parsePattern(input);
        assertEquals(4, results.size(), "Should generate patterns for class and all members");
        assertTrue(results.stream().anyMatch(p -> p.contains("public") && p.contains("com\\/example\\/MyClass")));
    }

    @Test
    @DisplayName("Test interface with extends")
    void testInterfaceWithExtends() {
        String input = """
                @interface com.example.Service extends BaseService {
                    @method #void process()
                    @method #CompletableFuture<String> getAsync()
                }
                """;

        List<String> results = ExclusionParser.parsePattern(input);
        assertTrue(results.stream().anyMatch(p ->
                p.contains("interface") && p.contains("extends:BaseService")));
    }

    @Test
    @DisplayName("Test abstract class with implements")
    void testAbstractClassWithImplements() {
        String input = """
                @abstract com.example.AbstractController implements Serializable, Cloneable {
                    @method abstract #void init()
                    @method public #String getName()
                }
                """;

        List<String> results = ExclusionParser.parsePattern(input);
        assertTrue(results.stream().anyMatch(p ->
                p.contains("abstract") &&
                        p.contains("implements:Serializable") &&
                        p.contains("implements:Cloneable")));
    }

    @Test
    @DisplayName("Test method with generic types")
    void testMethodWithGenerics() {
        String input = """
                @class com.example.Repository {
                    @method public #List<String> findAll()
                    @method protected #Map<String, List<Integer>> getData()
                }
                """;

        List<String> results = ExclusionParser.parsePattern(input);
        assertTrue(results.stream().anyMatch(p -> p.contains("List<String>")));
        assertTrue(results.stream().anyMatch(p -> p.contains("Map<String, List<Integer>>")));
    }

    @Test
    @DisplayName("Test wildcard patterns")
    void testWildcardPatterns() {
        String input = """
            @class com.example.*.service.** {
                @method get*
                @method set*(#String)
                @field *Data
            }
            """;

        List<String> results = ExclusionParser.parsePattern(input);
        results.forEach(System.out::println);
        assertTrue(results.stream().anyMatch(p -> p.contains("com\\/example\\/.*\\/service\\/.*.*")));
        assertTrue(results.stream().anyMatch(p -> p.contains("get*")));
        assertTrue(results.stream().anyMatch(p -> p.contains("set*")));
        assertTrue(results.stream().anyMatch(p -> p.contains("*Data")));
    }

    @Test
    @DisplayName("Test annotation type")
    void testAnnotationType() {
        String input = """
            @annotation com.example.MyAnnotation {
                @method #String value()
                @method #Class<?>[] classes()
            }
            """;

        List<String> results = ExclusionParser.parsePattern(input);
        assertTrue(results.stream().anyMatch(p -> p.contains("annotation")));
        assertTrue(results.stream().anyMatch(p -> p.contains("#String") && p.contains("value")));
        assertTrue(results.stream().anyMatch(p -> p.contains("Class<?>[]") && p.contains("classes")));
    }

    @Test
    @DisplayName("Test all modifiers")
    void testAllModifiers() {
        String input = """
            @class public static final com.example.Constants {
                @method private static synchronized #void init()
                @field public static final transient volatile #int count
            }
            """;

        List<String> results = ExclusionParser.parsePattern(input);
        assertTrue(results.stream().anyMatch(p ->
                p.contains("public") && p.contains("static") && p.contains("final")));
        assertTrue(results.stream().anyMatch(p ->
                p.contains("private") && p.contains("static") && p.contains("synchronized")));
        assertTrue(results.stream().anyMatch(p ->
                p.contains("public") && p.contains("static") && p.contains("final") &&
                        p.contains("transient") && p.contains("volatile")));
    }

    @Test
    @DisplayName("Test method parameters")
    void testMethodParameters() {
        String input = """
            @class com.example.Service {
                @method #void process(String, int)
                @method #List<T> convert(T, Class<T>)
                @method #void complex(Map<String, List<Integer>>)
            }
            """;

        List<String> results = ExclusionParser.parsePattern(input);
        assertTrue(results.stream().anyMatch(p -> p.contains("(String, int)")));
        assertTrue(results.stream().anyMatch(p -> p.contains("(T, Class<T>)")));
        assertTrue(results.stream().anyMatch(p -> p.contains("(Map<String, List<Integer>>)")));
    }

    @Test
    @DisplayName("Test error handling - invalid class type")
    void testInvalidClassType() {
        String input = """
            @invalid com.example.MyClass {
                @method test()
            }
            """;

        ExclusionParser.ExclusionParseException exception = assertThrows(
                ExclusionParser.ExclusionParseException.class,
                () -> ExclusionParser.parsePattern(input)
        );
    }

    @Test
    @DisplayName("Test error handling - invalid member declaration")
    void testInvalidMemberDeclaration() {
        String input = """
            @class com.example.MyClass {
                @invalid test()
            }
            """;

        ExclusionParser.ExclusionParseException exception = assertThrows(
                ExclusionParser.ExclusionParseException.class,
                () -> ExclusionParser.parsePattern(input)
        );
    }

    @Test
    @DisplayName("Test error handling - missing class name")
    void testMissingClassName() {
        String input = """
            @class {
                @method test()
            }
            """;

        assertThrows(ExclusionParser.ExclusionParseException.class,
                () -> ExclusionParser.parsePattern(input));
    }

    @Test
    @DisplayName("Test nested generics")
    void testNestedGenerics() {
        String input = """
            @class com.example.Repository<T extends Entity> {
                @method #Optional<List<T>> findAll()
                @method #CompletableFuture<Map<String, List<T>>> getComplexData()
            }
            """;

        List<String> results = ExclusionParser.parsePattern(input);
        assertTrue(results.stream().anyMatch(p -> p.contains("Optional<List<T>>")));
        assertTrue(results.stream().anyMatch(p -> p.contains("CompletableFuture<Map<String, List<T>>>")));
    }

    @Test
    @DisplayName("Test array types")
    void testArrayTypes() {
        String input = """
            @class com.example.ArrayTest {
                @method #String[] getArray()
                @method #List<String>[] getArrayOfLists()
                @field #int[][] matrix
            }
            """;

        List<String> results = ExclusionParser.parsePattern(input);
        assertTrue(results.stream().anyMatch(p -> p.contains("String[]")));
        assertTrue(results.stream().anyMatch(p -> p.contains("List<String>[]")));
        assertTrue(results.stream().anyMatch(p -> p.contains("int[][]")));
    }
}