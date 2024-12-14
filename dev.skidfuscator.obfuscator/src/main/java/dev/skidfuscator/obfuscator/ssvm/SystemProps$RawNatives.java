package dev.skidfuscator.obfuscator.ssvm;

import dev.xdark.ssvm.VirtualMachine;
import dev.xdark.ssvm.api.VMInterface;
import dev.xdark.ssvm.execution.Result;
import dev.xdark.ssvm.mirror.type.InstanceClass;
import dev.xdark.ssvm.operation.VMOperations;
import dev.xdark.ssvm.value.ArrayValue;

import java.util.Map;

/**
 * Sets up native bindings for jdk/internal/util/SystemProps$Raw,
 * integrating both VM and platform properties, including support
 * for propDefault() retrieval. This ensures that when SystemProps
 * requests default platform properties, a pre-populated array is
 * returned to simulate the native environment.
 */
public final class SystemProps$RawNatives {
    // Index constants as defined in the provided code
    private static final int _display_country_NDX = 0;
    private static final int _display_language_NDX = 1;
    private static final int _display_script_NDX = 2;
    private static final int _display_variant_NDX = 3;
    private static final int _file_encoding_NDX = 4;
    private static final int _file_separator_NDX = 5;
    private static final int _format_country_NDX = 6;
    private static final int _format_language_NDX = 7;
    private static final int _format_script_NDX = 8;
    private static final int _format_variant_NDX = 9;
    private static final int _ftp_nonProxyHosts_NDX = 10;
    private static final int _ftp_proxyHost_NDX = 11;
    private static final int _ftp_proxyPort_NDX = 12;
    private static final int _http_nonProxyHosts_NDX = 13;
    private static final int _http_proxyHost_NDX = 14;
    private static final int _http_proxyPort_NDX = 15;
    private static final int _https_proxyHost_NDX = 16;
    private static final int _https_proxyPort_NDX = 17;
    private static final int _java_io_tmpdir_NDX = 18;
    private static final int _line_separator_NDX = 19;
    private static final int _os_arch_NDX = 20;
    private static final int _os_name_NDX = 21;
    private static final int _os_version_NDX = 22;
    private static final int _path_separator_NDX = 23;
    private static final int _socksNonProxyHosts_NDX = 24;
    private static final int _socksProxyHost_NDX = 25;
    private static final int _socksProxyPort_NDX = 26;
    private static final int _sun_arch_abi_NDX = 27;
    private static final int _sun_arch_data_model_NDX = 28;
    private static final int _sun_cpu_endian_NDX = 29;
    private static final int _sun_cpu_isalist_NDX = 30;
    private static final int _sun_io_unicode_encoding_NDX = 31;
    private static final int _sun_jnu_encoding_NDX = 32;
    private static final int _sun_os_patch_level_NDX = 33;
    private static final int _sun_stderr_encoding_NDX = 34;
    private static final int _sun_stdout_encoding_NDX = 35;
    private static final int _user_dir_NDX = 36;
    private static final int _user_home_NDX = 37;
    private static final int _user_name_NDX = 38;
    private static final int FIXED_LENGTH = 39;

    public static void init(VirtualMachine vm) {
        InstanceClass jc = (InstanceClass) vm.findBootstrapClass("jdk/internal/util/SystemProps$Raw");
        if (jc != null) {
            VMInterface vmi = vm.getInterface();
            VMOperations ops = vm.getOperations();

            // Provide platform properties for propDefault
            vmi.setInvoker(jc, "platformProperties", "()[Ljava/lang/String;", ctx -> {
                ArrayValue platformArray = ops.allocateArray(vm.getSymbols().java_lang_String(), FIXED_LENGTH);

                // Provide meaningful defaults - adjust to suit your environment
                platformArray.setReference(_display_country_NDX,       ops.newUtf8(vm.getProperties().getOrDefault("user.country", "US")));
                platformArray.setReference(_display_language_NDX,      ops.newUtf8(vm.getProperties().getOrDefault("user.language", "en")));
                platformArray.setReference(_display_script_NDX,        ops.newUtf8(vm.getProperties().getOrDefault("user.script", "")));
                platformArray.setReference(_display_variant_NDX,       ops.newUtf8(vm.getProperties().getOrDefault("user.variant", "")));
                platformArray.setReference(_file_encoding_NDX,         ops.newUtf8("UTF-8"));
                platformArray.setReference(_file_separator_NDX,        ops.newUtf8(vm.getProperties().getOrDefault("file.separator", "/")));
                platformArray.setReference(_format_country_NDX,        ops.newUtf8("US"));
                platformArray.setReference(_format_language_NDX,       ops.newUtf8("en"));
                platformArray.setReference(_format_script_NDX,         ops.newUtf8(""));
                platformArray.setReference(_format_variant_NDX,        ops.newUtf8(""));
                platformArray.setReference(_ftp_nonProxyHosts_NDX,     ops.newUtf8(""));
                platformArray.setReference(_ftp_proxyHost_NDX,         ops.newUtf8(""));
                platformArray.setReference(_ftp_proxyPort_NDX,         ops.newUtf8(""));
                platformArray.setReference(_http_nonProxyHosts_NDX,    ops.newUtf8(""));
                platformArray.setReference(_http_proxyHost_NDX,        ops.newUtf8(""));
                platformArray.setReference(_http_proxyPort_NDX,        ops.newUtf8(""));
                platformArray.setReference(_https_proxyHost_NDX,       ops.newUtf8(""));
                platformArray.setReference(_https_proxyPort_NDX,       ops.newUtf8(""));
                platformArray.setReference(_java_io_tmpdir_NDX,        ops.newUtf8(vm.getProperties().getOrDefault("java.io.tmpdir", "/tmp")));
                platformArray.setReference(_line_separator_NDX,        ops.newUtf8(System.lineSeparator()));
                platformArray.setReference(_os_arch_NDX,               ops.newUtf8(vm.getProperties().getOrDefault("os.arch", "amd64")));
                platformArray.setReference(_os_name_NDX,               ops.newUtf8(vm.getProperties().getOrDefault("os.name", "Linux")));
                platformArray.setReference(_os_version_NDX,            ops.newUtf8(vm.getProperties().getOrDefault("os.version", "5.11.0-0")));
                platformArray.setReference(_path_separator_NDX,        ops.newUtf8(vm.getProperties().getOrDefault("path.separator", ":")));
                platformArray.setReference(_socksNonProxyHosts_NDX,    ops.newUtf8(""));
                platformArray.setReference(_socksProxyHost_NDX,        ops.newUtf8(""));
                platformArray.setReference(_socksProxyPort_NDX,        ops.newUtf8(""));
                platformArray.setReference(_sun_arch_abi_NDX,          ops.newUtf8(""));
                platformArray.setReference(_sun_arch_data_model_NDX,   ops.newUtf8("64"));
                platformArray.setReference(_sun_cpu_endian_NDX,        ops.newUtf8("little"));
                platformArray.setReference(_sun_cpu_isalist_NDX,       ops.newUtf8(""));
                platformArray.setReference(_sun_io_unicode_encoding_NDX, ops.newUtf8(""));
                platformArray.setReference(_sun_jnu_encoding_NDX,       ops.newUtf8("UTF-8"));
                platformArray.setReference(_sun_os_patch_level_NDX,     ops.newUtf8(""));
                platformArray.setReference(_sun_stderr_encoding_NDX,    ops.newUtf8("UTF-8"));
                platformArray.setReference(_sun_stdout_encoding_NDX,    ops.newUtf8("UTF-8"));
                platformArray.setReference(_user_dir_NDX,               ops.newUtf8(vm.getProperties().getOrDefault("user.dir", "/home/user")));
                platformArray.setReference(_user_home_NDX,              ops.newUtf8(vm.getProperties().getOrDefault("user.home", "/home/user")));
                platformArray.setReference(_user_name_NDX,              ops.newUtf8(vm.getProperties().getOrDefault("user.name", "user")));

                ctx.setResult(platformArray);
                return Result.ABORT;
            });

            // Provide VM properties from the VM environment
            vmi.setInvoker(jc, "vmProperties", "()[Ljava/lang/String;", ctx -> {
                Map<String, String> properties = vm.getProperties();
                ArrayValue array = ops.allocateArray(vm.getSymbols().java_lang_String(), properties.size() * 2);
                int i = 0;
                for (Map.Entry<String, String> entry : properties.entrySet()) {
                    array.setReference(i++, ops.newUtf8(entry.getKey()));
                    array.setReference(i++, ops.newUtf8(entry.getValue()));
                }

                ctx.setResult(array);
                return Result.ABORT;
            });
        }
    }

    private SystemProps$RawNatives() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
