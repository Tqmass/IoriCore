plugins {
    java
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
    // Lombok 1.18.36 no soporta javac 25 (TypeTag.UNKNOWN). Este módulo no usa Lombok.
    options.compilerArgs.add("-proc:none")
}

val spigotVersions =
    (findProperty("iori.spigotVersions") as String?) ?: "D:/Minecraft/BuildTools/Spigot Versions"

dependencies {
    implementation(project(":nms-api"))
    compileOnly(files("$spigotVersions/spigot-api-26.3.jar"))
    compileOnly(files("$spigotVersions/spigot-26.3-server.jar"))
    compileOnly(fileTree("$spigotVersions/libs-26.3") { include("*.jar") })
}

configurations.configureEach {
    if (isCanBeConsumed) {
        attributes.attribute(
            org.gradle.api.attributes.java.TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE,
            17
        )
    }
}

tasks.jar {
    archiveBaseName.set("IoriCore-nms-v26_R1")
}
