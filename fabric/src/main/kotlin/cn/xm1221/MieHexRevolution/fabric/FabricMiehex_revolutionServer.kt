package cn.xm1221.MieHexRevolution.fabric

import cn.xm1221.MieHexRevolution.Miehex_revolution
import net.fabricmc.api.DedicatedServerModInitializer

object FabricMiehex_revolutionServer : DedicatedServerModInitializer {
    override fun onInitializeServer() {
        Miehex_revolution.initServer()
    }
}
