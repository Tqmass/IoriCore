import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

plugins {
    java
    id("com.gradleup.shadow")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks.withType<JavaCompile>().configureEach {
    // Matches :common (switch/Adventure). Runs on JVM 17+.
    options.release.set(17)
}

dependencies {
    compileOnly(
        files(
            "${(findProperty("iori.spigotVersions") as String?) ?: "D:/Minecraft/BuildTools/Spigot Versions"}/spigot-1.8.8.jar"
        )
    )
    implementation(project(":common"))
    implementation(project(":api"))
    implementation(project(":nms-api"))
    implementation(project(":nms-v1_8_R3"))
    implementation(project(":nms-v1_20_R3"))
    implementation(project(":nms-v1_21_R1"))
    // v26_R1: Java 25 bytecode — embeber jar (no merge Shadow/ASM) y cargar en runtime.
    implementation("com.h2database:h2:2.3.232")

    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("com.mysql:mysql-connector-j:9.1.0")
    implementation("redis.clients:jedis:5.2.0")
    implementation("org.mindrot:jbcrypt:0.4")
    compileOnly("org.mongodb:mongodb-driver-sync:5.2.1")
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    enabled = false
}

val nmsV26Jar = project(":nms-v26_R1").tasks.named<Jar>("jar")

tasks.shadowJar {
    dependsOn(nmsV26Jar)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    archiveBaseName.set("IoriCore-Bukkit")
    archiveClassifier.set("")
    archiveVersion.set(project.version.toString())
    relocate("com.zaxxer.hikari", "us.iorilabs.libs.hikari")
    relocate("com.mysql", "us.iorilabs.libs.mysql")
    relocate("org.h2", "us.iorilabs.libs.h2")
    relocate("redis.clients", "us.iorilabs.libs.jedis")
    relocate("org.apache.commons.pool2", "us.iorilabs.libs.commons.pool2")
    relocate("org.json", "us.iorilabs.libs.json")
    relocate("org.yaml.snakeyaml", "us.iorilabs.libs.snakeyaml")
    relocate("com.google.gson", "us.iorilabs.libs.gson")
    relocate("org.mindrot", "us.iorilabs.libs.jbcrypt")
    relocate("net.kyori", "us.iorilabs.libs.kyori")
    mergeServiceFiles()

    // No merge ASM: bytecode 69 (Java 25). Inyectar jar anidado post-shadow.
    doLast {
        val shadowFile = archiveFile.get().asFile
        val nestedJar = nmsV26Jar.get().archiveFile.get().asFile
        val tmp = shadowFile.resolveSibling(shadowFile.name + ".nms-embed.tmp")
        ZipOutputStream(tmp.outputStream().buffered()).use { zos ->
            ZipFile(shadowFile).use { zin ->
                val entries = zin.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    if (entry.name == "META-INF/ioricore-nms/v26_R1.jar") continue
                    zos.putNextEntry(ZipEntry(entry.name))
                    zin.getInputStream(entry).use { input -> input.copyTo(zos) }
                    zos.closeEntry()
                }
            }
            zos.putNextEntry(ZipEntry("META-INF/ioricore-nms/v26_R1.jar"))
            nestedJar.inputStream().buffered().use { input -> input.copyTo(zos) }
            zos.closeEntry()
        }
        tmp.copyTo(shadowFile, overwrite = true)
        tmp.delete()
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.processResources {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}
