---
name: java-version-expert
description: Java 8/11/17/21/25 compatibility for Minecraft plugins. Use when choosing language features, bytecode target, or multi-JDK builds.
---

# java-version-expert

## IoriProxy
- Build/run target: **JDK 17** (Velocity).
- Avoid preview APIs; stick to Java 17 SE.

## Rules
- Records/switch/text blocks OK en 17+.
- No APIs Java 21+ si el target es 17.
- Bungee legacy clients ≠ Java 8 bytecode requirement del plugin proxy (sigue 17).
