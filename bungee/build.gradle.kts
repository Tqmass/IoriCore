plugins {
    java
    id("com.gradleup.shadow")
}

dependencies {
    compileOnly("net.md-5:bungeecord-api:1.21-R0.4-SNAPSHOT")
    implementation(project(":common"))
    implementation(project(":api"))

    implementation("net.kyori:adventure-platform-bungeecord:4.3.4")
    implementation("org.yaml:snakeyaml:2.3")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("com.mysql:mysql-connector-j:9.1.0")
    implementation("com.h2database:h2:2.3.232")
    implementation("org.mongodb:mongodb-driver-sync:5.2.1")
    implementation("redis.clients:jedis:5.2.0")
    implementation("net.kyori:adventure-api:4.17.0")
    implementation("net.kyori:adventure-text-minimessage:4.17.0")
    implementation("net.kyori:adventure-text-serializer-legacy:4.17.0")
    implementation("net.kyori:adventure-text-serializer-ansi:4.17.0")
    implementation("net.kyori:adventure-text-serializer-gson:4.17.0")
    implementation("net.dv8tion:JDA:5.6.1") {
        exclude(module = "opus-java")
    }
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    enabled = false
}

tasks.shadowJar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    archiveBaseName.set("IoriBungee")
    archiveClassifier.set("")
    archiveVersion.set(project.version.toString())
    relocate("com.zaxxer.hikari", "us.iorilabs.libs.hikari")
    relocate("com.mysql", "us.iorilabs.libs.mysql")
    relocate("org.h2", "us.iorilabs.libs.h2")
    relocate("com.mongodb", "us.iorilabs.libs.mongodb")
    relocate("org.bson", "us.iorilabs.libs.bson")
    relocate("redis.clients", "us.iorilabs.libs.jedis")
    relocate("org.yaml.snakeyaml", "us.iorilabs.libs.snakeyaml")
    relocate("com.google.gson", "us.iorilabs.libs.gson")
    relocate("de.mkammerer.argon2", "us.iorilabs.libs.argon2")
    relocate("org.mindrot", "us.iorilabs.libs.jbcrypt")
    relocate("net.kyori.adventure.platform", "us.iorilabs.libs.adventure.platform")
    relocate("net.dv8tion.jda", "us.iorilabs.libs.jda")
    relocate("gnu.trove", "us.iorilabs.libs.trove")
    relocate("com.neovisionaries", "us.iorilabs.libs.nv")
    relocate("okhttp3", "us.iorilabs.libs.okhttp3")
    relocate("okio", "us.iorilabs.libs.okio")
    relocate("org.apache.commons.collections4", "us.iorilabs.libs.commons.collections4")
    mergeServiceFiles()
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.processResources {
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}
