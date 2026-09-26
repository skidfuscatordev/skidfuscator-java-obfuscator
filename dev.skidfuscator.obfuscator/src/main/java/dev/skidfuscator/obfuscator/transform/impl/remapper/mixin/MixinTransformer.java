package dev.skidfuscator.obfuscator.transform.impl.remapper.mixin;

import com.google.gson.*;
import dev.skidfuscator.config.DefaultTransformerConfig;
import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.event.EventPriority;
import dev.skidfuscator.obfuscator.event.annotation.Listen;
import dev.skidfuscator.obfuscator.event.impl.transform.clazz.InitClassTransformEvent;
import dev.skidfuscator.obfuscator.event.impl.transform.skid.FinalSkidTransformEvent;
import dev.skidfuscator.obfuscator.event.impl.transform.skid.InitSkidTransformEvent;
import dev.skidfuscator.obfuscator.skidasm.SkidClassNode;
import dev.skidfuscator.obfuscator.transform.AbstractTransformer;
import dev.skidfuscator.obfuscator.util.MiscUtil;
import dev.skidfuscator.obfuscator.util.misc.Pair;
import lombok.NonNull;
import org.topdank.byteengineer.commons.data.JarResource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Transforms mixin config files that contain remapped Mixin classes
 * <p>
 * TODO: Support Mixin Plugin transformation.
 * TODO: In theory, it is possible to transform methods & fields found within Mixin classes, would require a lot more work though.
 * TODO: ^ ideally, it would require for the Method, Field (and Class, to remove the requirement of the transformer holding the changes) nodes to keep track of the initial & updated name
 *
 * @author Trol
 */
public class MixinTransformer extends AbstractTransformer {

    private final Gson gson = new GsonBuilder().create();

    /**
     * A mixin configuration file should contain the following paths in order to be valid.
     */
    private final String[] mixinConfigPathsToCheck = new String[]{"mixins", "client", "server"};
    /**
     * A mixin refmap file should contain the following paths in order to be valid.
     */
    private final String[] refmapPathsToCheck = new String[]{"data", "mappings"};

    /**
     * Lazily loaded mixin refmap as a json object
     */
    private JsonObject mixinRefmap;

    /**
     * Lazily loaded mixin configuration as a json object
     */
    private JsonObject mixinConfig;

    /**
     * We store classes that are marked with @Mixin annotation within our own list, together with their original name.
     * Then, we check if any of them have been modified by comparing with the original name with {@link SkidClassNode#getName()}
     */
    private final List<Pair<String, SkidClassNode>> mixinsToCheck = new ArrayList<>();


    public MixinTransformer(Skidfuscator skidfuscator) {
        super(skidfuscator, "Mixin Config Transformer");
    }

    /**
     * The initialization phase we validate the files, if they are invalid, then there's no need for the program to run.
     */
    @Listen
    void initConfigs(InitSkidTransformEvent event) {
        final String refmapPath = this.getConfig().getRefmapPath();
        final String mixinConfigPath = this.getConfig().getMixinPath();
        if (!validatePath(refmapPath, "refmap") || !validatePath(mixinConfigPath, "config")
                || !parseAndGetFile(refmapPath, "refmap") || !parseAndGetFile(mixinConfigPath, "config")) {
            Skidfuscator.LOGGER.warn("Mixin Transformer is disabled, due to reasons above.");
            return;
        }
    }

    /**
     * Upon the initialization of the jar paths, we obtain the Mixin classes and configuration/refmap files.
     */
    @Listen(EventPriority.MONITOR)
    void gatherMixins(InitClassTransformEvent event) {
        if (getFailed() > 0) {
            return;
        }
        SkidClassNode classNode = event.getClassNode();
        if (classNode.isMixin()) {
            mixinsToCheck.add(new Pair<>(classNode.getName(), classNode));
        }

        if (mixinConfig.size() == 0) {
            Skidfuscator.LOGGER.warn("Mixin Remapper found 0 Mixin classes. Aborting mission.");
            this.fail();
        }
    }

    /**
     * Validates the remapped count, by checking against the original list, it's either all of them, or none of them.
     * Afterward we update the files.
     * NOTE: Remapper needs to not remap mixins into their own each separate package, otherwise it will fail too!
     */
    @Listen(EventPriority.FINALIZER)
    void transformMixins(FinalSkidTransformEvent event) {
        if (getFailed() > 0) {
            return;
        }
        // Ask if StreamAPI is usable for speed or nah.
        int remappedCount = mixinsToCheck.stream().filter(it -> !it.getA().equals(it.getB().getName())).toList().size();
        if (remappedCount != mixinConfig.size()) {
            this.fail();
            Skidfuscator.LOGGER.warn("Mixin Remapper remapping class mismatch: [got: " + remappedCount + ", expected: " + mixinsToCheck.size() + "]. Aborting mission.");
            return;
        }
        String firstNodeName = mixinsToCheck.get(0).getB().getName();
        String firstQualifiedPackage = firstNodeName.substring(0, firstNodeName.lastIndexOf('.'));
        for (Pair<String, SkidClassNode> stringSkidClassNodePair : mixinsToCheck.subList(1, mixinsToCheck.size())) {
            String fullyQualifiedName = stringSkidClassNodePair.getB().getName();
            String qualifiedPackage = fullyQualifiedName.substring(0, fullyQualifiedName.lastIndexOf('.'));
            if (!firstQualifiedPackage.equals(qualifiedPackage)) {
                this.fail();
                Skidfuscator.LOGGER.warn("Mixin Remapper found two Mixin classes in two different directories. Aborting mission!");
                return;
            }
        }
        if (getFailed() > 0) {
            return;
        }
        for (Pair<String, SkidClassNode> pair : mixinsToCheck) {
            // Maybe the remapper has support for only confusing the package name?
            // i.e if it was in com.test.mixins, then it would support the following:
            // com.test.mixins.client.MinecraftClientMixin
            // com.test.mixins.SharedConstantsMixin
            String oldName = pair.getA().substring(firstQualifiedPackage.length());
            String newName = pair.getB().getName().substring(firstQualifiedPackage.length());
            if (!updateMixinConfig(oldName.replace("/", "."), newName.replace("/", "."))) {
                this.fail();
                Skidfuscator.LOGGER.warn("Mixin Remapper did not find " + oldName.replace("/", ".") + " within the Mixin configuration file");
                break;
            }
            if (!updateRefmapConfig(pair.getA(), pair.getB().getName())) {
                this.fail();
                Skidfuscator.LOGGER.warn("Mixin Remapper did not find " + pair.getA() + " within the Mixin refmap file");
                break;
            }
            this.success();
        }
    }

    /**
     * Updates the old class name to the new one within the Mixin configuration name
     *
     * @param oldName The initial name of the class
     * @param newName The remapped name of the class
     * @return status of the name being updated within the file
     */
    private Boolean updateMixinConfig(String oldName, String newName) {
        boolean flag = false;
        // It should only appear in one path, if it does in multiple - skill issue
        // Client / Server mixins are supposed to be separated, common - for both.
        for (String path : mixinConfigPathsToCheck) {
            JsonArray mixins = (JsonArray) this.mixinConfig.get(path);
            for (int i = 0; i < mixins.size(); i++) {
                String mixin = mixins.get(i).getAsString();
                if (mixin.equals(oldName)) {
                    mixins.set(i, new JsonPrimitive(newName));
                    flag = true;
                    break;
                }
            }
        }
        return flag;
    }

    /**
     * Updates the "mappings" and "data" of oldName to newName
     * TODO: Attempt to see if any specific Mixin version uses different identifications of this, but seeing as Mixin never went out of beta, this isn't the case.
     *
     * @param oldName The initial name of the class
     * @param newName The remapped name of the class
     * @return status of the name being updated within the file
     */
    private Boolean updateRefmapConfig(String oldName, String newName) {
        JsonObject mappings = mixinRefmap.get("mappings").getAsJsonObject();
        if (!mappings.has(oldName)) {
            return false;
        }
        JsonObject clazzMappings = mappings.get(oldName).getAsJsonObject();
        mappings.add(newName, clazzMappings);
        mappings.remove(oldName);

        JsonObject data = mixinRefmap.get("data").getAsJsonObject();
        for (Map.Entry<String, JsonElement> entry : data.entrySet()) {
            JsonObject clazzData = entry.getValue().getAsJsonObject();

            if (!clazzData.has(oldName)){
                return false;
            }
            JsonObject mappedData = clazzData.get(oldName).getAsJsonObject();
            clazzData.add(newName, mappedData);
            clazzData.remove(oldName);
            break;
        }
        return true;
    }

    /**
     * Failsafe to check if the mixin config is populated
     *
     * @param path the given path of the provided type
     * @param type the type of file it is currently checking
     * @return result of it checking if it is populated and valid.
     */
    private boolean validatePath(@NonNull String path, @NonNull String type) {
        if (path.equals("not_found")) {
            this.fail();
            // TODO: Is there a way to show an error message? This is critically important.
            Skidfuscator.LOGGER.warn("Mixin " + type + " file is not set");
            return false;
        }

        if (this.skidfuscator.getJarContents().getResourceContents().namedMap().containsKey(path)) {
            this.fail();
            Skidfuscator.LOGGER.warn("Mixin " + type + " at " + path + " does not exist.");
            return false;
        }
        return true;
    }

    /**
     * Validates the given file if it is indeed a mixin file, for specific types it is looking for, see (mixinConfigs|refMap)pathsToCheck
     *
     * @param path the resource path given
     * @param type the type it is currently checking
     * @return parsed file as a JsonObject, or empty.
     */
    private Optional<JsonObject> isValidFormatAndParse(@NonNull String path, @NonNull String type) {
        JarResource jarResource = this.skidfuscator.getJarContents().getResourceContents().namedMap().get(path);

        try {
            final ByteArrayInputStream bais = new ByteArrayInputStream(jarResource.getData());

            final InputStreamReader isr = new InputStreamReader(bais);

            JsonObject jsonObject = gson.fromJson(isr, JsonObject.class);

            String[] paths = type.equals("refmap") ? refmapPathsToCheck : mixinConfigPathsToCheck;
            boolean flag = true;
            for (String jsonElementPath : paths) {
                if (jsonObject.has(jsonElementPath)) {
                    flag = false;
                }
            }

            if (flag) {
                // Maybe it should support plugins? But for the first revision, it should be fine I think.
                Skidfuscator.LOGGER.warn("Provided mixin " + type + " does not have any valid paths to remap.");
                return Optional.empty();
            }


            isr.close();
            bais.close();

            return Optional.of(jsonObject);
        } catch (IOException e) {
            Skidfuscator.LOGGER.warn("Failed to close the file reading of " + path);
            return Optional.empty();
        }
    }

    /**
     * Parses the file {@link #isValidFormatAndParse} and updates the fields mixin(Refmap|Config)
     *
     * @param path the path to the file
     * @param type the type of file being currently parsed
     * @return if it succeeded in parsing or not.
     */
    private boolean parseAndGetFile(String path, String type) {
        Optional<JsonObject> parsedData = isValidFormatAndParse(path, type);

        if (parsedData.isEmpty()) {
            return false;
        }
        if (type.equals("refmap")) {
            this.mixinRefmap = parsedData.get();
        } else {
            this.mixinConfig = parsedData.get();
        }
        return true;
    }

    @Override
    protected <T extends DefaultTransformerConfig> T createConfig() {
        return (T) new MixinConfig(skidfuscator.getTsConfig(), MiscUtil.toCamelCase(name));
    }

    @Override
    public MixinConfig getConfig() {
        return (MixinConfig) super.getConfig();
    }
}
