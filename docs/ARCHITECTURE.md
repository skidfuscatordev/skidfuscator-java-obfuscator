# Skidfuscator — How It Works

This document describes the end-to-end obfuscation pipeline: how a JAR flows
from input to obfuscated output, and which components are responsible for each
stage.

## Pipeline overview

```mermaid
flowchart TD
    subgraph ENTRY["Entry point"]
        CLI["java -jar skidfuscator.jar obfuscate INPUT"] --> SESS["SkidfuscatorSession<br/>(input, output, libs, runtime, config)"]
        SESS --> RUN["Skidfuscator.run()"]
    end

    subgraph IMPORT["1 - Import and resolution"]
        RUN --> INJAR["_importClasspath<br/>JarInputSource reads input<br/>APK/DEX via dex2jar"]
        RUN --> INJVM["_importJvm<br/>JDK jmods (Java 9+)<br/>or rt.jar (Java 8)"]
        INJAR --> SRC["SkidApplicationClassSource"]
        INJVM --> LIB["LibraryClassSource(s)"]
        LIBS["-li libs / mappings"] --> LIB
        PHANTOM["-ph JPhantom<br/>synthesize missing classes"] --> LIB
        SRC --> TREE["Build ClassTree / hierarchy"]
        LIB --> TREE
    end

    subgraph IR["2 - Lift to SSA IR"]
        TREE --> READ["ClassHelper + ASM<br/>read class (SKIP_FRAMES, JSR inline)"]
        READ --> CFG["MapleIR builds<br/>SSA Control Flow Graph per method"]
    end

    subgraph VM["3 - Opaque-predicate engine"]
        CFG --> SSVM["SSVM boots internal VM<br/>on downloaded JDK 17"]
        SSVM --> HASH["VmHashTransformer<br/>computes pure/number hashes"]
    end

    subgraph XFORM["4 - Transformer pipeline: GEN3 flow, event-driven"]
        HASH --> T1["Interprocedural hardening"]
        T1 --> T2["Number encryption"]
        T2 --> T3["Flow: Switch / Condition / Exception / Range"]
        T3 --> T4["String encryption"]
        T4 --> T5["Hash checks: String equals / instanceof / type"]
        T5 --> T6["SDK injection + Ahegao decoys"]
        T6 --> PRED["IntegerBlockPredicateRenderer<br/>injects opaque predicates (seeds/keys)"]
    end

    subgraph OUT["5 - Emit"]
        PRED --> WRITE["ClassWriter COMPUTE_FRAMES / COMPUTE_MAXS<br/>getCommonSuperClass from own ClassTree"]
        WRITE --> DUMP["CompleteResolvingJarDumper /<br/>PhantomResolvingJarDumper"]
        DUMP --> OUTJAR["Obfuscated JAR<br/>original class-file version preserved"]
    end
```

## Stage notes

1. **Import & resolution.** The input JAR (or APK/DEX) is read into a
   `SkidApplicationClassSource`. The JDK runtime is imported as library classes —
   from the running JVM's `jmods` on Java 9+, or `rt.jar` on Java 8 — so references
   to standard-library types resolve. Extra dependencies come from `-li`; missing
   classes can be synthesized with JPhantom (`-ph`).

2. **Lift to SSA IR.** Each class is read with ASM (via `ClassHelper`, inlining JSR
   and skipping frames) and each method is lifted into MapleIR's **SSA control-flow
   graph**. Working in SSA form is what lets the flow transforms mangle control flow
   without blowing up time/space complexity.

3. **Opaque-predicate engine (SSVM).** For the "pure"/number obfuscation,
   Skidfuscator boots an embedded [SSVM](https://github.com/terminalsin/SSVM)
   virtual machine against a downloaded **JDK 17** image (cached in `~/.ssvm/jdk`).
   It executes small pure functions to derive deterministic hash values used as
   opaque predicates. This internal VM is pinned to JDK 17 and is independent of
   both the JVM running Skidfuscator and the Java version of the input; candidates
   it cannot load are skipped gracefully.

4. **Transformer pipeline.** An event-driven pipeline (`EventBus`) runs the
   configured transformers over every class/method: interprocedural hardening,
   number & string encryption, the GEN3 flow passes (switch, condition, exception,
   range), hash-based checks, SDK injection and decoys. The
   `IntegerBlockPredicateRenderer` then threads opaque predicates (per-method seeds
   and keys) through the graph. Each pass is toggled and tuned via the HOCON config
   (see [USAGE.md](USAGE.md)).

5. **Emit.** Transformed methods are written back with ASM. Frame/stack computation
   resolves common supertypes from Skidfuscator's **own** class tree (not the host
   classloader), so it works for any supported Java version. The dumper writes the
   classes, resources and the injected runtime SDK into the output JAR, **preserving
   the original class-file version** of every class.

## Module map

| Stage | Module(s) |
|-------|-----------|
| CLI / GUI / session | `dev.skidfuscator.client.standalone` |
| Orchestration & transformers | `dev.skidfuscator.obfuscator` |
| SSA IR, class I/O (ASM) | `org.mapleir.parent/*` (MapleIR, `modasm`) |
| Phantom resolution | `dev.skidfuscator.obfuscator.dependanalysis` + JPhantom |
| Pure/number VM | `dev.skidfuscator.obfuscator.pureanalysis` + SSVM (`io.github.terminalsin:SSVM`) |
| Injected runtime SDK | `dev.skidfuscator.sdk` |

See [USAGE.md](USAGE.md) for the command-line parameters and configuration reference.
