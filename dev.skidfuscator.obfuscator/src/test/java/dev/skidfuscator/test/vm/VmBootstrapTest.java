package dev.skidfuscator.test.vm;

import dev.skidfuscator.obfuscator.ssvm.JdkBootClassFinder;
import dev.skidfuscator.obfuscator.ssvm.JdkClassDefiner;
import dev.skidfuscator.obfuscator.util.JdkDownloader;
import dev.xdark.ssvm.VirtualMachine;
import dev.xdark.ssvm.api.MethodInvoker;
import dev.xdark.ssvm.classloading.*;
import dev.xdark.ssvm.execution.ExecutionContext;
import dev.xdark.ssvm.execution.Result;
import dev.xdark.ssvm.execution.VMException;
import dev.xdark.ssvm.mirror.type.InstanceClass;
import dev.xdark.ssvm.value.InstanceValue;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class VmBootstrapTest {
    @Test
    public void testBoot() throws IOException {
        VirtualMachine vm = new VirtualMachine() {
            @Override
            protected BootClassFinder createBootClassFinder() {
                try {
                    return new JdkBootClassFinder(RuntimeBootClassFinder.create());
                } catch (IOException e) {
                    throw new RuntimeException("Failed to create JDK class definer", e);
                }
            }
        };


        // Set JDK 17 specific properties
        vm.getProperties().put("java.version", "17.0.2");
        vm.getProperties().put("java.class.path", "");

        try {
            vm.initialize();
            InstanceClass jdk_internal_module_ModuleBootstrap = (InstanceClass) vm.findBootstrapClass("jdk/internal/module/ModuleBootstrap");
            vm.getInterface().setInvoker(
                    vm.getSymbols().java_lang_System(), "initPhase2", "(ZZ)I",
                    ctx -> {
                        ctx.setResult(0);
                        return Result.ABORT;
                    }
            );
            vm.getInterface().setInvoker(
                    jdk_internal_module_ModuleBootstrap, "boot", "()V",
                    MethodInvoker.noop()
            );
            vm.bootstrap();
        } catch (Exception ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof VMException) {
                InstanceValue oop = ((VMException) cause).getOop();
                if (oop.getJavaClass() == vm.getSymbols().java_lang_ExceptionInInitializerError()) {
                    oop = (InstanceValue) vm.getOperations().getReference(oop, "exception", "Ljava/lang/Throwable;");
                }
                cause.printStackTrace();
                vm.getOperations().toJavaException(oop).printStackTrace();
            } else {
                throw ex;
            }
        }
    }
}
