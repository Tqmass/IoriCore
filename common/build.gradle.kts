plugins {
    java
    `java-library`
}

dependencies {
    api("de.mkammerer:argon2-jvm:2.11")
    api("org.mindrot:jbcrypt:0.4")
    api("net.kyori:adventure-api:4.17.0")
    api("net.kyori:adventure-text-minimessage:4.17.0")
    api("net.kyori:adventure-text-serializer-legacy:4.17.0")
    api("net.kyori:adventure-text-serializer-ansi:4.17.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    archiveBaseName.set("IoriCore-common")
}
