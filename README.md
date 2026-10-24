[![](https://jitpack.io/v/sakura-ryoko/litematica.svg)](https://jitpack.io/#sakura-ryoko/litematica)

Litematica
==============
Litematica is a client-side Minecraft mod using LiteLoader.
It is more or less a re-creation of or a substitute for [Schematica](https://www.curseforge.com/minecraft/mc-mods/schematica),
for players who don't want to have Forge installed.
For compiled builds (= downloads), see https://www.curseforge.com/minecraft/mc-mods/litematica/files/all

Versions
========
![](https://img.shields.io/badge/Minecraft-26.3-blueviolet?style=flat-square)
![](https://img.shields.io/badge/Minecraft-26.2-blueviolet?style=flat-square)
![](https://img.shields.io/badge/Minecraft-26.1-blueviolet?style=flat-square)
![](https://img.shields.io/badge/Minecraft-1.21.11-blue?style=flat-square)
![](https://img.shields.io/badge/Minecraft-1.21.10-blue?style=flat-square)
![](https://img.shields.io/badge/Minecraft-1.21.8-blue?style=flat-square)
![](https://img.shields.io/badge/Minecraft-1.21.5-blue?style=flat-square)
![](https://img.shields.io/badge/Minecraft-1.21.4-blue?style=flat-square)
![](https://img.shields.io/badge/Minecraft-1.21.3-blue?style=flat-square)
![](https://img.shields.io/badge/Minecraft-1.21-blue?style=flat-square)
![](https://img.shields.io/badge/Minecraft-1.20.6-green?style=flat-square)
![](https://img.shields.io/badge/Minecraft-1.20.4-green?style=flat-square)
![](https://img.shields.io/badge/Minecraft-1.20.2-green?style=flat-square)
![](https://img.shields.io/badge/Minecraft-1.20.1-green?style=flat-square)

One source tree, fourteen builds. All versions share a single mod version (`0.92.2`),
and every jar lands under `build/<version>/libs/`.

Compiling
=========
* Clone the repository
* Open a command prompt/terminal to the repository directory
* run 'gradlew build'
* The built jar files will be under build/<version>/libs/

Build a single version:

* run 'gradlew :1.21.10:build'
* or gather all jars into one folder with 'gradlew buildAndGather'

Note: Gradle itself needs Java 25 (Fabric Loom 1.18.2). The game process is
started with the JDK matching each Minecraft version, so `runClient` works on the
older targets as well.
