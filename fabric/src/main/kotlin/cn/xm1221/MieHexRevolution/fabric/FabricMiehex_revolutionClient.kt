package cn.xm1221.MieHexRevolution.fabric

import at.petrak.hexcasting.api.misc.MediaConstants
import at.petrak.hexcasting.common.items.magic.ItemMediaBattery
import at.petrak.hexcasting.common.lib.HexCreativeTabs
import cn.xm1221.MieHexRevolution.Miehex_revolutionClient
import cn.xm1221.MieHexRevolution.block.idea.client.IdeaBlockEntityRenderer
import cn.xm1221.MieHexRevolution.registry.Miehex_revolutionBlocks
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.item.ItemStack

object FabricMiehex_revolutionClient : ClientModInitializer {
    override fun onInitializeClient() {
        Miehex_revolutionClient.init()
        IdeaBlockEntityRenderer.register()

        val hexTab = BuiltInRegistries.CREATIVE_MODE_TAB.getResourceKey(HexCreativeTabs.HEX) ?: return
        ItemGroupEvents.modifyEntriesEvent(hexTab.get()).register { entries ->
            val stack = ItemStack(Miehex_revolutionBlocks.IDEA_SHARD.get())
            ItemMediaBattery.withMedia(stack, MediaConstants.QUENCHED_SHARD_UNIT, MediaConstants.QUENCHED_SHARD_UNIT)
            entries.accept(stack)
        }
    }
}
