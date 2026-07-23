package cn.xm1221.MieHexRevolution.registry

import cn.xm1221.MieHexRevolution.block.idea.*
import dev.architectury.registry.registries.DeferredRegister
import dev.architectury.registry.registries.RegistrySupplier
import net.minecraft.core.registries.Registries
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block
import cn.xm1221.MieHexRevolution.Miehex_revolution

object Miehex_revolutionBlocks {
    private val BLOCKS = DeferredRegister.create(Miehex_revolution.MODID, Registries.BLOCK)
    private val ITEMS = DeferredRegister.create(Miehex_revolution.MODID, Registries.ITEM)

    val IDEA_FULL: RegistrySupplier<IdeaFullBlock> = registerBlockItem("idea_full", ::IdeaFullBlock)

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
