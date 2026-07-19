package cn.xm1221.MieHexRevolution.fabric

import cn.xm1221.MieHexRevolution.Miehex_revolution
import net.fabricmc.api.ModInitializer

object FabricMiehex_revolution : ModInitializer {
    override fun onInitialize() {
        Miehex_revolution.init()
    }
}
