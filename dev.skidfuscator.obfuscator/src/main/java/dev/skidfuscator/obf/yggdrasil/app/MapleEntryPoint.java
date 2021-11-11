package dev.skidfuscator.obf.yggdrasil.app;

import dev.skidfuscator.obf.init.SkidSession;
import dev.skidfuscator.obf.yggdrasil.EntryPoint;
import dev.skidfuscator.obf.yggdrasil.method.MethodInvokerResolver;
import org.mapleir.app.service.ApplicationClassSource;
import org.mapleir.app.service.InvocationResolver;
import org.mapleir.asm.MethodNode;
import org.mapleir.context.AnalysisContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author Ghast
 * @since 21/01/2021
 * SkidfuscatorV2 © 2021
 */
public class MapleEntryPoint implements EntryPoint {
    public List<MethodNode> getEntryPoints(final SkidSession context, final ApplicationClassSource contents) {
        final AnalysisContext cxt = context.getCxt();
        final Set<MethodNode> free = context.getCxt().getApplicationContext().getEntryPoints();
        return new ArrayList<>(free);
    }

}
