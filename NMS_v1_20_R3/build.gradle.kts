plugins {
    java
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
}

val spigotVersions =
    (findProperty("iori.spigotVersions") as String?) ?: "D:/Minecraft/BuildTools/Spigot Versions"

dependencies {
    implementation(project(":nms-api"))
    compileOnly(files("$spigotVersions/spigot-api-1.20.4.jar"))
    compileOnly(files("$spigotVersions/spigot-1.20.4-server.jar"))
    compileOnly(fileTree("$spigotVersions/libs-1.20.4") { include("*.jar") })
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
    archiveBaseName.set("IoriCore-nms-v1_20_R3")
}
