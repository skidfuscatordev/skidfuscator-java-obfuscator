package dev.skidfuscator.gradle;

import org.gradle.api.Action;
import org.gradle.api.Task;
import org.gradle.api.file.FileCollection;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

public class SkidfuscatorCompileAction implements Action<Task> {

    private final SkidfuscatorSpec spec;
    private final SkidfuscatorRuntime runtime;

    @Inject
    public SkidfuscatorCompileAction(SkidfuscatorSpec spec, SkidfuscatorRuntime runtime) {
        this.spec = spec;
        this.runtime = runtime;
    }

    @Override
    public void execute(Task task) {
        try {
            this.executeObfuscator();
        } catch (Throwable throwable) {
            throw new IllegalStateException("Failed to compile with skidfuscator", throwable);
        }
    }

    private void executeObfuscator() throws IOException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        FileCollection fileCollection = runtime.fetchClasspath();
        List<URL> urls = new ArrayList<>();
        for (File file : fileCollection)
            urls.add(file.toURI().toURL());

        try (URLClassLoader classLoader = new URLClassLoader(urls.toArray(new URL[0]))) {
            Class<?> sessionClass = classLoader.loadClass("dev.skidfuscator.obfuscator.SkidfuscatorSession");
            Class<?> obfuscatorClass = classLoader.loadClass("dev.skidfuscator.obfuscator.Skidfuscator");
            Object session = this.buildSkidfuscatorSession(sessionClass);
            Object obfuscator = obfuscatorClass.getDeclaredConstructor(sessionClass).newInstance(session);

            this.addToExemptAnalysis(obfuscator);

            // run obfuscator!
            Method run = obfuscator.getClass().getMethod("run");
            run.invoke(obfuscator);
        }
    }

    private void addToExemptAnalysis(Object obfuscator) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        Class<?> obfuscatorClass = obfuscator.getClass();
        Method getExemptAnalysis = obfuscatorClass.getMethod("getExemptAnalysis");
        Object exemptAnalysis = getExemptAnalysis.invoke(obfuscator);

        Method method = exemptAnalysis.getClass().getMethod("add", String.class);
        for (String exclude : this.spec.getExcludes())
            method.invoke(exemptAnalysis, exclude);
    }

    private Object buildSkidfuscatorSession(Class<?> aClass) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Constructor<?> constructor = aClass.getDeclaredConstructor(
                File.class, // input
                File.class, // output
                File[].class, // libs
                File.class, // mappings
                File.class, // exempt
                File.class, // runtime
                boolean.class, // phantom
                boolean.class, // jmod
                boolean.class, // fuckit
                boolean.class // analytics
        );
        return constructor.newInstance(
                spec.getInput(), spec.getOutput(), spec.getLibs(), spec.getMappings(), spec.getExempt(), spec.getRuntime(),
                spec.isPhantom(), spec.isJmod(), spec.isFuckit(), spec.isAnalytics());
    }
}
