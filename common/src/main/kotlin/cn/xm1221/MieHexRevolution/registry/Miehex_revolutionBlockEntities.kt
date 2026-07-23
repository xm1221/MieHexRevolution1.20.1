package cn.xm1221.MieHexRevolution.registry

import cn.xm1221.MieHexRevolution.Miehex_revolution
import cn.xm1221.MieHexRevolution.block.idea.IdeaBlockEntity
import dev.architectury.registry.registries.DeferredRegister
import dev.architectury.registry.registries.RegistrySupplier
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.Registries
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState

object Miehex_revolutionBlockEntities {
    private val BLOCK_ENTITIES = DeferredRegister.create(Miehex_revolution.MODID, Registries.BLOCK_ENTITY_TYPE)

    private object Factory {
        lateinit var type: BlockEntityType<IdeaBlockEntity>

        fun create(pos: BlockPos, state: BlockState): IdeaBlockEntity =
            IdeaBlockEntity(type, pos, state)
    }

    val IDEA_BE: RegistrySupplier<BlockEntityType<IdeaBlockEntity>> = BLOCK_ENTITIES.register("idea_be") {
        BlockEntityType.Builder.of(
            Factory::create,
            Miehex_revolutionBlocks.IDEA_FULL.get(),
        ).build(null).also { Factory.type = it }
    }

    fun init() {
        BLOCK_ENTITIES.register()
    }
}
