<p align="center">
  <img width="50%" height="50%" src="https://i.imgur.com/KzXiF67.png">
  <br>
  <a><img alt="Server Version" src="https://img.shields.io/badge/Server%20Version-J8%20J16-blue"></a>
  <a><img alt="Api Type" src="https://img.shields.io/badge/API-MapleIR-blue"></a>
  <a><img alt="Authors" src="https://img.shields.io/badge/Authors-Ghast-blue"></a>
  <a><img alt="Issues" src="https://img.shields.io/github/issues/terminalsin/skidfuscator-java-obfuscator"></a>
  <a><img alt="Forks" src="https://img.shields.io/github/forks/terminalsin/skidfuscator-java-obfuscator"></a>
  <a><img alt="Stars" src="https://img.shields.io/github/stars/terminalsin/skidfuscator-java-obfuscator"></a>
</p>

# What is Skidfuscator?
Skidfuscator is a proof of concept obfuscation tool designed to take advantage of SSA form to optimize and obfuscate Java bytecode
code flow. This is done via intra-procedural passes each designed to mingle the code in a shape where neither the time complexity
neither the space complexity suffers from a great loss. To achieve the such, we have modeled a couple of well known tricks to 
add a significant strength to the obfuscation whilst at the same time retaining a stable enough execution time.

This project is **not completed**. This is a proof of concept I've been working on for a while. As far as I could tell, there are
some serious flaws with parameter injection. 
