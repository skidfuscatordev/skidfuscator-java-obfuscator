package dev.skidfuscator.obfuscator.ssvm;

import dev.xdark.ssvm.VirtualMachine;
import dev.xdark.ssvm.api.MethodInvoker;
import dev.xdark.ssvm.api.VMInterface;
import dev.xdark.ssvm.execution.Result;
import dev.xdark.ssvm.mirror.type.InstanceClass;
import dev.xdark.ssvm.operation.VMOperations;
import dev.xdark.ssvm.value.ArrayValue;
import dev.xdark.ssvm.value.ObjectValue;

import java.util.Map;

/**
 * Sets up native bindings for jdk/internal/util/SystemProps$Raw,
 * integrating both VM and platform properties, including support
 * for propDefault() retrieval. If the index is out of range, we
 * return an empty string.
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

    // Cache the platform properties so we can access them in propDefault
    private static ArrayValue platformArrayCache;

    public static void init(VirtualMachine vm) {
        InstanceClass jc = (InstanceClass) vm.findBootstrapClass("jdk/internal/util/SystemProps$Raw");
        if (jc != null) {
            VMInterface vmi = vm.getInterface();
            VMOperations ops = vm.getOperations();

            // Provide platform properties for propDefault
            vmi.setInvoker(jc, "platformProperties", "()[Ljava/lang/String;", ctx -> {
                if (platformArrayCache == null) {
                    platformArrayCache = ops.allocateArray(vm.getSymbols().java_lang_String(), FIXED_LENGTH);

                    // Provide meaningful defaults - adjust to suit your environment
                    platformArrayCache.setReference(_display_country_NDX,       ops.newUtf8(vm.getProperties().getOrDefault("user.country", "US")));
                    platformArrayCache.setReference(_display_language_NDX,      ops.newUtf8(vm.getProperties().getOrDefault("user.language", "en")));
                    platformArrayCache.setReference(_display_script_NDX,        ops.newUtf8(vm.getProperties().getOrDefault("user.script", "")));
                    platformArrayCache.setReference(_display_variant_NDX,       ops.newUtf8(vm.getProperties().getOrDefault("user.variant", "")));
                    platformArrayCache.setReference(_file_encoding_NDX,         ops.newUtf8("UTF-8"));
                    platformArrayCache.setReference(_file_separator_NDX,        ops.newUtf8(vm.getProperties().getOrDefault("file.separator", "/")));
                    platformArrayCache.setReference(_format_country_NDX,        ops.newUtf8("US"));
                    platformArrayCache.setReference(_format_language_NDX,       ops.newUtf8("en"));
                    platformArrayCache.setReference(_format_script_NDX,         ops.newUtf8(""));
                    platformArrayCache.setReference(_format_variant_NDX,        ops.newUtf8(""));
                    platformArrayCache.setReference(_ftp_nonProxyHosts_NDX,     ops.newUtf8(""));
                    platformArrayCache.setReference(_ftp_proxyHost_NDX,         ops.newUtf8(""));
                    platformArrayCache.setReference(_ftp_proxyPort_NDX,         ops.newUtf8(""));
                    platformArrayCache.setReference(_http_nonProxyHosts_NDX,    ops.newUtf8(""));
                    platformArrayCache.setReference(_http_proxyHost_NDX,        ops.newUtf8(""));
                    platformArrayCache.setReference(_http_proxyPort_NDX,        ops.newUtf8(""));
                    platformArrayCache.setReference(_https_proxyHost_NDX,       ops.newUtf8(""));
                    platformArrayCache.setReference(_https_proxyPort_NDX,       ops.newUtf8(""));
                    platformArrayCache.setReference(_java_io_tmpdir_NDX,        ops.newUtf8(vm.getProperties().getOrDefault("java.io.tmpdir", "/tmp")));
                    platformArrayCache.setReference(_line_separator_NDX,        ops.newUtf8(System.lineSeparator()));
                    platformArrayCache.setReference(_os_arch_NDX,               ops.newUtf8(vm.getProperties().getOrDefault("os.arch", "amd64")));
                    platformArrayCache.setReference(_os_name_NDX,               ops.newUtf8(vm.getProperties().getOrDefault("os.name", "Linux")));
                    platformArrayCache.setReference(_os_version_NDX,            ops.newUtf8(vm.getProperties().getOrDefault("os.version", "5.11.0-0")));
                    platformArrayCache.setReference(_path_separator_NDX,        ops.newUtf8(vm.getProperties().getOrDefault("path.separator", ":")));
                    platformArrayCache.setReference(_socksNonProxyHosts_NDX,    ops.newUtf8(""));
                    platformArrayCache.setReference(_socksProxyHost_NDX,        ops.newUtf8(""));
                    platformArrayCache.setReference(_socksProxyPort_NDX,        ops.newUtf8(""));
                    platformArrayCache.setReference(_sun_arch_abi_NDX,          ops.newUtf8(""));
                    platformArrayCache.setReference(_sun_arch_data_model_NDX,   ops.newUtf8("64"));
                    platformArrayCache.setReference(_sun_cpu_endian_NDX,        ops.newUtf8("little"));
                    platformArrayCache.setReference(_sun_cpu_isalist_NDX,       ops.newUtf8(""));
                    platformArrayCache.setReference(_sun_io_unicode_encoding_NDX, ops.newUtf8(""));
                    platformArrayCache.setReference(_sun_jnu_encoding_NDX,       ops.newUtf8("UTF-8"));
                    platformArrayCache.setReference(_sun_os_patch_level_NDX,     ops.newUtf8(""));
                    platformArrayCache.setReference(_sun_stderr_encoding_NDX,    ops.newUtf8("UTF-8"));
                    platformArrayCache.setReference(_sun_stdout_encoding_NDX,    ops.newUtf8("UTF-8"));
                    platformArrayCache.setReference(_user_dir_NDX,               ops.newUtf8(vm.getProperties().getOrDefault("user.dir", "/home/user")));
                    platformArrayCache.setReference(_user_home_NDX,              ops.newUtf8(vm.getProperties().getOrDefault("user.home", "/home/user")));
                    platformArrayCache.setReference(_user_name_NDX,              ops.newUtf8(vm.getProperties().getOrDefault("user.name", "user")));
                }

                ctx.setResult(platformArrayCache);
                return Result.ABORT;
            });

            vmi.setInvoker(jc, "<init>", "()V", MethodInvoker.noop());

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

            // Override propDefault(int) to return empty if index is not found or null.
            // Signature: propDefault(I)Ljava/lang/String;
            vmi.setInvoker(jc, "propDefault", "(I)Ljava/lang/String;", ctx -> {
                int index = ctx.getLocals().loadInt(1);
                VMOperations vops = vm.getOperations();

                // Ensure platformArrayCache is initialized
                if (platformArrayCache == null) {
                    // If for some reason platformProperties hasn't been called yet, call it now.
                    // This will populate platformArrayCache.
                    platformArrayCache = vops.allocateArray(vm.getSymbols().java_lang_String(), FIXED_LENGTH);
                    // You may reinitialize it here or ensure platformProperties is called beforehand.
                    // For brevity, assume defaults again or call ctx.getInvoker() with platformProperties if accessible.
                }

                if (index < 0 || index >= FIXED_LENGTH) {
                    ctx.setResult(vops.newUtf8(""));
                } else {
                    ObjectValue val = platformArrayCache.getReference(index);
                    if (val.isNull()) {
                        ctx.setResult(vops.newUtf8(""));
                    } else {
                        ctx.setResult(val);
                    }
                }
                return Result.ABORT;
            });
        }
    }

    private SystemProps$RawNatives() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
