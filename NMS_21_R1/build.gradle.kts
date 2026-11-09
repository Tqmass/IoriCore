plugins {
    java
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
}

val spigotVersions =
    (findProperty("iori.spigotVersions") as String?) ?: "D:/Minecraft/BuildTools/Spigot Versions"

dependencies {
    implementation(project(":nms-api"))
    compileOnly(files("$spigotVersions/spigot-api-1.21.1.jar"))
    compileOnly(files("$spigotVersions/spigot-1.21.1-server.jar"))
    compileOnly(fileTree("$spigotVersions/libs-1.21.1") { include("*.jar") })
}

// Loaded only on 1.21 JVMs via VersionMatcher; expose as 17-compatible for :bukkit resolution.
configurations.configureEach {
    if (isCanBeConsumed) {
        attributes.attribute(
            org.gradle.api.attributes.java.TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE,
            17
        )
    }
}

tasks.jar {
    archiveBaseName.set("IoriCore-nms-v1_21_R1")
}
