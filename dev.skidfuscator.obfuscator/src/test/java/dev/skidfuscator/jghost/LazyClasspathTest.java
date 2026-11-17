package dev.skidfuscator.jghost;

import dev.skidfuscator.jghost.tree.GhostClassNode;
import org.junit.jupiter.api.Test;
import org.mapleir.app.service.*;
import org.mapleir.asm.ClassHelper;
import org.mapleir.asm.ClassNode;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class LazyClasspathTest {
    ClassNode node(String name) {
        var value = new org.objectweb.asm.tree.ClassNode(); value.name=name; value.superName="java/lang/Object";
        return ClassHelper.create(value);
    }
    @Test void indexingDoesNotMaterializeMetadataAndEachLookupRetainsIdentity() {
        int[] reads = {0};
        GhostClassNode metadata = new GhostClassNode() {
            public String getName() { return "Example"; }
            public org.objectweb.asm.tree.ClassNode read() { reads[0]++; return node("Example").node; }
        };
        var lazy = new LazyClassMap(Collections.singleton(metadata));
        var library = new ApplicationClassSource("lib",false,lazy);
        var application = new ApplicationClassSource("app",false,Collections.emptyList());
        application.addLibraries(new LibraryClassSource(library,5));
        assertTrue(application.getDuplicateClasses().isEmpty()); assertEquals(0,reads[0]);
        var first = application.findClassNode("Example");
        assertSame(first,application.findClassNode("Example")); assertEquals(1,reads[0]);
    }
    @Test void duplicatePrecedenceIsStableAndUpdatesWhenLibrariesAreAdded() {
        var app = new ApplicationClassSource("app",false,Collections.singleton(node("Own")));
        var one = new ApplicationClassSource("one",false,Arrays.asList(node("Shared"),node("Own")));
        var two = new ApplicationClassSource("two",false,Collections.singleton(node("Shared")));
        app.addLibraries(new LibraryClassSource(one,5)); app.addLibraries(new LibraryClassSource(two,5));
        assertSame(one.findClassNode("Shared"),app.findClassNode("Shared"));
        assertEquals(Arrays.asList("one","two"),app.getDuplicateClasses().get("Shared"));
        assertEquals(Arrays.asList("app","one"),app.getDuplicateClasses().get("Own"));
        var highest = new ApplicationClassSource("highest",false,Collections.singleton(node("Shared")));
        app.addLibraries(new LibraryClassSource(highest,10));
        assertSame(highest.findClassNode("Shared"),app.findClassNode("Shared"));
        assertNull(app.findClassNode("Missing"));
        app.add(node("Missing")); assertNotNull(app.findClassNode("Missing"));
    }
}
