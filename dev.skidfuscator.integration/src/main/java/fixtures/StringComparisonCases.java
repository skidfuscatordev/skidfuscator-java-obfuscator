package fixtures;

import java.util.Locale;

/** Java 8 compatible behavior corpus; no application services or external dependencies. */
public final class StringComparisonCases {
    private static int evaluations;
    private static String value(String value) { evaluations++; return value; }
    private static Object object(Object value) { evaluations++; return value; }
    private static boolean equalsNull(String value) { return value.equals(null); }
    private static boolean ignoresNull(String value) { return value.equalsIgnoreCase(null); }
    private static boolean literalReceiver(String value) { return "I".equals(value(value)); }
    private static boolean literalArgument(String value) { return value(value).equals("I"); }
    private static boolean insensitiveReceiver(String value) { return "I".equalsIgnoreCase(value(value)); }
    private static boolean insensitiveArgument(String value) { return value(value).equalsIgnoreCase("I"); }
    private static boolean objectArgument(Object value) { return "I".equals(object(value)); }
    private static void require(boolean valid) { if (!valid) throw new AssertionError("String comparison contract changed"); }

    public static String run() {
        Locale previous = Locale.getDefault();
        int digest = 1;
        try {
            for (Locale locale : new Locale[]{Locale.ROOT, new Locale("tr", "TR"), new Locale("az", "AZ")}) {
                Locale.setDefault(locale);
                for (String other : new String[]{null, "", "I", "i", "\u0130", "\u0131", "\u03a3", "\u212a", "\ud801\udc28"}) {
                    evaluations = 0;
                    boolean direct = literalReceiver(other);
                    require(evaluations == 1 && direct == "I".equals(other));
                    evaluations = 0;
                    boolean insensitive = insensitiveReceiver(other);
                    require(evaluations == 1 && insensitive == "I".equalsIgnoreCase(other));
                    digest = 31 * digest + (direct ? 1 : 0) + (insensitive ? 2 : 0);
                    if (other == null) {
                        evaluations = 0;
                        try { literalArgument(null); throw new AssertionError("Null receiver accepted"); }
                        catch (NullPointerException expected) { require(evaluations == 1); }
                        evaluations = 0;
                        try { insensitiveArgument(null); throw new AssertionError("Null receiver accepted"); }
                        catch (NullPointerException expected) { require(evaluations == 1); }
                        try { equalsNull(null); throw new AssertionError("Null receiver accepted"); }
                        catch (NullPointerException expected) { }
                        try { ignoresNull(null); throw new AssertionError("Null receiver accepted"); }
                        catch (NullPointerException expected) { }
                    } else {
                        evaluations = 0;
                        require(literalArgument(other) == other.equals("I") && evaluations == 1);
                        evaluations = 0;
                        require(insensitiveArgument(other) == other.equalsIgnoreCase("I") && evaluations == 1);
                        require(!equalsNull(other) && !ignoresNull(other));
                    }
                }
                for (Object other : new Object[]{null, "I", new StringBuilder("I"), Integer.valueOf(1), new Object()}) {
                    evaluations = 0;
                    require(objectArgument(other) == "I".equals(other) && evaluations == 1);
                }
            }
        } finally { Locale.setDefault(previous); }
        return Integer.toHexString(digest);
    }

    public static void main(String[] args) { System.out.println("RESULT:" + run()); }
}
