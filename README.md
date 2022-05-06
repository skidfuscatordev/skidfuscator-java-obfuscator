
## Features

Here are the features:
| Feature | Type | Description |
| --- | --- | --- |
| `Bogus Jump` | Flow (Intra) | Invalid jump to some random generated code to prevent skidding |
| `Mangled Jump` | Flow (Intra) | Mutation to the jump condition to make it appear more complex than it actually is |
| `Exception Jump` | Flow (Intra) | Changes done to flow semantics by forcing an exception then handling all the code in the catch clause |
| `Exception Return`| Flow (**Inter**) | Throw an exception with the value and catch it as opposed to returning it (Very heavy) |
| `Strong Opaque Predicate` | Flow (**Inter**) | Use heredity and method invocation to pass a predicate as opposed to declaring it at the beginning of the CFG |
| `Method Inlining` | Flow (**Inter**) | Inline uncommon methods which aren't too big |
| `Method Outlining` | Flow (**Inter**) | Outline some non-sensitive blocks |
| `Loop Unrolling` | Flow (**Inter**) | Rewrite some loops instructions into continuous segments if the loop limit can be pre-determined |
| `Flattening` | Flow (**Intra**) | Use a dispatcher method to harden the semantics of some block ranges (do not use entire method) |
| `String Encryption` | String | Encrypt the strings using the opaque predicate |
| `Reference Encryption` | Reference | Encrypt the reference calls using InvokeDynamic using the opaque predicate |
| `Reference Proxying` | Reference | Proxy references using a builder pattern OR dispatcher classes (mostly for initialisation) |



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
