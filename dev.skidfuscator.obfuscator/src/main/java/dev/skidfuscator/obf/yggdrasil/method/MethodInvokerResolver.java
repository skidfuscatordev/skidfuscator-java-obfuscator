package dev.skidfuscator.obf.yggdrasil.method;

import dev.skidfuscator.obf.yggdrasil.method.hash.InvokerHash;
import org.mapleir.asm.MethodNode;

import java.util.List;

public interface MethodInvokerResolver {
    List<MethodNode> getNeverCalled();
    List<InvokerHash> getCallers(MethodNode methodNode);
    List<InvokerHash> getCalled(MethodNode methodNode);
}
