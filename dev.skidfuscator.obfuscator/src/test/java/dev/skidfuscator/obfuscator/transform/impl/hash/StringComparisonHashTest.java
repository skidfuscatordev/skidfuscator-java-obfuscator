package dev.skidfuscator.obfuscator.transform.impl.hash;

import com.typesafe.config.ConfigFactory;
import dev.skidfuscator.obfuscator.Skidfuscator;
import org.junit.jupiter.api.Test;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.expr.ConstantExpr;
import org.mapleir.ir.code.expr.VarExpr;
import org.mapleir.ir.code.expr.invoke.InvocationExpr;
import org.mapleir.ir.code.expr.invoke.StaticInvocationExpr;
import org.mapleir.ir.code.expr.invoke.VirtualInvocationExpr;
import org.mapleir.ir.code.stmt.ReturnStmt;
import org.mapleir.ir.locals.Local;
import org.objectweb.asm.Type;
import dev.skidfuscator.config.DefaultSkidConfig;
import dev.skidfuscator.obfuscator.reliability.Profiles;
import dev.skidfuscator.obfuscator.ReliabilityOptions;
import sdk.SDK;
import java.util.Locale;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StringComparisonHashTest {
    private Skidfuscator owner() {
        Skidfuscator owner = mock(Skidfuscator.class);
        when(owner.getTsConfig()).thenReturn(ConfigFactory.empty());
        when(owner.getConfig()).thenReturn(new DefaultSkidConfig(ConfigFactory.empty(), ""));
        return owner;
    }

    private Expr variable() {
        return new VarExpr(mock(Local.class), Type.getType(String.class));
    }

    private VirtualInvocationExpr call(boolean ignoreCase, Expr receiver, Expr argument) {
        return new VirtualInvocationExpr(InvocationExpr.CallType.VIRTUAL, new Expr[]{receiver, argument},
                "java/lang/String", ignoreCase ? "equalsIgnoreCase" : "equals",
                ignoreCase ? "(Ljava/lang/String;)Z" : "(Ljava/lang/Object;)Z");
    }

    @Test void nullArgumentRemainsAnOriginalEqualsCall() {
        var call = call(false, variable(), new ConstantExpr(null));
        var statement = new ReturnStmt(Type.BOOLEAN_TYPE, call);
        assertFalse(new StringEqualsHashTransformer(owner()).transformExpression(call, null));
        assertSame(call, statement.getExpression());
    }

    @Test void nullReceiverRemainsAnOriginalEqualsCall() {
        var call = call(false, new ConstantExpr(null, Type.getType(String.class)), variable());
        var statement = new ReturnStmt(Type.BOOLEAN_TYPE, call);
        assertFalse(new StringEqualsHashTransformer(owner()).transformExpression(call, null));
        assertSame(call, statement.getExpression());
    }

    @Test void nullArgumentRemainsAnOriginalIgnoreCaseCall() {
        var call = call(true, variable(), new ConstantExpr(null, Type.getType(String.class)));
        var statement = new ReturnStmt(Type.BOOLEAN_TYPE, call);
        assertFalse(new StringEqualsIgnoreCaseHashTransformer(owner()).transformExpression(call, null));
        assertSame(call, statement.getExpression());
    }

    @Test void nullReceiverRemainsAnOriginalIgnoreCaseCall() {
        var call = call(true, new ConstantExpr(null, Type.getType(String.class)), variable());
        var statement = new ReturnStmt(Type.BOOLEAN_TYPE, call);
        assertFalse(new StringEqualsIgnoreCaseHashTransformer(owner()).transformExpression(call, null));
        assertSame(call, statement.getExpression());
    }

    @Test void constantReceiverAcceptsAnObjectArgumentWithoutAnUnsafeStringCast() {
        Expr other = new VarExpr(mock(Local.class), Type.getType(Object.class));
        var invocation = call(false, new ConstantExpr("literal"), other);
        var statement = new ReturnStmt(Type.BOOLEAN_TYPE, invocation);
        assertTrue(new StringEqualsHashTransformer(owner()).transformExpression(invocation, null));
        var replacement = (StaticInvocationExpr) statement.getExpression();
        assertEquals("equalsConstantReceiver", replacement.getName());
        assertEquals("(Ljava/lang/String;Ljava/lang/Object;J)Z", replacement.getDesc());
        Expr[] args = replacement.getArgumentExprs();
        assertEquals(Type.getType(Object.class), args[1].getType());
        String encoded = (String) ((ConstantExpr) args[0]).getConstant();
        long key = (Long) ((ConstantExpr) args[2]).getConstant();
        assertNotEquals("literal", encoded);
        assertTrue(SDK.equalsConstantReceiver(encoded, "literal", key));
        assertFalse(SDK.equalsConstantReceiver(encoded, new StringBuilder("literal"), key));
        assertFalse(SDK.equalsConstantReceiver(encoded, null, key));
    }

    @Test void literalArgumentKeepsTheReceiverInItsOriginalPosition() {
        var literal = new ConstantExpr("literal");
        var invocation = call(false, variable(), literal);
        var statement = new ReturnStmt(Type.BOOLEAN_TYPE, invocation);
        assertTrue(new StringEqualsHashTransformer(owner()).transformExpression(invocation, null));
        var replacement = (StaticInvocationExpr) statement.getExpression();
        assertEquals("equalsConstantArgument", replacement.getName());
        assertInstanceOf(VarExpr.class, replacement.getArgumentExprs()[0]);
        assertEquals("literal", literal.getConstant(), "Do not mutate detached/shared constant nodes");
        String encoded = (String) ((ConstantExpr) replacement.getArgumentExprs()[1]).getConstant();
        long key = (Long) ((ConstantExpr) replacement.getArgumentExprs()[2]).getConstant();
        assertTrue(SDK.equalsConstantArgument("literal", encoded, key));
        assertThrows(NullPointerException.class, () -> SDK.equalsConstantArgument(null, encoded, key));
    }

    @Test void runtimeNullsRetainNativeComparisonBehaviorInAllFourHelpers() {
        long key = 42;
        String encoded = SDK.maskComparisonLiteral("literal", key);
        assertFalse(SDK.equalsConstantReceiver(encoded, null, key));
        assertFalse(SDK.equalsIgnoreCaseConstantReceiver(encoded, null, key));
        assertThrows(NullPointerException.class, () -> SDK.equalsConstantArgument(null, encoded, key));
        assertThrows(NullPointerException.class, () -> SDK.equalsIgnoreCaseConstantArgument(null, encoded, key));
    }

    @Test void sharingAHashKeyNeverMakesUnequalStringsEqual() {
        long sameKey = 17;
        String first = SDK.maskComparisonLiteral("first", sameKey);
        String second = SDK.maskComparisonLiteral("second", sameKey);
        assertFalse(SDK.equalsConstantReceiver(first, "second", sameKey));
        assertFalse(SDK.equalsConstantArgument("first", second, sameKey));
        assertFalse(SDK.equalsIgnoreCaseConstantReceiver(first, "SECOND", sameKey));
        assertFalse(SDK.equalsIgnoreCaseConstantArgument("FIRST", second, sameKey));
    }

    @Test void unicodeCaseComparisonIsIndependentOfTheDefaultLocale() {
        Locale previous = Locale.getDefault();
        try {
            for (Locale locale : new Locale[]{Locale.ROOT, Locale.forLanguageTag("tr-TR"), Locale.forLanguageTag("az-AZ")}) {
                Locale.setDefault(locale);
                for (String literal : new String[]{"I", "i", "\u0130", "\u0131", "\u03c3", "\u03c2", "\u212a", "\u017f", "\ud801\udc00"}) {
                    long key = 314159;
                    String encoded = SDK.maskComparisonLiteral(literal, key);
                    for (String other : new String[]{"I", "i", "\u0130", "\u0131", "\u03a3", "k", "S", "\ud801\udc28", null}) {
                        assertEquals(literal.equalsIgnoreCase(other), SDK.equalsIgnoreCaseConstantReceiver(encoded, other, key));
                        if (other != null) assertEquals(other.equalsIgnoreCase(literal), SDK.equalsIgnoreCaseConstantArgument(other, encoded, key));
                    }
                }
            }
        } finally { Locale.setDefault(previous); }
    }

    @Test void maskingRetainsEveryUtf16CodeUnit() {
        Random random = new Random(123456);
        for (int sample = 0; sample < 200; sample++) {
            char[] units = new char[random.nextInt(256)];
            for (int i = 0; i < units.length; i++) units[i] = (char) random.nextInt(65536);
            String value = new String(units);
            long key = random.nextLong();
            assertEquals(value, SDK.maskComparisonLiteral(SDK.maskComparisonLiteral(value, key), key));
        }
    }

    @Test void aMaskThatCannotFitInAClassConstantPreservesTheOriginalExpression() {
        var invocation = call(false, variable(), new ConstantExpr("x".repeat(30000)));
        var statement = new ReturnStmt(Type.BOOLEAN_TYPE, invocation);
        assertFalse(new StringEqualsHashTransformer(owner()).transformExpression(invocation, null));
        assertSame(invocation, statement.getExpression());
    }

    @Test void bothIgnoreCaseConfigurationSpellingsCanDisableTheTransformer() {
        for (String name : new String[]{"stringEqualsIgnoreCaseHash", "stringEqigcaseHash"}) {
            Skidfuscator owner = owner();
            var explicit = ConfigFactory.parseString(name + ".enabled=false");
            when(owner.getTsConfig()).thenReturn(Profiles.apply(explicit, ReliabilityOptions.Profile.CONSERVATIVE));
            assertFalse(new StringEqualsIgnoreCaseHashTransformer(owner).isEnabled(), name);
        }
    }
}
