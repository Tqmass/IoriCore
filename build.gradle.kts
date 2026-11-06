plugins {
    java
    id("com.gradleup.shadow") version "8.3.6" apply false
}

allprojects {
    group = "us.iorilabs"
    // Semver: patch=pequeño · minor=feature · major=breaking. Desktop jars = esta versión.
    version = "1.1.6"

    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
        maven("https://repo.velocitypowered.com/snapshots/") {
            mavenContent {
                includeGroupByRegex("com\\.velocitypowered.*")
            }
        }
        maven("https://repo.md-5.net/content/repositories/snapshots/") {
            mavenContent {
                includeGroupByRegex("net\\.md-5.*")
            }
        }
        maven("https://jitpack.io")
    }
}

subprojects {
    apply(plugin = "java")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
        }
        withSourcesJar()
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(17)
    }

    dependencies {
        "compileOnly"("org.jetbrains:annotations:24.1.0")
        "compileOnly"("org.projectlombok:lombok:1.18.36")
        "annotationProcessor"("org.projectlombok:lombok:1.18.36")
    }
}
