package dev.skidfuscator.test.exclusion;

import dev.skidfuscator.obfuscator.exempt.Exclusion;
import dev.skidfuscator.obfuscator.exempt.SimpleExemptAnalysis;
import dev.skidfuscator.obfuscator.exempt.v2.ExclusionParser;
import dev.skidfuscator.obfuscator.transform.Transformer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapleir.asm.ClassNode;
import org.mapleir.asm.MethodNode;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NestedExclusionTest {
    private SimpleExemptAnalysis analysis;
    private Exclusion exclusion;
    private ClassNode mockServiceClass;
    private ClassNode mockInternalClass;
    private ClassNode mockCustomService;
    private ClassNode mockOtherService;

    @BeforeEach
    void setUp() {
        // Initialize the exempt analysis
        analysis = new SimpleExemptAnalysis(null);
        
        // Parse and add the exclusion pattern
        String pattern = """
            @class com.example.service.* {
                !@class internal.*
                !@class CustomService
            }
            """;
        exclusion = ExclusionParser.parsePatternExclusion(pattern);
        analysis.add(exclusion);

        // Setup mock classes
        mockServiceClass = mock(ClassNode.class);
        when(mockServiceClass.getName()).thenReturn("com/example/service/UserService");

        mockInternalClass = mock(ClassNode.class);
        when(mockInternalClass.getName()).thenReturn("com/example/service/internal/InternalService");

        mockCustomService = mock(ClassNode.class);
        when(mockCustomService.getName()).thenReturn("com/example/service/CustomService");

        mockOtherService = mock(ClassNode.class);
        when(mockOtherService.getName()).thenReturn("com/example/other/OtherService");
    }

    @Test
    @DisplayName("Test general service package exclusion")
    void testServicePackageExclusion() {
        assertTrue(analysis.isExempt(mockServiceClass), 
            "Classes in service package should be excluded");
    }

    @Test
    @DisplayName("Test internal package inclusion")
    void testInternalPackageInclusion() {
        assertFalse(analysis.isExempt(mockInternalClass), 
            "Classes in internal package should be included");
    }

    @Test
    @DisplayName("Test CustomService inclusion")
    void testCustomServiceInclusion() {
        assertFalse(analysis.isExempt(mockCustomService), 
            "CustomService should be included");
    }

    @Test
    @DisplayName("Test other package non-exclusion")
    void testOtherPackageNonExclusion() {
        assertFalse(analysis.isExempt(mockOtherService), 
            "Classes outside service package should not be excluded");
    }

    @Test
    @DisplayName("Test method exclusions follow class rules")
    void testMethodExclusions() {
        // Create mock methods
        MethodNode serviceMethod = mock(MethodNode.class);
        when(serviceMethod.getOwnerClass()).thenReturn(mockServiceClass);

        MethodNode internalMethod = mock(MethodNode.class);
        when(internalMethod.getOwnerClass()).thenReturn(mockInternalClass);

        MethodNode customServiceMethod = mock(MethodNode.class);
        when(customServiceMethod.getOwnerClass()).thenReturn(mockCustomService);

        // Test method exclusions
        assertTrue(analysis.isExempt(serviceMethod),
            "Methods in service package should be excluded");

        System.out.println(internalMethod.getOwnerClass().getName());
        System.out.println(exclusion);
        assertFalse(analysis.isExempt(internalMethod),
            "Methods in internal package should be included");
        
        assertFalse(analysis.isExempt(customServiceMethod),
            "Methods in CustomService should be included");
    }

    @Test
    @DisplayName("Test cached results remain consistent")
    void testCachedResults() {
        // First check
        boolean firstServiceCheck = analysis.isExempt(mockServiceClass);
        boolean firstInternalCheck = analysis.isExempt(mockInternalClass);
        boolean firstCustomCheck = analysis.isExempt(mockCustomService);

        // Second check (should use cache)
        boolean secondServiceCheck = analysis.isExempt(mockServiceClass);
        boolean secondInternalCheck = analysis.isExempt(mockInternalClass);
        boolean secondCustomCheck = analysis.isExempt(mockCustomService);

        // Assert consistency
        assertEquals(firstServiceCheck, secondServiceCheck,
            "Cached service class result should be consistent");
        assertEquals(firstInternalCheck, secondInternalCheck,
            "Cached internal class result should be consistent");
        assertEquals(firstCustomCheck, secondCustomCheck,
            "Cached CustomService result should be consistent");
    }

    @Test
    @DisplayName("Test direct method addition respects rules")
    void testDirectMethodAddition() {
        // Create methods
        MethodNode serviceMethod = mock(MethodNode.class);
        when(serviceMethod.getOwnerClass()).thenReturn(mockServiceClass);

        MethodNode internalMethod = mock(MethodNode.class);
        when(internalMethod.getOwnerClass()).thenReturn(mockInternalClass);

        // Add methods directly
        analysis.add(serviceMethod);
        //analysis.add(internalMethod);

        // Verify they follow exclusion rules
        assertTrue(analysis.isExempt(serviceMethod),
            "Directly added service method should be excluded");
        assertFalse(analysis.isExempt(internalMethod),
            "Directly added internal method should be included");
    }

    @Test
    @DisplayName("Test nested inclusion/exclusion rules")
    void testNestedInclusionExclusion() {
        String pattern = """
        @class com.example.service.* {
            !@class internal.*
            !@class CustomService
        }
        """;

        Exclusion exclusion = ExclusionParser.parsePatternExclusion(pattern);
        System.out.println(exclusion);
        SimpleExemptAnalysis analysis = new SimpleExemptAnalysis(null);
        analysis.add(exclusion);

        // Test regular service class (should be excluded)
        ClassNode regularService = mock(ClassNode.class);
        when(regularService.getName()).thenReturn("com/example/service/RegularService");
        assertTrue(analysis.isExempt(regularService));

        // Test internal service class (should be included)
        ClassNode internalService = mock(ClassNode.class);
        when(internalService.getName()).thenReturn("com/example/service/internal/InternalService");
        assertFalse(analysis.isExempt(internalService));

        // Test CustomService (should be included)
        ClassNode customService = mock(ClassNode.class);
        when(customService.getName()).thenReturn("com/example/service/CustomService");
        assertFalse(analysis.isExempt(customService));
    }

    @Test
    @DisplayName("Test direct inclusion rule")
    void testDirectInclusion() {
        String pattern = "!@class com.example.include.*";

        Exclusion exclusion = ExclusionParser.parsePatternExclusion(pattern);
        SimpleExemptAnalysis analysis = new SimpleExemptAnalysis(null);
        analysis.add(exclusion);

        ClassNode includedClass = mock(ClassNode.class);
        when(includedClass.getName()).thenReturn("com/example/include/TestClass");
        assertFalse(analysis.isExempt(includedClass));
    }
}