plugins {
    java
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(8)
}

dependencies {
    implementation(project(":nms-api"))
    compileOnly(
        files(
            "${(findProperty("iori.spigotVersions") as String?) ?: "D:/Minecraft/BuildTools/Spigot Versions"}/spigot-1.8.8.jar"
        )
    )
}

tasks.jar {
    archiveBaseName.set("IoriCore-nms-v1_8_R3")
}
