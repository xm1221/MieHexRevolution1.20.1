package cn.xm1221.MieHexRevolution.fabric

import cn.xm1221.MieHexRevolution.Miehex_revolution
import net.fabricmc.api.ModInitializer

/**
 * Fabric common entrypoint. Registration order matters only in that the platform
 * [Miehex_revolutionAbstractionsImpl.initRegistry] must be in place before
 * [Miehex_revolution.init] runs; Architectury resolves the expect/actual at compile time,
 * so this is a plain call.
 */
object FabricMiehex_revolution : ModInitializer {
    override fun onInitialize() {
        Miehex_revolution.init()
    }
}
