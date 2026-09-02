plugins {
    id("miehex_revolution.minecraft")
}

architectury {
    common("fabric", "forge")
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(kotlin("reflect"))

    // We depend on fabric loader here to use the fabric @Environment annotations and get the mixin dependencies
    // Do NOT use other classes from fabric loader
    modImplementation(libs.fabric.loader)
    modImplementation(libs.paucal.common)
    modApi(libs.architectury)

    modApi(libs.hexcasting.common)

    modApi(libs.clothConfig.common)

    modApi(libs.hexparse.fabric)

    libs.mixinExtras.common.also {
        implementation(it)
        annotationProcessor(it)
    }
}
