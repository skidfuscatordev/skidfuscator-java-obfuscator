![header](https://github.com/user-attachments/assets/65046709-27c3-417f-8053-3a6f8d5ea29d)

---
<p align="center">
  <h3 align="center">
    🗣️ Discord: https://discord.gg/QJC9g8fBU9  📚  Wiki: https://skidfuscator.dev/docs/
  </h3>
  <h3 align="center">
    🏢 Enterprise/Custom Version: https://skidfuscator.dev/pricing
  </h3>
</p>

---

> [!WARNING]
> This product is provided as-is, licensed under [MIT](/LICENSE). We will not provide any support, nor take liability for
> any reversal, deobfuscation, or impact this tool may have on your software. If you seek to protect sensitive applications
> for commercial use, we recommend you contact us at mastermind [at] zenrho.net or visit https://zenrho.com 


* [🚀 Quickstart](#-quickstart)
    + [🔥 Homebrew (macOS)](#-homebrew-macos)
    + [🔥 Bash (Linux/macOS)](#-bash-linuxmacos)
    + [🔥 Powershell [Admin required] (Windows)](#-powershell-admin-required-windows)
- [🕵️ What is Skidfuscator?](#-what-is-skidfuscator)
- [✨ Features](#-features)
    + [1. Automatic Dependency Downloading](#1-automatic-dependency-downloading)
    + [2. Smart Recovery](#2-smart-recovery)
    + [3. Auto Configuration](#3-auto-configuration)
    + [4. Flow Obfuscation (GEN3)](#4-flow-obfuscation--gen3-)
    + [5. Advanced Obfuscation Methods](#5-advanced-obfuscation-methods)
    + [6. Optimization Out-of-the-Box](#6-optimization-out-of-the-box)
- [🔥 Third Generation Flow](#-third-generation-flow)
    + [Flow Obfuscation](#flow-obfuscation)
    + [Encryption Obfuscation](#encryption-obfuscation)
    + [Structure Obfuscation](#structure-obfuscation)
    + [Miscelleanous](#miscelleanous)
- [Credits](#credits)
  * [Libraries used](#libraries-used)
  * [Inspired from](#inspired-from)

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
>}
> ```

You can download Skidfuscator [here](https://github.com/skidfuscatordev/skidfuscator-java-obfuscator/releases) and run it directly.

<details>
<summary>CLI Usage/Install</summary>
  
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
</details>

## 🕵️ What is Skidfuscator?
Skidfuscator is a ~~proof of concept~~ ✨ **production** ✨ grade obfuscation tool designed to take advantage of SSA form to optimize and obfuscate Java bytecode
code flow. This is done via intra-procedural passes each designed to mingle the code in a shape where neither the time complexity
neither the space complexity suffers from a great loss. To achieve the such, we have modeled a couple of well known tricks to 
add a significant strength to the obfuscation whilst at the same time retaining a stable enough execution time.

Skidfuscator is now feature-complete and continues to be actively maintained with several new obfuscation techniques aimed at hardening your code against reverse engineering.

<details>
  <summary>Research Poster (Finalist CCSCNE 2024)</summary>
  
![Classic Landscape 1 (3) (1)](https://github.com/skidfuscatordev/skidfuscator-java-obfuscator/assets/30368557/9ab9a2ab-8df7-4e62-a711-4df5f3042947)
</details>

## ✨ Features 

Skidfuscator is rich in over 10+ features. Our objective: **be one click, you're protected.** We provide a **GUI**, over **+10** configurable transfomers, we have failsafes and automatic tools to tailor your configuration to you with ease. 
<details><summary><h4>👉 Expand features</h4></summary>
  
### 1. Automatic Dependency Downloading
Skidfuscator intelligently identifies and downloads missing dependencies needed for your project, minimizing manual configuration. Known frameworks such as Bukkit are automatically handled, streamlining setup.

https://github.com/user-attachments/assets/9c349e09-01da-4073-be69-f00211add72a

### 2. Smart Recovery
In the event of errors or failed obfuscation, Skidfuscator implements a recovery system that intelligently resolves conflicts and provides suggestions to fix issues. This ensures minimal disruption in your development workflow.

https://github.com/user-attachments/assets/d71f3a10-ebac-466c-9e8e-3bfcaf5177d5

### 3. Auto Configuration
Skidfuscator comes with built-in presets for common configurations, allowing quick setup without needing to manually tweak every aspect of the obfuscation process. For advanced users, all settings remain fully customizable.

https://github.com/user-attachments/assets/fb6a5ac1-a739-4c83-a340-40e312016947

### 4. Flow Obfuscation (GEN3)
Skidfuscator introduces third-generation control flow obfuscation (Flow GEN3), which scrambles method logic and makes the control flow harder to understand. This method introduces opaque predicates and complex flow redirections, hindering static and dynamic analysis.
### 5. Advanced Obfuscation Methods
Comes with all sorts of advanced obfuscation methodologies only seen in modern obfuscators, such as Zelix KlassMaster. Skidfuscator is designed to be hyper-resilient and best of its field, for free.
### 6. Optimization Out-of-the-Box
Skidfuscator is built to ensure that obfuscation does not degrade your application’s runtime performance. By leveraging SSA and CFG-based transformations, it provides obfuscation that’s highly optimized to maintain both time and space complexity.

Here are all the cool features I've been adding to Skidfuscator. It's a fun project hence don't expect too much from it. It's purpose is
not to be commercial but to inspire some more clever approaches to code flow obfuscation, especially ones which make use of SSA and CFGs

![Cool gif](https://i.ibb.co/4MQnj4V/FE185-E3-B-0-D0-D-4-ACC-81-AA-A4862-DF01-FA3.gif)
</details>

## 🔥 Third Generation Flow

Skidfuscator boasts the **most complex**, research-backed flow obfuscation in the JVM community. Our flow obfuscation outperforms competitors such as [Zelix Klassmaster](https://zelix.com), [DexGuard](), and various others. And yes, we're open-source! 

<details><summary><h4>👉 Expand transformers</h4></summary>

### Flow Obfuscation
| **Feature**                          | **Edition**           | **Description**                                   |
|--------------------------------------|------------------------|---------------------------------------------------|
| Bogus Exception Flow                 | Community              | Adds fake exception handling to confuse reverse engineers. |
| Bogus Condition Flow                 | Community              | Introduces fake conditions to disrupt program analysis. |
| **NEW** ✨ Pure Function Hashing Flow | Community               | Hashes pure functions to obfuscate their flow and outputs. |
| Switch Flow                          | Community               | Uses switch statements to obscure logic paths.    |
| Factory Initiation Flow              | Community               | Obfuscates factory patterns, adding complexity to instance creation. |
| Integer Return Flow                  | [**Enterprise**](https://skidfuscator.dev/pricing) | Obfuscates integer return values to mislead reverse engineering. |
| Exception Return Flow                | [**Enterprise**](https://skidfuscator.dev/pricing) | Utilizes exceptions as a return flow to confuse the control path. |

### Encryption Obfuscation

| **Feature**                          | **Edition**           | **Description**                                   |
|--------------------------------------|------------------------|---------------------------------------------------|
| String Encryption                    | Community  | Encrypts strings to prevent static analysis.      |
| Annotation Encryption                | [**Enterprise**](https://skidfuscator.dev/pricing)  | Encrypts annotations to protect metadata from tampering. |
| Reference Encryption                 | [**Enterprise**](https://skidfuscator.dev/pricing)  | Encrypts object references to add another layer of security. |

### Structure Obfuscation
| **Feature**                          | **Edition**           | **Description**                                   |
|--------------------------------------|------------------------|---------------------------------------------------|
| Field Renamer                        | [**Enterprise**](https://skidfuscator.dev/pricing)   | Renames fields to meaningless names to obscure their purpose. |
| Method Renamer                       | [**Enterprise**](https://skidfuscator.dev/pricing)   | Renames methods to make reverse engineering more challenging. |
| Class Renamer                        | [**Enterprise**](https://skidfuscator.dev/pricing)   | Renames classes to break readability and tooling compatibility. |
| Mixin Support                        | [**Enterprise**](https://skidfuscator.dev/pricing)   | Adds support for obfuscating mixin-based systems. |
| Spigot Plugin Support                | [**Enterprise**](https://skidfuscator.dev/pricing)   | Obfuscates Spigot plugins for enhanced security.  |

### Miscelleanous 
| **Feature**                          | **Edition**           | **Description**                                   |
|--------------------------------------|------------------------|---------------------------------------------------|
| Ahegao Trolling                      | Community              | Adds humorous or trolling elements to deter casual inspection. |
| Driver Protection                    | Community              | Protects drivers from tampering or reverse engineering. |
| **New** Outlining Obfuscation        | [**Enterprise**](https://skidfuscator.dev/pricing)           | Segments code into separate outlined functions for obfuscation. |
| Native Driver Protection             | [**Enterprise**](https://skidfuscator.dev/pricing)           | Protects native drivers at the OS level for added security. |
</details>

## Credits

We'd like to extend our many thanks to the many individuals who have financially supported this project, supervised its research, or reported bugs. All contributions have curated this tool to achieve a production grade, and I could not be more thankful.

## Libraries used
- [Maple IR and the Team](https://github.com/LLVM-but-worse/maple-ir)
- [ASM](https://gitlab.ow2.org/asm/asm)
- [AntiDumper by Sim0n](https://github.com/sim0n/anti-java-agent/)
- [Recaf by Col-E](https://github.com/Col-E/Recaf)
- [Some works by xDark](https://github.com/xxDark)

## Inspired from
- [Soot](https://github.com/soot-oss/soot)
- [Zelix KlassMaster](https://zelix.com)
