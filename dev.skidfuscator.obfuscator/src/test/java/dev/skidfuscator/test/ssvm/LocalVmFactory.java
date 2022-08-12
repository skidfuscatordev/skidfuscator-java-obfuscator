package dev.skidfuscator.test.ssvm;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.xdark.ssvm.VirtualMachine;
import dev.xdark.ssvm.api.MethodInvoker;
import dev.xdark.ssvm.api.VMInterface;
import dev.xdark.ssvm.classloading.BootClassLoader;
import dev.xdark.ssvm.classloading.CompositeBootClassLoader;
import dev.xdark.ssvm.classloading.RuntimeBootClassLoader;
import dev.xdark.ssvm.mirror.InstanceJavaClass;

import java.util.Arrays;

/**
 * Factory implementation that provides workspace class access via the VM bootloader.
 *
 * @author Matt Coley
 */
public class LocalVmFactory {
	private final Skidfuscator workspace;

	/**
	 * @param workspace
	 * 		Workspace to pull classes from.
	 */
	public LocalVmFactory(Skidfuscator workspace) {
		this.workspace = workspace;
	}

	public IntegratedVirtualMachine create(SsvmIntegration integration) {
		IntegratedVirtualMachine vm = new IntegratedVirtualMachine() {
			@Override
			protected SsvmIntegration integration() {
				return integration;
			}

			@Override
			protected BootClassLoader createBootClassLoader() {
				return new CompositeBootClassLoader(Arrays.asList(
						new SkidfuscatorBootClassLoader(workspace),
						RuntimeBootClassLoader.create()
				));
			}
		};
		vm.initialize();
		VMInterface vmi = vm.getInterface();
		InstanceJavaClass cl = (InstanceJavaClass) vm.findBootstrapClass("java/lang/Shutdown");
		vmi.setInvoker(cl, "beforeHalt", "()V", MethodInvoker.noop());
		vmi.setInvoker(cl, "halt0", "(I)V", MethodInvoker.noop());
		return vm;
	}
}