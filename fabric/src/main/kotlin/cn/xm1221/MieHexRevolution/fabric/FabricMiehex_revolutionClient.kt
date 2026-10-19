package cn.xm1221.MieHexRevolution.fabric

import cn.xm1221.MieHexRevolution.Miehex_revolutionClient
import cn.xm1221.MieHexRevolution.block.idea.client.IdeaBlockEntityRenderer
import cn.xm1221.MieHexRevolution.registry.Miehex_revolutionBlocks
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents
import at.petrak.hexcasting.api.item.MediaHolderItem
import at.petrak.hexcasting.api.misc.MediaConstants
import at.petrak.hexcasting.common.lib.HexCreativeTabs
import net.minecraft.world.item.ItemStack

/**
 * Client entrypoint: config (Cloth AutoConfig) and the Idea Block renderer.
 *
 * The Idea Shard is inserted into Hex's creative tab here. In Hex 0.12 media lives on
 * `ItemStack` data components, so it is charged through [MediaHolderItem.setMedia] rather
 * than the old static `ItemMediaBattery.withMedia` helper.
 */
object FabricMiehex_revolutionClient : ClientModInitializer {
    override fun onInitializeClient() {
        Miehex_revolutionClient.init()
        IdeaBlockEntityRenderer.register()

        // Hex 0.12 exposes the tab as a Holder plus its ResourceKey; Fabric's event takes the key.
        ItemGroupEvents.modifyEntriesEvent(HexCreativeTabs.HEX_KEY).register { entries ->
            val stack = ItemStack(Miehex_revolutionBlocks.IDEA_SHARD.get())
            (stack.item as? MediaHolderItem)?.setMedia(stack, MediaConstants.QUENCHED_SHARD_UNIT)
            entries.accept(stack)
        }
    }
}
