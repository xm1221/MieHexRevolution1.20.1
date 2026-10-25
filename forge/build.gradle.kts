plugins {
    id("miehex_revolution.platform")
}

val modId: String by project

architectury {
    forge()
}

loom {
    forge {
        convertAccessWideners = true
        extraAccessWideners.add(loom.accessWidenerPath.get().asFile.name)

        mixinConfig(
            "miehex_revolution-common.mixins.json",
            "miehex_revolution-forge.mixins.json",
        )
    }

    runs {
        // Forge 开发环境的 Minecraft 已是 Mojmap。legacy mixin AP 的 refmap 存的是中间名
        // （class_1657），运行时对不回去，注入会失败并让早期加载窗口把进程挂住。
        // 正式包仍走 refmap，由 Architectury 在 transformProductionForge 里翻成 SRG。
        listOf("client", "server").forEach { runName ->
            named(runName) {
                property("mixin.env.disableRefMap", "true")
            }
        }

        for ((platform, outputProject) in arrayOf(
            // we're using forge to do the common datagen because fabric's datagen kind of sucks
            "common" to project(":common"),
            "forge" to project,
        )) {
            register("${platform}Datagen") {
                data()
                programArgs(
                    "--mod", modId,
                    "--all",
                    "--output", outputProject.file("src/generated/resources").absolutePath,
                    "--existing", file("src/main/resources").absolutePath,
                    "--existing", project(":common").file("src/main/resources").absolutePath,
                    "--existing-mod", "hexcasting",
                )
                property("miehex_revolution.apply-datagen-mixin", "true")
                property("miehex_revolution.$platform-datagen", "true")
            }
        }
    }
}

miehex_revolutionModDependencies {
    // expand versions in mods.toml
    filesMatching.add("META-INF/mods.toml")

    // transform Gradle version ranges into a Forge-compatible format
    anyVersion = ""
    mapVersions {
        replace(Regex("""\](\S+)"""), "($1")
        replace(Regex("""(\S+)\["""), "$1)")
    }

    // CurseForge/Modrinth mod dependency metadata
    requires("architectury-api")
    requires("cloth-config")
    requires(curseforge = "hexcasting", modrinth = "hex-casting")
    requires("kotlin-for-forge")
}

dependencies {
    modApi(libs.mielib.forge) { isTransitive = false }
    include(libs.mielib.forge)

    forge(libs.forge)
    modApi(libs.architectury.forge)

    implementation(libs.kotlin.forge)

    modApi(libs.hexcasting.forge) { isTransitive = false }
    // Hex Casting dependencies
    // we use modLocalRuntime to add these to the development runtime, but not at compile time or for consumers of this project
    // but we use PAUCAL for datagen, so that's part of the actual implementation
    modImplementation(libs.paucal.forge)
    modLocalRuntime(libs.patchouli.forge)
    modLocalRuntime(libs.caelus)
    modLocalRuntime(libs.inline.forge) { isTransitive = false }

    // hexparse is a soft dependency: dev-runtime only, not declared as a hard requirement
    modLocalRuntime(libs.hexparse.forge)

    modApi(libs.clothConfig.forge)

    libs.mixinExtras.common.also {
        compileOnly(it)
        annotationProcessor(it)
    }

    libs.mixinExtras.forge.also {
        localRuntime(it)
        include(it)
    }
}

tasks {
    shadowJar {
        exclude("fabric.mod.json")
    }
}
