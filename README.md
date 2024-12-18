<p align="center">
  <img width="15%" height="15%" src="https://i.imgur.com/RgDz1Qn.png" href="https://github.com/terminalsin/skidfuscator-java-obfuscator/releases">
  <br>
</p>
<p align="center">
  Skidfuscator: Obfuscation like never seen before.
</p>
<p align="center">
  <a><img alt="Api Type" src="https://img.shields.io/badge/API-MapleIR-blue"></a>
  <a><img alt="Authors" src="https://img.shields.io/badge/Authors-Ghast-blue"></a>
  <a><img alt="Issues" src="https://img.shields.io/github/issues/terminalsin/skidfuscator-java-obfuscator"></a>
  <a><img alt="Forks" src="https://img.shields.io/github/forks/terminalsin/skidfuscator-java-obfuscator"></a>
  <a><img alt="Stars" src="https://img.shields.io/github/stars/terminalsin/skidfuscator-java-obfuscator"></a> 
  <h3 align="center">
    Join the discord: https://discord.gg/QJC9g8fBU9
  </h3>
  <h3 align="center">
    Wiki: https://skidfuscator.dev/docs/
  </h3>
</p>

---
## 🚀 Quickstart

> [!TIP]
> If you are using Gradle, consider using our [Gradle plugin](https://github.com/skidfuscatordev/skidfuscator-gradle-plugin) for easy integration.
> ```java
> plugins {
>    id("io.github.skidfuscatordev.skidfuscator") version "0.1.3"
> }
> 
> skidfuscator {
>   // Configure the plugin here
>   skidfuscatorVersion = "latest"
>   exempt = ["com/example/SomeClass"]
>
>   transformers {
>       interprocedural {
>           enabled = true
>           exempt = ["com/example/IgnoredClass"]
>       }
>       stringEncryption {
>           type = "STANDARD"
>           enabled = true
>       }
>   }
>}
> ```

You can download Skidfuscator [here](https://github.com/skidfuscatordev/skidfuscator-java-obfuscator/releases) and run it directly using:
```
java -jar skidfuscator.jar obfuscate <path to your jar>
```

Skidfuscator uses a config system, which allows you to customize your obfuscation. We try to automatically download all compatible libraries, but some may slip through the cracks. The Gradle plugin is a work in progress. For now, use:
```
java -jar skidfuscator.jar obfuscate <path to your jar> -li=<path to folder with all libs>
```

### 🔥 Homebrew (macOS)
```
brew tap skidfuscatordev/skidfuscator
brew install skidfuscator
```

### 🔥 Bash (Linux/macOS)
```
curl -sL https://raw.githubusercontent.com/skidfuscatordev/skidfuscator-java-obfuscator/refs/heads/master/scripts/install.sh | bash
```
### 🔥 Powershell [Admin required] (Windows)
```
iex "& { $(iwr -useb https://raw.githubusercontent.com/skidfuscatordev/skidfuscator-java-obfuscator/refs/heads/master/scripts/install.ps1) }"
```

## 🕵️ What is Skidfuscator?
Skidfuscator is a proof of concept obfuscation tool designed to take advantage of SSA form to optimize and obfuscate Java bytecode
code flow. This is done via intra-procedural passes each designed to mingle the code in a shape where neither the time complexity
neither the space complexity suffers from a great loss. To achieve the such, we have modeled a couple of well known tricks to 
add a significant strength to the obfuscation whilst at the same time retaining a stable enough execution time.

Skidfuscator is now feature-complete and continues to be actively maintained with several new obfuscation techniques aimed at hardening your code against reverse engineering.

![Classic Landscape 1 (3) (1)](https://github.com/skidfuscatordev/skidfuscator-java-obfuscator/assets/30368557/9ab9a2ab-8df7-4e62-a711-4df5f3042947)

# ✨ Features 

### 1. Automatic Dependency Downloading
Skidfuscator intelligently identifies and downloads missing dependencies needed for your project, minimizing manual configuration. Known frameworks such as Bukkit are automatically handled, streamlining setup.

![autodepend](https://github.com/user-attachments/assets/958349aa-582b-4751-88e1-ed61408d24b8)

### 2. Smart Recovery
In the event of errors or failed obfuscation, Skidfuscator implements a recovery system that intelligently resolves conflicts and provides suggestions to fix issues. This ensures minimal disruption in your development workflow.

![FocuSee_Project_2024-12-18_03-32-25-ezgif com-optimize](https://github.com/user-attachments/assets/8d1ec8e7-3f07-4da3-b4ef-c3b34cb1931b)

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

### Flow Obfuscation
- [x] Bogus Exception Flow
- [x] Bogus Condition Flow
- [x] **NEW** ✨ Pure Function Hashing Flow
- [x] Switch Flow
- [x] Factory Initiation Flow (**Enterprise**)
- [x] Integer Return Flow (**Enterprise**)
- [x] Exception Return Flow (**Enterprise**)

### Encryption Obfuscation
- [x] String Encryption
- [x] Annotation Encryption (**Enterprise**)
- [x] Reference Encryption (**Enterprise**)

### Structure Obfuscation
- [x] Field Renamer (**Enterprise**)
- [x] Method Renamer (**Enterprise**)
- [x] Class Renamer (**Enterprise**)
- [x] Mixin Support (**Enterprise**)
- [x] Spigot Plugin Support (**Enterprise**)

### Miscelleanous 
- [x] Ahegao Trolling
- [x] Driver Protection
- [x] **Experimental** Outlining Obfuscation (**Enterprise**)
- [x] Native Driver Protection (**Enterprise**)

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
