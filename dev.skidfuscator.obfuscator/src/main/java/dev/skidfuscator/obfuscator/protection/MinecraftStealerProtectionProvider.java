package dev.skidfuscator.obfuscator.protection;

import dev.skidfuscator.obfuscator.event.annotation.Listen;
import dev.skidfuscator.obfuscator.event.impl.transform.method.InitMethodTransformEvent;
import dev.skidfuscator.obfuscator.skidasm.SkidMethodNode;
import dev.skidfuscator.obfuscator.skidasm.expr.SkidConstantExpr;
import dev.skidfuscator.obfuscator.util.ConsoleColors;
import dev.skidfuscator.obfuscator.util.TypeUtil;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.code.expr.ConstantExpr;
import org.mapleir.ir.code.stmt.PopStmt;

import javax.swing.text.html.Option;
import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Collectors;

public class MinecraftStealerProtectionProvider implements ProtectionProvider {
    private static final List<String> bannedStrings = Arrays.asList(
            ".feather/accounts.json",
            "essential/microsoft_accounts.json",
            ".lunarclient/settings/game/accounts.json"
    );

    private final Set<String> findings = new HashSet<>();

    @Listen
    void handle(final InitMethodTransformEvent event) {
        final SkidMethodNode methodNode = event.getMethodNode();

        methodNode.getCfg()
                .allExprStream()
                .filter(SkidConstantExpr.class::isInstance)
                .map(SkidConstantExpr.class::cast)
                .filter(e -> e.getType().equals(TypeUtil.STRING_TYPE))
                .collect(Collectors.toList())
                .forEach(e -> {
                    final String cst = (String) e.getConstant();
                    final Optional<String> match = bannedStrings
                            .stream()
                            .filter(cst::contains)
                            .findFirst();

                    if (match.isPresent()) {
                        findings.add(cst);

                        e.setExempt(true);

                        final BasicBlock basicBlock = e.getBlock();
                        final ConstantExpr warner = new ConstantExpr(
                                "[Skidfuscator Anti-Abuse] MinecraftStealer Type "
                                        + Integer.toHexString(bannedStrings.indexOf(match.get())),
                                TypeUtil.STRING_TYPE
                        );
                        basicBlock.add(0, new PopStmt(warner));
                    }
                });
    }

    @Override
    public boolean shouldWarn() {
        return !findings.isEmpty();
    }

    @Override
    public String getWarning() {
        return ConsoleColors.YELLOW
                + "██╗    ██╗ █████╗ ██████╗ ███╗   ██╗██╗███╗   ██╗ ██████╗ \n"
                + "██║    ██║██╔══██╗██╔══██╗████╗  ██║██║████╗  ██║██╔════╝ \n"
                + "██║ █╗ ██║███████║██████╔╝██╔██╗ ██║██║██╔██╗ ██║██║  ███╗\n"
                + "██║███╗██║██╔══██║██╔══██╗██║╚██╗██║██║██║╚██╗██║██║   ██║\n"
                + "╚███╔███╔╝██║  ██║██║  ██║██║ ╚████║██║██║ ╚████║╚██████╔╝\n"
                + " ╚══╝╚══╝ ╚═╝  ╚═╝╚═╝  ╚═╝╚═╝  ╚═══╝╚═╝╚═╝  ╚═══╝ ╚═════╝ \n"
                + "\n"
                + "⚠️  Warning! Skidfuscator has found some suspicious strings!\n"
                + "\n"
                + ConsoleColors.YELLOW_BOLD_BRIGHT + "Type:" + ConsoleColors.YELLOW + " Minecraft Stealer\n"
                + ConsoleColors.YELLOW_BOLD_BRIGHT + "Confidence: " + ConsoleColors.RED + "HIGH" + ConsoleColors.YELLOW + "\n"
                + ConsoleColors.YELLOW_BOLD_BRIGHT + "Findings: \n" + ConsoleColors.YELLOW
                + " - " + String.join("\n - ", findings)
                + "\n"
                + "\n"
                + ConsoleColors.YELLOW_BRIGHT
                + "If you believe this is an error, please submit a bug report.\n"
                + "You are reminded that illicit access to remote hardware is illegal\n"
                + "and punishable under International Computer Law. Stealing information\n"
                + "and other any other forms of infostealing, hacking, or abuse of power is"
                + "a CRIME.\n"
                + "Obfuscation will proceed, but all liability is voided.\n"
                + ConsoleColors.RESET
                ;
    }
}
