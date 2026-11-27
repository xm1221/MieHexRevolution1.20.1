package cn.xm1221.MieHexRevolution.neoforge

import at.petrak.hexcasting.api.item.MediaHolderItem
import at.petrak.hexcasting.api.misc.MediaConstants
import at.petrak.hexcasting.common.lib.HexCreativeTabs
import cn.xm1221.MieHexRevolution.Miehex_revolution
import cn.xm1221.MieHexRevolution.Miehex_revolutionClient
import cn.xm1221.MieHexRevolution.block.idea.client.IdeaBlockEntityRenderer
import cn.xm1221.MieHexRevolution.client.pigment.CustomPigmentTextures
import cn.xm1221.MieHexRevolution.registry.Miehex_revolutionBlocks
import net.minecraft.world.item.ItemStack
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModContainer
import net.neoforged.fml.common.Mod
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent

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
        modBus.addListener(::onRegisterReloadListeners)
        modBus.addListener(::onBuildCreativeTab)
    }

    private fun onClientSetup(event: FMLClientSetupEvent) {
        event.enqueueWork {
            Miehex_revolutionClient.init()
            IdeaBlockEntityRenderer.register()
        }
    }

    private fun onRegisterReloadListeners(event: RegisterClientReloadListenersEvent) {
        event.registerReloadListener(CustomPigmentTextures.RELOAD_LISTENER)
    }

    private fun onBuildCreativeTab(event: BuildCreativeModeTabContentsEvent) {
        if (event.tabKey == HexCreativeTabs.HEX_KEY) {
            val stack = ItemStack(Miehex_revolutionBlocks.IDEA_SHARD.get())
            (stack.item as? MediaHolderItem)?.setMedia(stack, MediaConstants.QUENCHED_SHARD_UNIT)
            event.accept(stack)
            event.accept(ItemStack(Miehex_revolutionBlocks.IDEA_FULL.get()))
            event.accept(ItemStack(Miehex_revolutionBlocks.CUSTOM_PIGMENT.get()))
        }
    }
}
