<p align="center">
  <img width="70%" height="70%" src="https://i.imgur.com/kY8ilvC.gif">
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

### ***NEW*** Number Mutation
![Graph](https://i.imgur.com/XjUFdRU.png)

### Switch Mutation
![Graph](https://i.imgur.com/yPjFC8k.png)

### Fake exceptions
![Graph](https://i.imgur.com/bJcTNHm.png)

### Fake jumps
![Graph](https://i.imgur.com/780UIIc.png)


### Todo
- [ ] (In progress) Converting block creation to a factory style to give us more leniency to play around with stmts and stuff without having to wrap em 
- [ ] Convert method nodes and modasm to factory style too for that same reason
- [ ] Create a proper util which allows for easy addition, editing and so and forth of the IR. For example, a proper util which can find edges. Perhaps also add a reference to the apropriate jump edge linked in the stmt and vice versa? For the util I envision doing something such as Build.new().Integer(<params>).create() or Build.new().IllegalStateException(<params>).create() or Build.invokevirtual(method).build() or Build.jump(target) or Build.if(<condition>).jump(<target>).build() or Build.if(<condition>).invokevirtual(method).store().build(). Depending on what we want it to return, we give it multiple choices, making it easier to create obfuscation and stuff
- [ ] Begin implementation of LLVM compiler using the sorta-LLVM style stmt structure we got. We need to override them all and add a LLVM compile method to compile them to LLVM bytecode. Once that's done in the future we'll be able to create a website which runs that shit in LLVM-clang to cross compile on our backend, making it a smooth experience for customers
- [ ] Add proper parameter obfuscation with a properly done seeding system. My idea is that seeds should vary in type instead of being consistent eg one seed will be passed as a double then will be transformed using it's hashcode and stuff.
- [ ] Add a proper invocation resolver which caches everything pre-emptively. Make sure to make it support exclusions and stuff
- [ ] Optimize MapleIR's class heredity construction. Pretty weak sauce rn

## Examples
  
### Builder example
  
```java
Builder
  .invokevirtual(method)        // Invokes the method and adds it to the stack. We have to use the stack value before exiting the builder for a stmt
  .asImplicitInt()              // Converts the builder into an integer builder, allowing us to use arithmetic operations. We could also just make this refer 
                                // to the hashcode function instead if it isn't an integer
  .add()                        // Adds the next value, switches to an Addition builder
    .invokevirtual(method2)     // Pops back a value, switches back to the expression builder
  .condition()                  // Adds a condition, switches to the condition builder
    .ifEqual(target)            
    .ifSmaller(5, target2)                     
    .ifBigger(6, target3)
    .else(target4)              
  .buildStmt()                  // Creates a statement (or statement list) based on the previous instructions
```

# Credits

## Libraries used
- [Maple IR and the Team](https://github.com/LLVM-but-worse/maple-ir)
- [ASM](https://gitlab.ow2.org/asm/asm)
- [AntiDumper by Sim0n](https://github.com/sim0n/anti-java-agent/)

## Inspired from
- [Soot](https://github.com/soot-oss/soot)
- [Zelix KlassMaster](https://zelix.com)
