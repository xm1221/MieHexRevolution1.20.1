package cn.xm1221.MieHexRevolution.neoforge

import cn.xm1221.MieHexRevolution.Miehex_revolution
import cn.xm1221.MieHexRevolution.Miehex_revolutionClient
import cn.xm1221.MieHexRevolution.block.idea.client.IdeaBlockEntityRenderer
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModContainer
import net.neoforged.fml.common.Mod
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent

/**
 * NeoForge client entrypoint.
 *
 * `idea_base` has no model elements, so the block is invisible without the BER. Fabric
 * registers it from its client initializer; NeoForge needs this separate @Mod(dist = CLIENT)
 * class to do the same.
 */
@Mod(value = Miehex_revolution.MODID, dist = [Dist.CLIENT])
class NeoforgeMiehex_revolutionClient(modBus: IEventBus, container: ModContainer) {
    init {
        modBus.addListener(::onClientSetup)
    }

    private fun onClientSetup(event: FMLClientSetupEvent) {
        event.enqueueWork {
            Miehex_revolutionClient.init()
            IdeaBlockEntityRenderer.register()
        }
    }
}
