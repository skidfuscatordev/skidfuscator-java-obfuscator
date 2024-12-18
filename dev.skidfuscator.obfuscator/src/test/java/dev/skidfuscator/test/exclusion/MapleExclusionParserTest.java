package dev.skidfuscator.test.exclusion;

import dev.skidfuscator.obfuscator.exempt.Exclusion;
import dev.skidfuscator.obfuscator.exempt.ExclusionTester;
import dev.skidfuscator.obfuscator.exempt.ExclusionType;
import dev.skidfuscator.obfuscator.exempt.v2.ExclusionParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mapleir.asm.ClassNode;
import org.mapleir.asm.MethodNode;
import org.mapleir.asm.FieldNode;

import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MapleExclusionParserTest {
    private ClassNode mockClassNode;
    private MethodNode mockMethodNode;
    private FieldNode mockFieldNode;

    @BeforeEach
    void setUp() {
        // Setup mock ClassNode
        mockClassNode = mock(ClassNode.class);
        when(mockClassNode.getName()).thenReturn("com/example/TestClass");
        when(mockClassNode.getSuperName()).thenReturn("com/example/BaseClass");
        when(mockClassNode.getInterfaces()).thenReturn(Arrays.asList("com/example/Interface1"));
        
        // Setup mock MethodNode
        mockMethodNode = mock(MethodNode.class);
        when(mockMethodNode.getName()).thenReturn("testMethod");
        when(mockMethodNode.getDesc()).thenReturn("()V");
        when(mockMethodNode.getOwnerClass()).thenReturn(mockClassNode);
        
        // Setup mock FieldNode
        mockFieldNode = mock(FieldNode.class);
        when(mockFieldNode.getDisplayName()).thenReturn("testField");
        when(mockFieldNode.getDesc()).thenReturn("Ljava/lang/String;");
        when(mockFieldNode.getOwnerClass()).thenReturn(mockClassNode);
    }

    @Test
    @DisplayName("Test basic class pattern parsing and matching")
    void testBasicClassPattern() {
        String input = """
            @class public com.example.TestClass {
                @method getName
                @field name
            }
            """;
        
        when(mockClassNode.isPublic()).thenReturn(true);
        
        Exclusion exclusion = ExclusionParser.parsePatternExclusion(input);
        ExclusionTester<ClassNode> classTester = exclusion.getTesters().poll(ExclusionType.CLASS);
        
        assertTrue(classTester.test(mockClassNode));
    }

    @Test
    @DisplayName("Test interface pattern with extends")
    void testInterfacePattern() {
        String input = """
            @interface com.example.TestInterface extends BaseInterface {
                @method #void process()
            }
            """;
        
        when(mockClassNode.isInterface()).thenReturn(true);
        when(mockClassNode.getName()).thenReturn("com/example/TestInterface");
        when(mockClassNode.getSuperName()).thenReturn("com/example/BaseInterface");
        
        Exclusion exclusion = ExclusionParser.parsePatternExclusion(input);
        ExclusionTester<ClassNode> classTester = exclusion.getTesters().poll(ExclusionType.CLASS);
        
        assertTrue(classTester.test(mockClassNode));
    }

    @Test
    @DisplayName("Test method modifiers and return type")
    void testMethodModifiers() {
        String input = """
            @class com.example.TestClass {
                @method public static #void testMethod()
            }
            """;
        
        when(mockMethodNode.isPublic()).thenReturn(true);
        when(mockMethodNode.isStatic()).thenReturn(true);

        System.out.println(mockMethodNode.getName() + " " + mockMethodNode.getDesc() + " " + mockMethodNode.isPublic());
        System.out.println(mockClassNode.getName() + " " + mockClassNode.getSuperName() + " " + mockClassNode.getInterfaces());
        Exclusion exclusion = ExclusionParser.parsePatternExclusion(input);
        System.out.println(exclusion);
        assertTrue(exclusion.test(mockMethodNode));
    }

    @Test
    @DisplayName("Test field modifiers and type")
    void testFieldModifiers() {
        String input = """
            @class com.example.TestClass {
                @field private static final #String testField
            }
            """;
        
        when(mockFieldNode.isPrivate()).thenReturn(true);
        when(mockFieldNode.isStatic()).thenReturn(true);
        when(mockFieldNode.isFinal()).thenReturn(true);
        
        Exclusion exclusion = ExclusionParser.parsePatternExclusion(input);
        assertTrue(exclusion.test(mockFieldNode));
    }

    @Test
    @DisplayName("Test wildcard pattern matching")
    void testWildcardPatterns() {
        String input = """
            @class com.example.* {
                @method get*
                @field *Data
            }
            """;
        
        when(mockMethodNode.getName()).thenReturn("getData");
        when(mockFieldNode.getDisplayName()).thenReturn("userDatav");
        
        Exclusion exclusion = ExclusionParser.parsePatternExclusion(input);
        ExclusionTester<MethodNode> methodTester = exclusion.getTesters().poll(ExclusionType.METHOD);
        ExclusionTester<FieldNode> fieldTester = exclusion.getTesters().poll(ExclusionType.FIELD);
        
        assertTrue(methodTester.test(mockMethodNode));
        assertTrue(fieldTester.test(mockFieldNode));
    }

    @ParameterizedTest
    @DisplayName("Test class type patterns")
    @MethodSource("provideClassTypePatterns")
    void testClassTypes(String classType, boolean isInterface, boolean isAnnotation, boolean isEnum) {
        String input = String.format("@%s com.example.Test { }", classType);
        
        when(mockClassNode.isInterface()).thenReturn(isInterface);
        when(mockClassNode.isAnnotation()).thenReturn(isAnnotation);
        when(mockClassNode.isEnum()).thenReturn(isEnum);
        
        Exclusion exclusion = ExclusionParser.parsePatternExclusion(input);
        ExclusionTester<ClassNode> classTester = exclusion.getTesters().poll(ExclusionType.CLASS);
        
        assertTrue(classTester.test(mockClassNode));
    }

    private static Stream<Arguments> provideClassTypePatterns() {
        return Stream.of(
            Arguments.of("class", false, false, false),
            Arguments.of("interface", true, false, false),
            Arguments.of("annotation", false, true, false),
            Arguments.of("enum", false, false, true)
        );
    }

    @Test
    @DisplayName("Test complex inheritance pattern")
    void testComplexInheritance() {
        String input = """
            @class com.example.TestClass extends BaseClass implements Interface1, Interface2 {
                @method test()
            }
            """;
        
        when(mockClassNode.getInterfaces()).thenReturn(Arrays.asList(
            "com/example/Interface1",
            "com/example/Interface2"
        ));
        
        Exclusion exclusion = ExclusionParser.parsePatternExclusion(input);
        ExclusionTester<ClassNode> classTester = exclusion.getTesters().poll(ExclusionType.CLASS);
        
        assertTrue(classTester.test(mockClassNode));
    }

    @Test
    @DisplayName("Test method parameter matching")
    void testMethodParameters() {
        String input = """
            @class com.example.TestClass {
                @method #void process(String, int)
            }
            """;
        
        when(mockMethodNode.getName()).thenReturn("process");
        when(mockMethodNode.getDesc()).thenReturn("(Ljava/lang/String;I)V");
        
        Exclusion exclusion = ExclusionParser.parsePatternExclusion(input);
        assertTrue(exclusion.test(mockMethodNode));
    }

    @Test
    @DisplayName("Test invalid pattern handling")
    void testInvalidPatterns() {
        assertThrows(ExclusionParser.ExclusionParseException.class, () ->
            ExclusionParser.parsePattern("@invalid com.example.Test { }"));
            
        assertThrows(ExclusionParser.ExclusionParseException.class, () ->
            ExclusionParser.parsePattern("@class { }"));
            
        assertThrows(ExclusionParser.ExclusionParseException.class, () -> {
            Exclusion exclusion = ExclusionParser.parsePatternExclusion("@class com.example.Test { @invalid test }");
            exclusion.getTesters().poll(ExclusionType.METHOD).toString();
            System.out.println(exclusion);
        });
    }

    @Test
    @DisplayName("Test multiple method/field patterns")
    void testMultiplePatterns() {
        String input = """
            @class com.example.TestClass {
                @method get*
                @method set*
                @field *Data
                @field *Count
            }
            """;
        
        MethodNode getMethod = mock(MethodNode.class);
        when(getMethod.getName()).thenReturn("getName");
        
        MethodNode setMethod = mock(MethodNode.class);
        when(setMethod.getName()).thenReturn("setName");
        
        FieldNode dataField = mock(FieldNode.class);
        when(dataField.getDisplayName()).thenReturn("userData");
        
        FieldNode countField = mock(FieldNode.class);
        when(countField.getDisplayName()).thenReturn("userCount");
        
        Exclusion exclusion = ExclusionParser.parsePatternExclusion(input);
        ExclusionTester<MethodNode> methodTester = exclusion.getTesters().poll(ExclusionType.METHOD);
        ExclusionTester<FieldNode> fieldTester = exclusion.getTesters().poll(ExclusionType.FIELD);
        
        assertTrue(methodTester.test(getMethod));
        assertTrue(methodTester.test(setMethod));
        assertTrue(fieldTester.test(dataField));
        assertTrue(fieldTester.test(countField));
    }
}