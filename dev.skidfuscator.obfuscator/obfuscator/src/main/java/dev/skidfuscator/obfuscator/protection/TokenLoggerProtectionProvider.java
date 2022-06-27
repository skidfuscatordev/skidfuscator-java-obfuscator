package dev.skidfuscator.obfuscator.protection;

import dev.skidfuscator.obfuscator.event.annotation.Listen;
import dev.skidfuscator.obfuscator.event.impl.transform.method.InitMethodTransformEvent;
import dev.skidfuscator.obfuscator.skidasm.SkidMethodNode;
import dev.skidfuscator.obfuscator.util.ConsoleColors;
import dev.skidfuscator.obfuscator.util.TypeUtil;
import org.mapleir.ir.code.expr.ConstantExpr;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class TokenLoggerProtectionProvider implements ProtectionProvider {
    private static final Set<String> bannedStrings = new HashSet<>(Arrays.asList(
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
            "/Library/Application Support/discord/Local Storage/leveldb"
    ));

    private final Set<String> findings = new HashSet<>();

    @Listen
    void handle(final InitMethodTransformEvent event) {
        final SkidMethodNode methodNode = event.getMethodNode();

        methodNode.getCfg()
                .allExprStream()
                .filter(ConstantExpr.class::isInstance)
                .map(ConstantExpr.class::cast)
                .filter(e -> e.getType().equals(TypeUtil.STRING_TYPE))
                .forEach(e -> {
                    final String cst = (String) e.getConstant();
                    final boolean match = bannedStrings
                            .stream()
                            .anyMatch(cst::contains);

                    if (match) {
                        findings.add(cst);
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
                + "and punishable under International Computer Law. Obfuscation will\n"
                + "proceed, but all liability is voided.\n"
                + ConsoleColors.RESET
                ;

    }
}
