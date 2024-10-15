<p align="center">
  <img width="15%" height="15%" src="https://i.imgur.com/RgDz1Qn.png" href="https://github.com/terminalsin/skidfuscator-java-obfuscator/releases">
  <br>
</p>
<p align="center">
  Skidfuscator: Obfuscation like never seen before.
</p>
<p align="center">
  <a><img alt="Server Version" src="https://github.com/terminalsin/skidfuscator-java-obfuscator/actions/workflows/maven.yml/badge.svg?branch=master"></a>
  <a><img alt="Api Type" src="https://img.shields.io/badge/API-MapleIR-blue"></a>
  <a><img alt="Authors" src="https://img.shields.io/badge/Authors-Ghast-blue"></a>
  <a><img alt="Issues" src="https://img.shields.io/github/issues/terminalsin/skidfuscator-java-obfuscator"></a>
  <a><img alt="Forks" src="https://img.shields.io/github/forks/terminalsin/skidfuscator-java-obfuscator"></a>
  <a><img alt="Stars" src="https://img.shields.io/github/stars/terminalsin/skidfuscator-java-obfuscator"></a> 
  <h3 align="center">
    Join the discord: https://discord.gg/QJC9g8fBU9
  </h3>
</p>

---

# 🕵️ What is Skidfuscator?
Skidfuscator is a proof of concept obfuscation tool designed to take advantage of SSA form to optimize and obfuscate Java bytecode
code flow. This is done via intra-procedural passes each designed to mingle the code in a shape where neither the time complexity
neither the space complexity suffers from a great loss. To achieve the such, we have modeled a couple of well known tricks to 
add a significant strength to the obfuscation whilst at the same time retaining a stable enough execution time.

Skidfuscator is now feature-complete and continues to be actively maintained with several new obfuscation techniques aimed at hardening your code against reverse engineering.

![Classic Landscape 1 (3) (1)](https://github.com/skidfuscatordev/skidfuscator-java-obfuscator/assets/30368557/9ab9a2ab-8df7-4e62-a711-4df5f3042947)

# ✨ Features 

### 1. Automatic Dependency Downloading
Skidfuscator intelligently identifies and downloads missing dependencies needed for your project, minimizing manual configuration. Known frameworks such as Bukkit are automatically handled, streamlining setup.
### 2. Smart Recovery
In the event of errors or failed obfuscation, Skidfuscator implements a recovery system that intelligently resolves conflicts and provides suggestions to fix issues. This ensures minimal disruption in your development workflow.
### 3. Auto Configuration
Skidfuscator comes with built-in presets for common configurations, allowing quick setup without needing to manually tweak every aspect of the obfuscation process. For advanced users, all settings remain fully customizable.
### 4. Flow Obfuscation (GEN3)
Skidfuscator introduces third-generation control flow obfuscation (Flow GEN3), which scrambles method logic and makes the control flow harder to understand. This method introduces opaque predicates and complex flow redirections, hindering static and dynamic analysis.
### 5. Advanced Obfuscation Methods
Comes with all sorts of advanced obfuscation methodologies only seen in modern obfuscators, such as Zelix KlassMaster. Skidfuscator is designed to be hyper-resilient and best of its field, for free.
### 6. Optimization Out-of-the-Box
Skidfuscator is built to ensure that obfuscation does not degrade your application’s runtime performance. By leveraging SSA and CFG-based transformations, it provides obfuscation that’s highly optimized to maintain both time and space complexity.

Here are all the cool features I've been adding to Skidfuscator. It's a fun project hence don't expect too much from it. It's purpose is
not to be commercial but to inspire some more clever approaches to code flow obfuscation, especially ones which make use of SSA and CFGs

![Cool gif](https://i.ibb.co/4MQnj4V/FE185-E3-B-0-D0-D-4-ACC-81-AA-A4862-DF01-FA3.gif)

## Third Generation Flow

What is third generation flow obfuscation? Well, contrary to Zelix's [second generation flow obfuscation](https://www.zelix.com/klassmaster/featuresFlowObfuscation.html), we use an even more complex system with private and public seeds. Here's 
how it works:

<br>
<br>

![Exampel](https://i.imgur.com/j2tZavr.png)

<sub>_Graph representing the two different approaches towards flow obfuscation between Zelix (17.0) and Skidfuscator (0.0.1)_</sub>
<br>
<br>
<br>

We currently are working on a variety of ways to approach this system using various lightweight obfuscation methods. Here are the current ones
to date:
Here are the features:

| Feature | Type | Description | Status |
| --- | --- | --- | --- |
| `Flow GEN3` | Flow (Community) | Obfuscates methods using the GEN3 Obfuscation methodology | ✅ |
| `Bogus Jump` | Flow (Community) | Invalid jump to some random generated code to prevent skidding | ✅ |
| `Bogus Exception`| Flow (Community) | Invalid jump to some random generated exception | ✅ |
| `Mangled Jump` | Flow (**Enterprise**) | Mutation to the jump condition to make it appear more complex than it actually is | ❌ |
| `Exception Jump` | Flow (**Enterprise**) | Changes done to flow semantics by forcing an exception then handling all the code in the catch clause | ❌ |
| `Exception Return`| Flow (**Enterprise**) | Throw an exception with the value and catch it as opposed to returning it (Very heavy) | ❌ |
| `Strong Opaque Predicate` | Flow (Community) | Use heredity and method invocation to pass a predicate as opposed to declaring it at the beginning of the CFG | ✅ |
| `Method Inlining` | Flow (**Enterprise**) | Inline uncommon methods which aren't too big | ❌ |
| `Method Outlining` | Flow (**Enterprise**) | Outline some non-sensitive blocks | ❌ |
| `Loop Unrolling` | Flow (**Enterprise**) | Rewrite some loops instructions into continuous segments if the loop limit can be pre-determined | ❌ |
| `Flattening` | Flow (Community) | Use a dispatcher method to harden the semantics of some block ranges (do not use entire method) | ⚠️ |
| `String Encryption` | String | Encrypt the strings using the opaque predicate | ✅ |
| `Reference Encryption` | Reference | Encrypt the reference calls using InvokeDynamic using the opaque predicate | ❌ |
| `Reference Proxying` | Reference | Proxy references using a builder pattern OR dispatcher classes (mostly for initialisation) | ❌ |
# Credits

## Libraries used
- [Maple IR and the Team](https://github.com/LLVM-but-worse/maple-ir)
- [ASM](https://gitlab.ow2.org/asm/asm)
- [AntiDumper by Sim0n](https://github.com/sim0n/anti-java-agent/)
- [Recaf by Col-E](https://github.com/Col-E/Recaf)
- [Some works by xDark](https://github.com/xxDark)

## Inspired from
- [Soot](https://github.com/soot-oss/soot)
- [Zelix KlassMaster](https://zelix.com)
