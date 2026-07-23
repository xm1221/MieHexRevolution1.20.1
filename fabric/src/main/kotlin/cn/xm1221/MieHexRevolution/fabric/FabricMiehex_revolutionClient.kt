package cn.xm1221.MieHexRevolution.fabric

import cn.xm1221.MieHexRevolution.Miehex_revolutionClient
import cn.xm1221.MieHexRevolution.block.idea.client.IdeaBlockEntityRenderer
import net.fabricmc.api.ClientModInitializer

object FabricMiehex_revolutionClient : ClientModInitializer {
    override fun onInitializeClient() {
        Miehex_revolutionClient.init()
        IdeaBlockEntityRenderer.register()
    }
}
