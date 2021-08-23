package dev.skidfuscator.obf.yggdrasil.app;

import dev.skidfuscator.obf.init.SkidSession;
import dev.skidfuscator.obf.yggdrasil.EntryPoint;
import dev.skidfuscator.obf.yggdrasil.method.MethodInvokerResolver;
import org.mapleir.app.service.ApplicationClassSource;
import org.mapleir.app.service.InvocationResolver;
import org.mapleir.asm.MethodNode;
import org.mapleir.context.AnalysisContext;
import java.util.*;

/**
 * @author Ghast
 * @since 21/01/2021
 * SkidfuscatorV2 © 2021
 */
public class ApplicationEntryPoint implements EntryPoint {
    public List<MethodNode> getEntryPoints(final SkidSession context, final ApplicationClassSource contents) {
        final AnalysisContext cxt = context.getCxt();
        final InvocationResolver resolver = cxt.getInvocationResolver();
        final MethodInvokerResolver methodInvokerResolver = context.getMethodInvokerResolver();

        final List<MethodNode> free = methodInvokerResolver.getNeverCalled();
        return new ArrayList<>(free);
    }

}
