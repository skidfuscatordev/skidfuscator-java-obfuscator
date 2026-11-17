package fixtures;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.*;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.hibernate.cfg.Configuration;
import jakarta.persistence.*;
import java.io.*;
import java.util.*;
import java.util.function.IntUnaryOperator;

@SpringBootConfiguration(proxyBeanMethods=false)
public class FrameworkApp {
    @Bean public String greeting() { return "hello"; }
    @Entity(name="Person") @Table(name="people")
    public static class Person {
        @Id public Long id;
        public String name;
        public Person() {}
        public Person(Long id,String name) { this.id=id;this.name=name; }
    }
    public record Point(int x,int y) implements Serializable {}
    public static class LegacyData implements Serializable {
        private final String value;
        public LegacyData(String value) { this.value=value; }
        public String value() { return value; }
    }
    public static class InheritedData extends ArrayList<String> {
        public InheritedData() { add("inherited"); }
    }
    public enum State { READY, DONE }
    public static void main(String[] args) throws Exception {
        Map<String,Object> result = new LinkedHashMap<>();
        result.put("stringComparisons",StringComparisonCases.run());
        try (var context=new SpringApplicationBuilder(FrameworkApp.class).web(WebApplicationType.NONE)
                .bannerMode(Banner.Mode.OFF).logStartupInfo(false).run()) {
            result.put("spring",context.getBean("greeting"));
        }
        ObjectMapper mapper=new ObjectMapper();
        Person dto=mapper.readValue("{\"id\":1,\"name\":\"Alice\"}",Person.class);
        result.put("jackson",mapper.readTree(mapper.writeValueAsString(dto)).get("name").asText());
        Configuration config=new Configuration().addAnnotatedClass(Person.class);
        config.setProperty("hibernate.connection.driver_class","org.h2.Driver");
        config.setProperty("hibernate.connection.url","jdbc:h2:mem:fixture;DB_CLOSE_DELAY=-1");
        config.setProperty("hibernate.hbm2ddl.auto","create-drop");
        config.setProperty("hibernate.show_sql","false");
        try (var factory=config.buildSessionFactory(); var session=factory.openSession()) {
            var transaction=session.beginTransaction(); session.persist(dto); transaction.commit(); session.clear();
            result.put("hibernate",session.find(Person.class,1L).name);
        }
        result.put("spi",ServiceLoader.load(Greeter.class).findFirst().orElseThrow().greet());
        Point point=new Point(3,7);
        var bytes=new ByteArrayOutputStream();
        try (var output=new ObjectOutputStream(bytes)) { output.writeObject(point); }
        try (var input=new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) { result.put("record",point.equals(input.readObject())); }
        IntUnaryOperator lambda=x->x*7+3;
        result.put("lambda",lambda.applyAsInt(5));
        result.put("enum",State.valueOf("DONE").name());
        result.put("reflection",FrameworkApp.class.getMethod("reflected").invoke(null));
        result.put("exceptions",withFinally());
        synchronized (FrameworkApp.class) { result.put("monitor",true); }
        if (args.length>1) {
            java.nio.file.Path persisted=java.nio.file.Path.of(args[1]);
            if (!java.nio.file.Files.exists(persisted))
                try (var out=new ObjectOutputStream(java.nio.file.Files.newOutputStream(persisted))) {
                    out.writeObject(new LegacyData("persisted")); out.writeObject(new InheritedData());
                }
            try (var in=new ObjectInputStream(java.nio.file.Files.newInputStream(persisted))) {
                result.put("legacySerialization",((LegacyData)in.readObject()).value());
                result.put("inheritedSerialization",((InheritedData)in.readObject()).get(0));
            }
        }
        if (args.length>0) { System.load(args[0]); result.put("jni",NativeBridge.twice(9)); }
        System.out.println("RESULT:"+mapper.writeValueAsString(result));
    }
    public static String reflected() { return "reflected"; }
    private static int withFinally() { int value=0; try { throw new IllegalArgumentException("fixture"); }
        catch (IllegalArgumentException e) { value=7; } finally { value++; } return value; }
}
