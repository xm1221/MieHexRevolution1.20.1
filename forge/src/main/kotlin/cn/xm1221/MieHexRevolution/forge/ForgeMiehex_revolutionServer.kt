package cn.xm1221.MieHexRevolution.forge

import cn.xm1221.MieHexRevolution.Miehex_revolution
import net.minecraftforge.fml.event.lifecycle.FMLDedicatedServerSetupEvent

object ForgeMiehex_revolutionServer {
    @Suppress("UNUSED_PARAMETER")
    fun init(event: FMLDedicatedServerSetupEvent) {
        Miehex_revolution.initServer()
    }
}
