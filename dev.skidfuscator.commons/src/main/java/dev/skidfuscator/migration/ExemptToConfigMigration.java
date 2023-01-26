package dev.skidfuscator.migration;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ExemptToConfigMigration extends AbstractMigration {
    @Override
    public void migrate(File old, File updated) {
        final List<String> exclusions = new ArrayList<>();

        /*
         * This method is really scuffed but temporary for now. As
         * of right now we read every line from a .txt file, cache
         * them into an array list then pass them off to parsing.
         */
        try (final FileReader fileReader = new FileReader(old);
             final BufferedReader br = new BufferedReader(fileReader)) {

            String exclusion;
            while ((exclusion = br.readLine()) != null) {
                if (exclusion.equals(""))
                    continue;

                exclusions.add(exclusion);
            }
        } catch (IOException e) {
            System.out.println("Failed to load exempt");
            e.printStackTrace();
        }

        System.out.println(">>  Successfully found " + exclusions.size() + " exclusions");
        System.out.println(">>  Converting...");

        try (final FileWriter fileReader = new FileWriter(updated);
             final BufferedWriter writer = new BufferedWriter(fileReader)) {

            final InputStream localConfig = this.getClass().getResourceAsStream("/defaultConfig.hocon");
            final InputStreamReader reader = new InputStreamReader(localConfig);
            final BufferedReader bufferedReader = new BufferedReader(reader);

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (line.equals("%%INJECT%%")) {
                    for (String exclusion : exclusions) {
                        writer.write("  \"" + exclusion.replace("\\", "\\\\") + "\"\n");
                    }
                } else {
                    writer.write(line + "\n");
                }
            }
        } catch (IOException e) {
            System.out.println("Failed to write exempt");
            e.printStackTrace();
        }

        System.out.println(">>  Converted!");
    }
}
