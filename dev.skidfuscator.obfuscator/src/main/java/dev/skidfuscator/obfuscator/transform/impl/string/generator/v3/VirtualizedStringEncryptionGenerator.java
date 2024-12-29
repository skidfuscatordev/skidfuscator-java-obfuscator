package dev.skidfuscator.obfuscator.transform.impl.string.generator.v3;

import dev.skidfuscator.obfuscator.skidasm.SkidClassNode;
import dev.skidfuscator.obfuscator.skidasm.SkidMethodNode;
import dev.skidfuscator.obfuscator.skidasm.cfg.SkidBlock;
import dev.skidfuscator.obfuscator.util.RandomUtil;
import org.mapleir.ir.code.Expr;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class VirtualizedStringEncryptionGenerator extends AbstractEncryptionGeneratorV3 {
    private final byte[] keys;
    private final Map<Integer, Integer> vmOpcodes;
    private final int[] shuffleMap;

    public VirtualizedStringEncryptionGenerator() {
        super("VM String Generator");
        this.keys = RandomUtil.randomBytes(32);
        this.vmOpcodes = generateOpcodeMap();
        this.shuffleMap = generateShuffleMap();
    }

    private Map<Integer, Integer> generateOpcodeMap() {
        Map<Integer, Integer> map = new HashMap<>();
        List<Integer> values = new ArrayList<>();
        
        for (int i = 0; i < OpcodeType.values().length; i++) {
            values.add(i);
        }
        Collections.shuffle(values);
        
        for (int i = 0; i < OpcodeType.values().length; i++) {
            map.put(RandomUtil.nextInt(1000) + 1000, values.get(i));
        }
        return map;
    }

    private int[] generateShuffleMap() {
        int[] map = new int[256];
        for (int i = 0; i < map.length; i++) {
            map[i] = i;
        }
        for (int i = map.length - 1; i > 0; i--) {
            int j = RandomUtil.nextInt(i + 1);
            int temp = map[i];
            map[i] = map[j];
            map[j] = temp;
        }
        return map;
    }

    @Override
    public void visitPre(SkidClassNode node) {
        super.visitPre(node);

        // Inject runtime fields
        node.getClassInit().getEntryBlock().add(0, storeInjectField(
                node,
                "keys",
                "[B",
                generateByteArrayGenerator(node, keys)
        ));

        node.getClassInit().getEntryBlock().add(0, storeInjectField(
                node,
                "vm_map",
                "[I",
                generateIntArrayGenerator(node, shuffleMap)
        ));

        node.getClassInit().getEntryBlock().add(0, storeInjectField(
                node,
                "vm_opcodes",
                "[I",
                generateIntArrayFromMap(node, vmOpcodes)
        ));
    }

    @Override
    public String decrypt(DecryptorDictionary input, int key) {
        return "";
    }

    @Override
    public void visitPost(SkidClassNode node) {
        super.visitPost(node);
    }

    private byte[] convertToByteArray(int[] arr) {
        byte[] result = new byte[arr.length * 4];
        for (int i = 0; i < arr.length; i++) {
            int val = arr[i];
            result[i * 4] = (byte) (val >>> 24);
            result[i * 4 + 1] = (byte) (val >>> 16);
            result[i * 4 + 2] = (byte) (val >>> 8);
            result[i * 4 + 3] = (byte) val;
        }
        return result;
    }

    private byte[] convertToByteArray(Map<Integer, Integer> map) {
        byte[] result = new byte[map.size() * 8];
        int i = 0;
        for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
            int key = entry.getKey();
            int value = entry.getValue();
            result[i * 8] = (byte) (key >>> 24);
            result[i * 8 + 1] = (byte) (key >>> 16);
            result[i * 8 + 2] = (byte) (key >>> 8);
            result[i * 8 + 3] = (byte) key;
            result[i * 8 + 4] = (byte) (value >>> 24);
            result[i * 8 + 5] = (byte) (value >>> 16);
            result[i * 8 + 6] = (byte) (value >>> 8);
            result[i * 8 + 7] = (byte) value;
            i++;
        }
        return result;
    }

    @Override
    public Expr encrypt(String input, SkidMethodNode node, SkidBlock block) {
        byte[] data = input.getBytes(StandardCharsets.UTF_16);
        int predicate = node.getBlockPredicate(block);
        byte[] vmCode = generateVMCode(data.length, predicate);
        
        // Initial encryption
        byte[] keyBytes = Integer.toString(predicate).getBytes();
        for (int i = 0; i < data.length; i++) {
            data[i] ^= keyBytes[i % keyBytes.length];
            data[i] ^= keys[i % keys.length];
            data[i] = (byte) shuffleMap[data[i] & 0xFF];
        }

        return callInjectMethod(
                node.getParent(),
                "decrypt",
                "([B[BI)Ljava/lang/String;",
                generateByteArrayGenerator(node.getParent(), data),
                generateByteArrayGenerator(node.getParent(), vmCode),
                node.getFlowPredicate().getGetter().get(block)
        );
    }

    private byte[] generateVMCode(int length, int key) {
        List<Byte> code = new ArrayList<>();

        // Stack setup
        addInstruction(code, OpcodeType.PUSH, length); // data length
        addInstruction(code, OpcodeType.PUSH, key);    // decryption key

        // Main loop setup
        addInstruction(code, OpcodeType.PUSH, 0); // counter
        int loopStart = code.size();

        // Loop condition
        addInstruction(code, OpcodeType.DUP);          // counter
        addInstruction(code, OpcodeType.LOAD, 0);      // length
        addInstruction(code, OpcodeType.LT);           // compare
        addInstruction(code, OpcodeType.JMPZ, 0);      // exit if done
        int jumpExitPos = code.size() - 1;

        // Decryption logic
        addInstruction(code, OpcodeType.DUP);         // counter
        addInstruction(code, OpcodeType.LOAD_ARR);    // load encrypted byte
        addInstruction(code, OpcodeType.SWAP);
        addInstruction(code, OpcodeType.DUP);
        addInstruction(code, OpcodeType.LOAD_KEY);    // load key byte
        addInstruction(code, OpcodeType.XOR);         // decrypt
        addInstruction(code, OpcodeType.SHUFFLE_REV); // reverse shuffle
        addInstruction(code, OpcodeType.STORE_ARR);   // store decrypted byte

        // Increment counter
        addInstruction(code, OpcodeType.INC);
        addInstruction(code, OpcodeType.JMP, loopStart);

        // Fix exit jump
        code.set(jumpExitPos, (byte)(code.size() - jumpExitPos));

        // Convert to array
        byte[] result = new byte[code.size()];
        for (int i = 0; i < code.size(); i++) {
            result[i] = code.get(i);
        }
        return result;
    }

    private void addInstruction(List<Byte> code, OpcodeType opcode, int... operands) {
        // Find random opcode mapping
        int encodedOpcode = vmOpcodes.entrySet().stream()
                .filter(e -> e.getValue() == opcode.ordinal())
                .findFirst()
                .get()
                .getKey();
        
        code.add((byte) (encodedOpcode & 0xFF));
        for (int operand : operands) {
            code.add((byte) (operand & 0xFF));
        }
    }

    @InjectField(
            value = "keys",
            tags = {InjectFieldTag.RANDOM_NAME}
    )
    private static byte[] vm_keys;

    @InjectField(
            value = "vm_map",
            tags = {InjectFieldTag.RANDOM_NAME}
    )
    private static int[] vm_shuffle;

    @InjectField(
            value = "vm_opcodes",
            tags = {InjectFieldTag.RANDOM_NAME}
    )
    private static int[] vm_op_map;

    @InjectMethod(
            value = "decrypt",
            tags = {InjectMethodTag.RANDOM_NAME}
    )
    private static String decrypt(byte[] data, byte[] code, int key) {
        // VM state
        int[] stack = new int[256];
        int sp = 0;
        int ip = 0;
        long hash = System.nanoTime();

        try {
            while (ip < code.length) {
                // Integrity check
                hash = (hash * 0x5DEECE66DL + 0xBL) & 0xFFFFFFFFFFFFL;
                if ((hash & 0xFF) == 0) {
                    throw new IllegalStateException();
                }

                // Anti-debug check
                if ((ip & 0xF) == 0 && java.lang.management.ManagementFactory
                        .getRuntimeMXBean()
                        .getInputArguments()
                        .toString()
                        .contains("jdwp")) {
                    throw new IllegalStateException();
                }

                // Decode and execute instruction
                int opcode = code[ip++] & 0xFF;
                int type = -1;

                decode: {
                    for (int i = 0; i < vm_op_map.length; i += 8) {
                        if ((vm_op_map[i] << 24 | vm_op_map[i + 1] << 16 |
                                vm_op_map[i + 2] << 8 | vm_op_map[i + 3]) == opcode) {
                            type = vm_op_map[i + 4] << 24 | vm_op_map[i + 5] << 16 |
                                    vm_op_map[i + 6] << 8 | vm_op_map[i + 7];
                        }
                    }
                }

                switch (type) {
                    case 0: // PUSH
                        stack[sp++] = code[ip++] & 0xFF;
                        break;
                    case 1: // POP
                        sp--;
                        break;
                    case 2: // DUP
                        stack[sp] = stack[sp - 1];
                        sp++;
                        break;
                    case 3: // SWAP
                        int temp = stack[sp - 1];
                        stack[sp - 1] = stack[sp - 2];
                        stack[sp - 2] = temp;
                        break;
                    case 4: // LOAD
                        int idx = code[ip++] & 0xFF;
                        stack[sp++] = stack[idx];
                        break;
                    case 5: // LOAD_ARR
                        stack[sp - 1] = data[stack[sp - 1]] & 0xFF;
                        break;
                    case 6: // STORE_ARR
                        data[stack[sp - 2]] = (byte) stack[sp - 1];
                        sp -= 2;
                        break;
                    case 7: // LOAD_KEY
                        idx = stack[sp - 1] % vm_keys.length;
                        stack[sp - 1] = vm_keys[idx] & 0xFF;
                        break;
                    case 8: // XOR
                        stack[sp - 2] ^= stack[sp - 1];
                        sp--;
                        break;
                    case 9: // INC
                        stack[sp - 1]++;
                        break;
                    case 10: // LT
                        stack[sp - 2] = stack[sp - 2] < stack[sp - 1] ? 1 : 0;
                        sp--;
                        break;
                    case 11: // JMP
                        ip = code[ip] & 0xFF;
                        break;
                    case 12: // JMPZ
                        int offset = code[ip] & 0xFF;
                        if (stack[--sp] == 0) {
                            ip = offset;
                        } else {
                            ip++;
                        }
                        break;
                    case 13: // SHUFFLE_REV
                        stack[sp - 1] = reverseShuffleMap(stack[sp - 1]);
                        break;
                }
            }
        } catch (Exception e) {
            // Corrupted execution
            return null;
        }

        return new String(data, StandardCharsets.UTF_16);
    }

    @InjectMethod(
            value = "reverseShuffleMap",
            tags = {InjectMethodTag.RANDOM_NAME}
    )
    private static int reverseShuffleMap(int value) {
        for (int i = 0; i < vm_shuffle.length; i++) {
            if (vm_shuffle[i] == value) {
                return i;
            }
        }
        throw new IllegalStateException();
    }

    private enum OpcodeType {
        PUSH, POP, DUP, SWAP, LOAD, LOAD_ARR, STORE_ARR, LOAD_KEY,
        XOR, INC, LT, JMP, JMPZ, SHUFFLE_REV
    }
}