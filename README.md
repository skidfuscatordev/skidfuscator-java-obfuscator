<p align="center">
  <img width="50%" height="50%" src="https://i.imgur.com/KzXiF67.png">
  <br>
  <a><img alt="Server Version" src="https://img.shields.io/badge/Server%20Version-J8%20J16-blue"></a>
  <a><img alt="Api Type" src="https://img.shields.io/badge/API-MapleIR-blue"></a>
  <a><img alt="Authors" src="https://img.shields.io/badge/Authors-Ghast-blue"></a>
  <a><img alt="Issues" src="https://img.shields.io/github/issues/terminalsin/skidfuscator-java-obfuscator"></a>
  <a><img alt="Forks" src="https://img.shields.io/github/forks/terminalsin/skidfuscator-java-obfuscator"></a>
  <a><img alt="Stars" src="https://img.shields.io/github/stars/terminalsin/skidfuscator-java-obfuscator"></a>
  
  <h3 align="center">Join the discord: https://discord.gg/QJC9g8fBU9</h3>
</p>


# What is Skidfuscator?
Skidfuscator is a proof of concept obfuscation tool designed to take advantage of SSA form to optimize and obfuscate Java bytecode
code flow. This is done via intra-procedural passes each designed to mingle the code in a shape where neither the time complexity
neither the space complexity suffers from a great loss. To achieve the such, we have modeled a couple of well known tricks to 
add a significant strength to the obfuscation whilst at the same time retaining a stable enough execution time.

This project is **___not completed___**. This is a proof of concept I've been working on for a while. As far as I could tell, there are
some serious flaws with parameter injection. 

# Features 

Here are all the cool features I've been adding to Skidfuscator. It's a fun project hence don't expect too much from it. It's purpose is
not to be commercial but to inspire some more clever approaches to code flow obfuscation, especially ones which make use of SSA and CFGs

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

- [x] Fake Exception GEN3 Obfuscation
- [ ] Block Mangling GEN3 Obfuscation
- [x] Fake Jump GEN3 Obfuscation
- [x] Switch Mangling GEN3 Obfuscation
- [ ] Loop Mangling GEN3 Obfuscation
- [ ] Arithmetic Obfuscation GEN3 Obfuscation
- [ ] Exception Mangling GEN3 Obfuscation
- [ ] Condition Mangling GEN3 Obfuscation

### **NEW** Switch Mutation
![Graph](https://i.imgur.com/yPjFC8k.png)

### Fake exceptions
![Graph](https://i.imgur.com/bJcTNHm.png)

### Fake jumps
![Graph](https://i.imgur.com/780UIIc.png)

# Credits

## Libraries used
- [Maple IR and the Team](https://github.com/LLVM-but-worse/maple-ir)
- [ASM](https://gitlab.ow2.org/asm/asm)
- [AntiDumper by Sim0n](https://github.com/sim0n/anti-java-agent/)

## Inspired from
- [Soot](https://github.com/soot-oss/soot)
- [Zelix KlassMaster](https://zelix.com)
