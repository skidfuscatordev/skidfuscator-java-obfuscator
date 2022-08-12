package dev.skidfuscator.test.ssvm;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.test.util.VmUtil;
import dev.xdark.ssvm.VirtualMachine;
import dev.xdark.ssvm.fs.FileDescriptorManager;
import dev.xdark.ssvm.fs.HostFileDescriptorManager;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.util.Random;

/**
 * Virtual machine that can pull classes from a {@link Skidfuscator} and perform optimizations on executed bytecode.
 *
 * @author Matt Coley
 */
public abstract class IntegratedVirtualMachine extends VirtualMachine {
	private static final int RECAF_LIVE_ZIP_HANDLE = new Random().nextInt();
	private static final Logger logger = LogManager.getLogger(IntegratedVirtualMachine.class);
	private final VmUtil vmUtil;

	public IntegratedVirtualMachine() {
		vmUtil = VmUtil.create(this);
	}

	/**
	 * Exists so that the {@link Skidfuscator} reference can be used in the constructor.
	 * Supplied via an outer class.
	 *
	 * @return Associated SSVM integration instance.
	 */
	protected abstract SsvmIntegration integration();

	/**
	 * @return VM's associated workspace.
	 */
	protected Skidfuscator getWorkspace() {
		return integration().getWorkspace();
	}

	/**
	 * @return VM utils.
	 */
	public final VmUtil getVmUtil() {
		return vmUtil;
	}

	@Override
	public void bootstrap() {
		super.bootstrap();
		// TODO: Replace workspace bootloader with fake classpath jar
		// VirtualMachineUtil.addUrl(this, RECAF_LIVE_ZIP);
	}

	@Override
	@SuppressWarnings("unchecked")
	protected FileDescriptorManager createFileDescriptorManager() {
		return new HostFileDescriptorManager();
	}
}
