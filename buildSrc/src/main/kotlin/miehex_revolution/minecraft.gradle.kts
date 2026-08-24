// A convention plugin that should be applied to all Minecraft-related subprojects, including common.

@file:Suppress("UnstableApiUsage")

package miehex_revolution

import kotlin.io.path.div
import libs

plugins {
    id("miehex_revolution.java")

    `maven-publish`
    id("dev.architectury.loom")
    id("at.petra-k.pkpcpbp.PKJson5Plugin")
}

val modId: String by project
val platform: String by project

base.archivesName = "${modId}-$platform"

loom {
    silentMojangMappingsLicense()
    accessWidenerPath = project(":common").file("src/main/resources/miehex_revolution.accesswidener")

    mixin {
        // the default name includes both archivesName and the subproject, resulting in the platform showing up twice
        // default: miehex_revolution-common-common-refmap.json
        // fixed:   miehex_revolution-common.refmap.json
        defaultRefmapName = "${base.archivesName.get()}.refmap.json"
        // legacy AP 由 loom 托管（自动加 AP 依赖 + 传混淆映射 + 生成 refmap）。
        // Fabric 必须靠 refmap 把 mojmap 名映射到 intermediary，false（新版 AP）拿不到 loom 的映射参数，refmap 不生成。
        useLegacyMixinAp = true
    }
}

pkJson5 {
    autoProcessJson5 = true
    autoProcessJson5Flattening = false
}

dependencies {
    minecraft(libs.minecraft)

    mappings(loom.layered {
        officialMojangMappings()
        parchment(libs.parchment)
    })

    annotationProcessor(libs.bundles.asm)
}

sourceSets {
    main {
        kotlin {
            srcDir(file("src/main/java"))
        }
        resources {
            srcDir(file("src/generated/resources"))
        }
    }
}
