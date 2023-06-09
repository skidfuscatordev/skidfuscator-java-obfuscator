package dev.skidfuscator.obfuscator.predicate.renderer.seed;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.predicate.opaque.BlockOpaquePredicate;
import dev.skidfuscator.obfuscator.predicate.renderer.seed.impl.ExceptionSeedLoaderRenderer;
import dev.skidfuscator.obfuscator.predicate.renderer.seed.impl.InternalSeedLoaderRenderer;
import dev.skidfuscator.obfuscator.predicate.renderer.seed.impl.StaticSeedLoaderRenderer;
import dev.skidfuscator.obfuscator.predicate.renderer.seed.impl.SwitchSeedLoaderRenderer;
import dev.skidfuscator.obfuscator.skidasm.SkidMethodNode;
import dev.skidfuscator.obfuscator.skidasm.cfg.SkidBlock;
import dev.skidfuscator.obfuscator.util.RandomUtil;

public interface SeedLoadable {

    SeedLoaderRenderer[] RENDERERS = new SeedLoaderRenderer[] {
            new StaticSeedLoaderRenderer(),
            new InternalSeedLoaderRenderer(),
            new SwitchSeedLoaderRenderer()
    };

    default void addSeedLoader(
            final SkidMethodNode methodNode,
            final SkidBlock block,
            final SkidBlock targetBlock,
            final int index,
            final BlockOpaquePredicate predicate,
            final int value,
            final String type
    ) {
        RENDERERS[RandomUtil.nextInt(RENDERERS.length)]
                .addSeedLoader(
                        methodNode,
                        block,
                        targetBlock,
                        index,
                        predicate,
                        value,
                        type
                );
    }
}
