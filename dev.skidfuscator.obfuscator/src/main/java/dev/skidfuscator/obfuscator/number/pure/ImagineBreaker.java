package dev.skidfuscator.obfuscator.number.pure;

import jdk.internal.reflect.Reflection;
import sun.misc.Unsafe;

import java.lang.Module;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.lang.ref.SoftReference;
import java.lang.reflect.Field;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @since 2.0
 * No natives required, no starting arguments required.
 * This is achieved thanks to jdk.internal.reflect.Reflection class not banning methods
 * from being reflectively invoked. With this, we can gain a lot of ground.
 * Unfortunately, it is likely that this will be patched very soon.
 * Extensive testing is provided for nearly all major jdk versions and vendors.
 */
public final class ImagineBreaker {

    public static final boolean IS_OPEN_J9 = isOpenJ9();

    private static final Unsafe UNSAFE = retrieveUnsafe();
    private static final Lookup LOOKUP = retrieveLookup();
    private static final MethodHandle MODULE$ADD_EXPORTS_TO_ALL_0 = retrieveAddExportsToAll0();

    private static final Set<Module> EVERYONE_MODULE_SET = retrieveEveryoneSet();
    private static final VarHandle MODULE$OPEN_PACKAGES = retrieveOpenPackagesHandle();
    private static final VarHandle CLASS$MODULE = retrieveModuleHandle();
    private static final VarHandle REFLECTION$FIELD_FILTER_MAP = retrieveFieldFilterMap();
    private static final VarHandle REFLECTION$METHOD_FILTER_MAP = retrieveMethodFilterMap();
    private static final VarHandle CLASS$REFLECTION_DATA = retrieveReflectionData();

    /**
     * Accessor to sun.misc.Unsafe.
     *
     * @return instance of sun.misc.Unsafe.
     */
    public static Unsafe unsafe() {
        return UNSAFE;
    }

    /**
     * Accessor to the trusted MethodHandles$Lookup.
     *
     * @return instance of the trusted lookup.
     */
    public static Lookup lookup() {
        return LOOKUP;
    }

    /**
     * Opens all modules within the boot ModuleLayer.
     */
    public static void openBootModules() {
        openModuleLayer(ModuleLayer.boot());
    }

    public static void openBootModule(String bootModule) {
        openModule(ModuleLayer.boot().findModule(bootModule).orElseThrow());
    }

    /**
     * Opens all modules within the specified ModuleLayer
     *
     * @param layer module layer to have all of its modules opened
     */
    public static void openModuleLayer(ModuleLayer layer) {
        layer.modules().forEach(ImagineBreaker::openModule);
    }

    /**
     * Opens a specific module
     *
     * @param module module to be opened
     */
    public static void openModule(Module module) {
        MODULE$OPEN_PACKAGES.set(module, WorldRejector.INSTANCE);
        for (String pkg : module.getPackages()) {
            try {
                MODULE$ADD_EXPORTS_TO_ALL_0.invokeExact(module, pkg);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Disguises a class as having a module of a different class.
     * Extremely useful when invoking caller-sensitive methods.
     *
     * <p>Consider {@link ImagineBreaker#disguiseAsModule(Class, Class, Runnable)}
     *    if you want to revert the disguise. Use the runnable to run actions before the reversion</p>
     *
     * @param target      target class to have its module changed
     * @param moduleClass class to have its module queried
     */
    public static void disguiseAsModule(Class<?> target, Class<?> moduleClass) {
        Object module = CLASS$MODULE.get(moduleClass);
        CLASS$MODULE.set(target, module);
    }

    /**
     * Disguises a class as having a different module.
     * Extremely useful when invoking caller-sensitive methods.
     *
     * <p>Consider {@link ImagineBreaker#disguiseAsModule(Class, Module, Runnable)}
     *    if you want to revert the disguise. Use the runnable to run actions before the reversion</p>
     *
     * @param target target class to have its module changed
     * @param module module for the target class
     */
    public static void disguiseAsModule(Class<?> target, Module module) {
        CLASS$MODULE.set(target, module);
    }

    /**
     * Disguises a class as having a different module.
     * Extremely useful when invoking caller-sensitive methods.
     * This method allows reversion of module for the target class after the runnable is ran.
     *
     * @param target      target class to have its module changed
     * @param moduleClass module for the target class
     * @param runnable    runnable to run before the class is reverted to having its module changed
     */
    public static void disguiseAsModule(Class<?> target, Class<?> moduleClass, Runnable runnable) {
        Object old = CLASS$MODULE.get(target);
        disguiseAsModule(target, moduleClass);
        runnable.run();
        CLASS$MODULE.set(target, old);
    }

    /**
     * Disguises a class as having a different module.
     * Extremely useful when invoking caller-sensitive methods.
     * This method allows reversion of module for the target class after the runnable is ran.
     *
     * @param target   target class to have its module changed
     * @param module   module for the target class
     * @param runnable runnable to run before the class is reverted to having its module changed
     */
    public static void disguiseAsModule(Class<?> target, Module module, Runnable runnable) {
        Object old = CLASS$MODULE.get(target);
        disguiseAsModule(target, module);
        runnable.run();
        CLASS$MODULE.set(target, old);
    }

    /**
     * Wipes {@link jdk.internal.reflect.Reflection#fieldFilterMap} as well as the
     * {@link java.lang.Class#reflectionData} member in their respective classes, though this is not done for OpenJ9.
     * This method should be called first to eliminate the default filters
     * but since the method to register field filters is copy-on-write
     * new filters may be added after-the-fact, meaning this method needs to be called again.
     */
    public static void wipeFieldFilters() {
        if (!IS_OPEN_J9) {
            for (Class clazz : ((Map<Class, Set>) REFLECTION$FIELD_FILTER_MAP.get()).keySet()) {
                CLASS$REFLECTION_DATA.setVolatile(clazz, null);
            }
        }
        REFLECTION$FIELD_FILTER_MAP.setVolatile(new HashMap<>());
    }

    /**
     * Wipes {@link jdk.internal.reflect.Reflection#methodFilterMap} as well as the
     * {@link java.lang.Class#reflectionData} member in their respective classes, though this is not done for OpenJ9.
     * This method should be called first to eliminate the default filters
     * but since the method to register method filters is copy-on-write
     * new filters may be added after-the-fact, meaning this method needs to be called again.
     */
    public static void wipeMethodFilters() {
        if (!IS_OPEN_J9) {
            for (Class clazz : ((Map<Class, Set>) REFLECTION$METHOD_FILTER_MAP.get()).keySet()) {
                CLASS$REFLECTION_DATA.setVolatile(clazz, null);
            }
        }
        REFLECTION$METHOD_FILTER_MAP.setVolatile(new HashMap<>());
    }

    private static boolean isOpenJ9() {
        return "Eclipse OpenJ9".equals(System.getProperty("java.vm.vendor"));
    }

    private static Unsafe retrieveUnsafe() {
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            return (Unsafe) theUnsafe.get(null);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    private static Lookup retrieveLookup() {
        Field methodHandles$lookup$implLookup = retrieveImplLookup();
        long offset = UNSAFE.staticFieldOffset(methodHandles$lookup$implLookup);
        return (Lookup) UNSAFE.getObject(Lookup.class, offset);
    }

    private static Field retrieveImplLookup() {
        try {
            return Lookup.class.getDeclaredField("IMPL_LOOKUP");
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    private static MethodHandle retrieveAddExportsToAll0() {
        try {
            return LOOKUP.findStatic(Module.class, "addExportsToAll0", MethodType.methodType(void.class, Module.class, String.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static Set<Module> retrieveEveryoneSet() {
        try {
            return (Set<Module>) LOOKUP.findStaticVarHandle(Module.class, "EVERYONE_SET", Set.class).get();
        } catch (IllegalAccessException e1) {
            if (e1.getMessage().endsWith("Expected static field.")) {
                try {
                    return (Set<Module>) mockLookup(Module.class).findStaticVarHandle(Module.class, "EVERYONE_SET", Set.class).get();
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            }
            throw new RuntimeException(e1);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    private static VarHandle retrieveOpenPackagesHandle() {
        try {
            return LOOKUP.findVarHandle(Module.class, "openPackages", Map.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static VarHandle retrieveModuleHandle() {
        try {
            return LOOKUP.findVarHandle(Class.class, "module", Module.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static VarHandle retrieveFieldFilterMap() {
        try {
            openBootModule("java.base");
            return LOOKUP.findStaticVarHandle(Reflection.class, "fieldFilterMap", Map.class);
        } catch (IllegalAccessException e1) {
            if (e1.getMessage().endsWith("Expected static field.")) {
                try {
                    return mockLookup(Reflection.class).findStaticVarHandle(Reflection.class, "fieldFilterMap", Map.class);
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            }
            throw new RuntimeException(e1);
        } catch (NoSuchFieldException e2) {
            throw new RuntimeException(e2);
        }
    }

    private static VarHandle retrieveMethodFilterMap() {
        try {
            openBootModule("java.base");
            return LOOKUP.findStaticVarHandle(Reflection.class, "methodFilterMap", Map.class);
        } catch (IllegalAccessException e1) {
            if (e1.getMessage().endsWith("Expected static field.")) {
                try {
                    return mockLookup(Reflection.class).findStaticVarHandle(Reflection.class, "methodFilterMap", Map.class);
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            }
            throw new RuntimeException(e1);
        } catch (NoSuchFieldException e2) {
            throw new RuntimeException(e2);
        }
    }

    private static VarHandle retrieveReflectionData() {
        try {
            return LOOKUP.findVarHandle(Class.class, "reflectionData", SoftReference.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            if (!IS_OPEN_J9) {
                throw new RuntimeException(e);
            }
            return null;
        }
    }

    // Extremely crude hack, used for OpenJ9 9 - 15, where nothing ever made sense
    private static Lookup mockLookup(Class<?> mockClass) throws Throwable {
        MethodHandle lookup$ctor = LOOKUP.findConstructor(Lookup.class,
                                                          MethodType.methodType(void.class, Class.class, Class.class, int.class, boolean.class));
        return (Lookup) lookup$ctor.invokeExact(mockClass, (Class) null, 31, false); // Magic number
    }

    private ImagineBreaker() { }

    private static class WorldRejector extends AbstractMap<String, Set<Module>> {

        private static final WorldRejector INSTANCE = new WorldRejector();

        @Override
        public Set<Module> get(Object key) {
            return EVERYONE_MODULE_SET;
        }

        @Override
        public Set<Entry<String, Set<Module>>> entrySet() {
            return Collections.emptySet();
        }

    }

}