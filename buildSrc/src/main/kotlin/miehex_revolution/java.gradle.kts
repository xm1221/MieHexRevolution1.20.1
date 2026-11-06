// A convention plugin that should be applied to all build scripts.

package miehex_revolution

import libs

plugins {
    java
    kotlin("jvm")
    id("architectury-plugin")
}

val mavenGroup: String by project
val modVersion: String by project
val javaVersion = libs.versions.java.get().toInt()
val release = System.getenv("RELEASE") == "true"

group = mavenGroup

version = modVersion
if (!release) {
    version = "$version-SNAPSHOT"
}

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
    maven { url = uri("https://maven.blamejared.com") }
    maven { url = uri("https://maven.fabricmc.net/") }
    maven { url = uri("https://maven.hexxy.media") }
    maven { url = uri("https://maven.ladysnake.org/releases") } // Cardinal Components
    maven { url = uri("https://maven.minecraftforge.net/") }
    maven { url = uri("https://maven.parchmentmc.org") }
    maven { url = uri("https://maven.shedaniel.me") }
    maven { url = uri("https://maven.terraformersmc.com/releases") }
    maven { url = uri("https://maven.theillusivec4.top") } // Caelus
    maven { url = uri("https://thedarkcolour.github.io/KotlinForForge") }
    exclusiveContent {
        filter {
            includeGroup("maven.modrinth")
        }
        forRepository {
            maven { url = uri("https://api.modrinth.com/maven") }
        }
    }
    exclusiveContent {
        filter {
            includeGroup("libs")
        }
        forRepository {
            flatDir { dir(rootProject.file("libs")) }
        }
    }
    exclusiveContent {
        filter {
            includeGroup("cn.xm1221.MieLib")
        }
        forRepository {
            maven {
                name = "xm1221Maven"
                url = uri("https://xm1221.github.io/maven")
            }
        }
    }
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(javaVersion)
    withSourcesJar()
    withJavadocJar()
}

kotlin {
    jvmToolchain(javaVersion)
}

tasks {
    compileJava {
        options.apply {
            encoding = "UTF-8"
            release = javaVersion
        }
    }

    jar {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    withType<GenerateModuleMetadata>().configureEach {
        enabled = false
    }

    javadoc {
        options {
            this as StandardJavadocDocletOptions
            // 源码里有中文注释；不指定编码的话 javadoc 会按系统默认（GBK）读 UTF-8 文件，
            // 报一堆 "unmappable character" 然后让整个 build 挂掉。
            encoding = "UTF-8"
            charSet = "UTF-8"
            addStringOption("Xdoclint:none", "-quiet")
        }
    }

    processResources {
        exclude(".cache")
    }

    processTestResources {
        exclude(".cache")
    }
}
