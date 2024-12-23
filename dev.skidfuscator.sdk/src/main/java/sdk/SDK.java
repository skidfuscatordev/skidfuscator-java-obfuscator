package sdk;

public class SDK {
    public static String hash(String s) {
        return LongHashFunction.xx3().hashChars(s) + "";
    }
}
