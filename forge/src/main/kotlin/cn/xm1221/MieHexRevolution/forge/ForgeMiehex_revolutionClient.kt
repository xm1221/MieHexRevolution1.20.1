package cn.xm1221.MieHexRevolution.forge

import cn.xm1221.MieHexRevolution.Miehex_revolutionClient
import net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent
import thedarkcolour.kotlinforforge.forge.LOADING_CONTEXT

object ForgeMiehex_revolutionClient {
    @Suppress("UNUSED_PARAMETER")
    fun init(event: FMLClientSetupEvent) {
        Miehex_revolutionClient.init()
        LOADING_CONTEXT.registerExtensionPoint(ConfigScreenFactory::class.java) {
            ConfigScreenFactory { _, parent -> Miehex_revolutionClient.getConfigScreen(parent) }
        }
    }
}
