package fixtures;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Seeded property corpus: integer overflow, exceptions, switches, loops, arrays and NaNs. */
public class DifferentialCases {
    static int evaluate(int a,int b) {
        int value=0;
        try {
            int[] array={a,b,a^b};
            for (int i=0;i<7;i++) value+=(array[i%3]*31)^(i+a);
            switch(a&3) { case 0:value+=a/b;break;case 1:value^=a<<b;break;case 2:value-=a*b;break;default:value+=b-a; }
        } catch (ArithmeticException e) { value=0x7fff0001; }
        finally { value^=0x13579bdf; }
        return value;
    }
    public static void main(String[] args) throws Exception {
        long seed=args.length==0?1234567:Long.parseLong(args[0]);
        Random random=new Random(seed); MessageDigest digest=MessageDigest.getInstance("SHA-256");
        for (int i=0;i<1000;i++) {
            int a=random.nextInt(), b=i%7==0?0:random.nextInt();
            String observed=evaluate(a,b)+":"+Double.doubleToLongBits(Math.sqrt(i%3==0?-1.0:i));
            digest.update(observed.getBytes(StandardCharsets.UTF_8));
        }
        System.out.println("RESULT:"+HexFormat.of().formatHex(digest.digest()));
    }
}
