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

public class TokenLoggerProtectionProvider implements ProtectionProvider {
    private static final List<String> bannedStrings = Arrays.asList(
            "https://discordapp.com/api/v6/users/@me",
            "https://discord.com/api/v8/users/@me",
            "\\Discord\\Local Storage\\leveldb",
            "\\discordcanary\\Local Storage\\leveldb",
            "\\discordptb\\Local Storage\\leveldb",
            "\\Google\\Chrome\\User Data\\Default\\Local Storage\\leveldb",
            "\\Opera Software\\Opera Stable\\Local Storage\\leveldb",
            "\\BraveSoftware\\Brave-Browser\\User Data\\Default\\Local Storage\\leveldb",
            "\\Yandex\\YandexBrowser\\User Data\\Default\\Local Storage\\leveldb",
            ".config/BraveSoftware/Brave-Browser/Default/Local Storage/leveldb",
            ".config/yandex-browser-beta/Default/Local Storage/leveldb",
            ".config/yandex-browser/Default/Local Storage/leveldb",
            ".config/google-chrome/Default/Local Storage/leveldb",
            ".config/opera/Local Storage/leveldb",
            ".config/discord/Local Storage/leveldb",
            ".config/discordcanary/Local Storage/leveldb",
            ".config/discordptb/Local Storage/leveldb",
            "/Library/Application Support/discord/Local Storage/leveldb",
            "discord/Local Storage/leveldb",
            "(dQw4w9WgXcQ:)([^.*\\\\['(.*)\\\\]$][^\"]*)"
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
                                "[Skidfuscator Anti-Abuse] TokenLogger Type "
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
                + ConsoleColors.YELLOW_BOLD_BRIGHT + "Type:" + ConsoleColors.YELLOW + " Discord Token Logger\n"
                + ConsoleColors.YELLOW_BOLD_BRIGHT + "Confidence: " + ConsoleColors.RED + "HIGH" + ConsoleColors.YELLOW + "\n"
                + ConsoleColors.YELLOW_BOLD_BRIGHT + "Findings: \n" + ConsoleColors.YELLOW
                + " - " + String.join("\n - ", findings)
                + "\n"
                + "\n"
                + ConsoleColors.YELLOW_BRIGHT
                + "If you believe this is an error, please submit a bug report.\n"
                + "You are reminded that illicit access to remote hardware is illegal\n"
                + "and punishable under International Computer Law. Discord Token Logging\n"
                + "and other forms of ratting, hacking, or abuse of power is a CRIME.\n"
                + "Obfuscation will proceed, but all liability is voided.\n"
                + ConsoleColors.RESET
                ;
    }
}
