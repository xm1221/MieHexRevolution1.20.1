package cn.xm1221.MieHexRevolution.registry

import at.petrak.hexcasting.api.misc.MediaConstants
import cn.xm1221.MieHexRevolution.block.idea.*
import cn.xm1221.MieHexRevolution.item.ItemIdeaShard
import dev.architectury.registry.registries.DeferredRegister
import dev.architectury.registry.registries.RegistrySupplier
import net.minecraft.core.registries.Registries
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block
import cn.xm1221.MieHexRevolution.Miehex_revolution
import net.minecraft.world.item.Rarity

object Miehex_revolutionBlocks {
    private val BLOCKS = DeferredRegister.create(Miehex_revolution.MODID, Registries.BLOCK)
    private val ITEMS = DeferredRegister.create(Miehex_revolution.MODID, Registries.ITEM)

    val IDEA_FULL: RegistrySupplier<IdeaFullBlock> = registerBlockItem("idea_full", ::IdeaFullBlock)
    val IDEA_SHARD: RegistrySupplier<ItemIdeaShard> = ITEMS.register("idea_shard") { ItemIdeaShard(Item.Properties().stacksTo(1).rarity(
        Rarity.UNCOMMON)) }

    fun init() {
        BLOCKS.register()
        ITEMS.register()
    }

    private fun <T : Block> registerBlockItem(name: String, factory: () -> T): RegistrySupplier<T> {
        val block = BLOCKS.register(name, factory)
        ITEMS.register(name) { BlockItem(block.get(), Item.Properties()) }
        return block
    }

}
