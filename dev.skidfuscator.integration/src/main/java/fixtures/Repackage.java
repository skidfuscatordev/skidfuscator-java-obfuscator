package fixtures;
import java.io.File;
import org.springframework.boot.loader.tools.*;
public class Repackage {
    public static void main(String[] args) throws Exception {
        Repackager packager=new Repackager(new File(args[0]));
        packager.setMainClass("fixtures.FrameworkApp");
        packager.repackage(new File(args[1]), callback->{
            File[] files=new File(args[2]).listFiles(file->file.getName().endsWith(".jar"));
            if (files==null) throw new java.io.IOException("Missing libraries");
            java.util.Arrays.sort(files);
            for (File file:files) callback.library(new Library(file,LibraryScope.COMPILE));
        });
    }
}
