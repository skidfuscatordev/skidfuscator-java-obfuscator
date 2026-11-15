# Skidfuscator — Usage & Parameters

Skidfuscator is a flow-based Java bytecode obfuscator. It reads a JAR (or APK/DEX),
rebuilds each method into SSA form with MapleIR, applies a pipeline of
transformers, and writes an obfuscated JAR that **preserves the original
class-file version**.

> **Supported inputs:** class-file major versions **52 (Java 8) through 69 (Java 25)**
> are read, obfuscated and written back with the input version preserved. The tool
> itself runs on any JDK 8+ runtime (the distribution is downgraded to Java 8 bytecode).

---

## 1. Running

The distribution is a single fat JAR. The first argument selects a sub-command.

```bash
# Graphical UI (no arguments)
java -jar skidfuscator.jar

# Interactive REPL
java -jar skidfuscator.jar cli

# One-shot obfuscation
java -jar skidfuscator.jar obfuscate <input> [options]

# Build a collated mappings file from a directory
java -jar skidfuscator.jar mappings <directory> [options]
```

On the very first obfuscation run, Skidfuscator downloads a private JDK 17 image
into `~/.ssvm/jdk` (used only by the internal SSVM virtual machine for the
"pure"/number obfuscation step — it is independent of the JDK you run the tool on
and of your application's Java version).

---

## 2. `obfuscate` command

```bash
java -jar skidfuscator.jar obfuscate <input> [options]
```

### Positional argument

| Argument  | Required | Description |
|-----------|----------|-------------|
| `<input>` | **yes**  | File to obfuscate. `.jar` (and `.apk` / `.dex`, converted via dex2jar) are accepted. |

### Options

| Option | Alias | Type | Default | Description |
|--------|-------|------|---------|-------------|
| `--output` | `-o` | file | `<input>-out.jar` | Output JAR location. |
| `--runtime` | `-rt` | file | running JVM's `jmods` (or `lib/rt.jar` on Java 8) | Runtime library used to resolve JDK classes. Point it at a JDK's `jmods` directory (Java 9+) or `rt.jar` (Java 8) to obfuscate against a specific Java version. |
| `--libs` | `-li` | folder | – | Folder containing the application's dependency JARs, so references resolve correctly. |
| `--config` | `-cfg` | file | built-in `defaultConfig.hocon` | HOCON config file selecting and tuning transformers (see §4). |
| `--exempt` | `-ex` | file | – | **Deprecated.** Legacy exemption file; it is auto-migrated to a `config.hocon` on use. Prefer `--config`. |
| `--phantom` | `-ph` | flag | off | Enable JPhantom computation of missing/phantom classes when the classpath is incomplete. |
| `--notrack` | `-notrack` | flag | off (analytics on) | Opt out of anonymous usage analytics. |
| `--debug` | `-dbg` | flag | off | Internal debugging. Marked "do not use". |
| `--fuckit` | `-fuckit` | flag | off | Force-continue past recoverable errors ("fuck it" mode). Marked "do not use". |

### Examples

```bash
# Simplest form — output goes to <input>-out.jar
java -jar skidfuscator.jar obfuscate app.jar

# Custom output + dependency folder + custom config, analytics off
java -jar skidfuscator.jar obfuscate app.jar \
     -o app-obf.jar \
     -li ./libs \
     -cfg ./skid.hocon \
     --notrack

# Obfuscate against a specific JDK's runtime (e.g. a JDK 21 install)
java -jar skidfuscator.jar obfuscate app.jar -rt /path/to/jdk-21/jmods
```

---

## 3. `mappings` command

Collates a directory of mapping/library descriptors into a single mappings file.

```bash
java -jar skidfuscator.jar mappings <directory> -o mappings.json
```

| Argument / Option | Alias | Description |
|-------------------|-------|-------------|
| `<directory>` | – | Directory scanned to build the mappings file. |
| `--output` | `-o` | Output mappings file location. |

---

## 4. Configuration file (HOCON)

Pass a config with `-cfg`. Anything omitted falls back to the built-in
[`defaultConfig.hocon`](../dev.skidfuscator.commons/src/main/resources/defaultConfig.hocon).
Every transformer block accepts `enabled` and an `exempt` list; some add extra keys.

### Top-level keys

| Key | Type | Description |
|-----|------|-------------|
| `exempt` | list | Global exemption patterns (see §5). |
| `libs` | list | Additional library paths (same role as `-li`). |

### Transformer blocks

| Block | Extra keys | Purpose |
|-------|-----------|---------|
| `stringEncryption` | `type: STANDARD` | Encrypts string constants. |
| `numberEncryption` | – | Encrypts integer/number constants. |
| `intAnnotationEncryption` | – | Encrypts integer values inside annotations. |
| `stringAnnotationEncryption` | – | Encrypts string values inside annotations. |
| `exceptionReturn` | – | Return-via-exception obfuscation. |
| `flowCondition` | – | Opaque-predicate conditional flow (GEN3). |
| `flowException` | `strength: WEAK \| GOOD \| AGGRESSIVE` | Exception-based control-flow mangling. |
| `flowRange` | – | Range-based opaque predicates. |
| `flowSwitch` | – | Switch-dispatch flow flattening. |
| `flowFactoryMaker` | – | Factory-based flow indirection. |
| `outliner` | – | Outlines code into synthetic methods. |
| `ahegao` | – | Injects decoy/junk methods and metadata. |
| `interprocedural` | – | Interprocedural opaque-predicate hardening. |
| `native` | – | Native-stub related transformation (off by default). |
| `driver` | `path: "skid/Driver"` | Flow driver class (off by default). |
| `reference` | – | Reference obfuscation (off by default). |
| `fileCrasher` | – | Injects anti-decompiler/crasher metadata (off by default). |
| `classRenamer` | `type`, `prefix`, `chars`, `depth` | Renames classes (off by default). |
| `methodRenamer` | `type`, `chars`, `depth` | Renames methods (off by default). |
| `fieldRenamer` | `type: ALPHABETICAL \| CUSTOM` | Renames fields (off by default). |

### Minimal example

```hocon
exempt: [
    "class{^com/example/api/.*}"   # keep the public API readable
]

libs: []

stringEncryption { enabled: true, type: STANDARD, exempt: [] }
numberEncryption { enabled: true, exempt: [] }
flowCondition    { enabled: true, exempt: [] }
flowException    { enabled: true, strength: AGGRESSIVE, exempt: [] }
flowSwitch       { enabled: true, exempt: [] }
ahegao           { enabled: true, exempt: [] }
driver           { enabled: false }
```

---

## 5. Exemptions

`exempt` entries (global or per-transformer) are matchers that skip classes,
methods or fields from obfuscation. They use a small typed DSL:

```hocon
exempt: [
    "class{^com/example/keepme/.*}",       # by class (internal name, regex)
    "method{^com/example/Main\\.main.*}"   # by method
]
```

A global `exempt` applies to every transformer; a transformer's own `exempt`
list applies only to that pass.

---

## 6. Notes

- **Class-file version is preserved.** A Java 21 input produces a Java 21 output; a
  Java 25 input produces a Java 25 output. Skidfuscator does not upgrade or downgrade
  the bytecode version of your classes.
- **Automatic dependency download / phantom resolution** (`-ph`) helps when some
  library classes are missing from the classpath; for best results provide them via
  `-li`.
- **Analytics** are on by default; pass `--notrack` to disable.
