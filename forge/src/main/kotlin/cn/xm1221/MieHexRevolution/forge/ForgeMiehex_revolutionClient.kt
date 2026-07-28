package cn.xm1221.MieHexRevolution.forge

import at.petrak.hexcasting.api.misc.MediaConstants
import at.petrak.hexcasting.common.items.magic.ItemMediaBattery
import at.petrak.hexcasting.common.lib.HexCreativeTabs
import cn.xm1221.MieHexRevolution.Miehex_revolutionClient
import cn.xm1221.MieHexRevolution.block.idea.client.IdeaBlockEntityRenderer
import cn.xm1221.MieHexRevolution.registry.Miehex_revolutionBlocks
import net.minecraft.world.item.ItemStack
import net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory
import net.minecraftforge.client.event.CreativeModeTabEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent
import thedarkcolour.kotlinforforge.forge.LOADING_CONTEXT

object ForgeMiehex_revolutionClient {
    @Suppress("UNUSED_PARAMETER")
    fun init(event: FMLClientSetupEvent) {
        Miehex_revolutionClient.init()
        IdeaBlockEntityRenderer.register()
        LOADING_CONTEXT.registerExtensionPoint(ConfigScreenFactory::class.java) {
            ConfigScreenFactory { _, parent -> Miehex_revolutionClient.getConfigScreen(parent) }
        }
    }

    @SubscribeEvent
    fun onBuildCreativeTab(event: CreativeModeTabEvent.BuildContents) {
        if (event.tab == HexCreativeTabs.HEX) {
            val stack = ItemStack(Miehex_revolutionBlocks.IDEA_SHARD.get())
            ItemMediaBattery.withMedia(stack, MediaConstants.QUENCHED_SHARD_UNIT)
            event.accept(stack)
        }
    }
}
