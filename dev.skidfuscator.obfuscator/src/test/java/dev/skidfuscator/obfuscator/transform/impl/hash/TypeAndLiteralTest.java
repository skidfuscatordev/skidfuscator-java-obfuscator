package dev.skidfuscator.obfuscator.transform.impl.hash;

import com.typesafe.config.ConfigFactory;
import dev.skidfuscator.config.DefaultSkidConfig;
import dev.skidfuscator.obfuscator.*;
import dev.skidfuscator.obfuscator.reliability.Profiles;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import sdk.SDK;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TypeAndLiteralTest {
    @Test void typeChecksPreserveClassLoaderIdentity() throws Exception {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V1_8,Opcodes.ACC_PUBLIC,"probe/Identity",null,"java/lang/Object",null);
        var init = writer.visitMethod(Opcodes.ACC_PUBLIC,"<init>","()V",null,null);
        init.visitCode(); init.visitVarInsn(Opcodes.ALOAD,0); init.visitMethodInsn(Opcodes.INVOKESPECIAL,"java/lang/Object","<init>","()V",false);
        init.visitInsn(Opcodes.RETURN); init.visitMaxs(1,1); init.visitEnd(); writer.visitEnd();
        byte[] bytes = writer.toByteArray();
        class Loader extends ClassLoader { Class<?> define() { return defineClass("probe.Identity",bytes,0,bytes.length); } }
        Class<?> first = new Loader().define(), second = new Loader().define();
        Object value = second.getDeclaredConstructor().newInstance();
        assertFalse(SDK.checkType(value,first));
        assertTrue(SDK.checkType(value,second));
        assertFalse(SDK.checkType(value,first));
    }
    @Test void typeChecksRetainArrayInterfaceAndNullBehaviorAcrossRepeatedCalls() {
        for (int i=0;i<100;i++) {
            assertTrue(SDK.checkType(new String[2][],Object[].class));
            assertTrue(SDK.checkType(new int[2],Cloneable.class));
            assertFalse(SDK.checkType(new int[2],Object[].class));
            assertTrue(SDK.checkType(new ArrayList<>(),List.class));
            assertFalse(SDK.checkType(null,Object.class));
        }
    }
    @Test void bothTypeConfigurationNamesDisableTheActualPass() {
        for (String name : new String[]{"instanceOfHash","typeCheck"}) {
            Skidfuscator owner = mock(Skidfuscator.class);
            when(owner.getTsConfig()).thenReturn(Profiles.apply(ConfigFactory.parseString(name+".enabled=false"),ReliabilityOptions.Profile.CONSERVATIVE));
            when(owner.getConfig()).thenReturn(new DefaultSkidConfig(ConfigFactory.empty(),""));
            assertFalse(new InstanceOfHashTransformer(owner).isEnabled(),name);
        }
    }
    @Test void hotLiteralReusesItsDecodedValueAndCacheChurnDoesNotChangeSemantics() {
        String encoded = SDK.maskComparisonLiteral("literal",42);
        String first = SDK.comparisonLiteral(encoded,42);
        assertSame(first,SDK.comparisonLiteral(encoded,42));
        for (int i=0;i<5000;i++) {
            String value = "value-"+i; long key = i*31L;
            assertEquals(value,SDK.comparisonLiteral(SDK.maskComparisonLiteral(value,key),key));
        }
        assertEquals("literal",SDK.comparisonLiteral(encoded,42));
        assertEquals(SDK.maskComparisonLiteral(encoded,43),SDK.comparisonLiteral(encoded,43));
        String large = "x".repeat(2048);
        assertEquals(large,SDK.comparisonLiteral(SDK.maskComparisonLiteral(large,42),42));
    }
    @Test void concurrentLiteralCacheCollisionsAreSafe() throws Exception {
        ExecutorService workers = Executors.newFixedThreadPool(4);
        try {
            List<Callable<Void>> tasks = new ArrayList<>();
            for (int worker=0;worker<4;worker++) {
                final int id = worker;
                tasks.add(() -> {
                    for (int i=0;i<3000;i++) {
                        long key = (long)i*17+id; String value = "thread-"+id+"-"+i;
                        String encoded = SDK.maskComparisonLiteral(value,key);
                        assertTrue(SDK.equalsConstantReceiver(encoded,value,key));
                        assertFalse(SDK.equalsConstantReceiver(encoded,value+"!",key));
                    }
                    return null;
                });
            }
            for (Future<Void> task : workers.invokeAll(tasks)) task.get();
        } finally { workers.shutdownNow(); }
    }
}
