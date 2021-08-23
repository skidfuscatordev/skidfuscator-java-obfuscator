package dev.skidfuscator.obf.yggdrasil;

import dev.skidfuscator.obf.init.SkidSession;
import org.mapleir.app.service.ApplicationClassSource;
import org.mapleir.asm.ClassNode;
import org.mapleir.asm.MethodNode;
import org.mapleir.deob.PassContext;
import org.topdank.byteio.in.SingleJarDownloader;

import java.util.List;

public interface EntryPoint {
    List<MethodNode> getEntryPoints(final SkidSession context, final ApplicationClassSource contents);
}
