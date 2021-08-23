package dev.skidfuscator.obf.transform_legacy.parameter.impl;

import dev.skidfuscator.obf.init.SkidSession;
import dev.skidfuscator.obf.transform_legacy.parameter.ParameterResolver;
import dev.skidfuscator.obf.transform_legacy.parameter.ParameterTransformer;

/**
 * @author Ghast
 * @since 09/03/2021
 * SkidfuscatorV2 © 2021
 */
public class ZelixParameterTransformer implements ParameterTransformer {
    @Override
    public ParameterResolver transform(SkidSession session) {
        // Todo
        final ZelixParameterResolver zelixParameterResolver = new ZelixParameterResolver(session);
        zelixParameterResolver.initVTables();
        //session.getEntryPoints().
        return null;
    }
}
