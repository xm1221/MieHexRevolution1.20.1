package cn.xm1221.MieHexRevolution.block.idea

import cn.xm1221.MieHexRevolution.util.BlockStateCycler
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtUtils
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.DoorBlock
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState

/**
 * 理念方块的方块实体：只存“要模仿成什么样”这一件事。
 *
 * <p>形状、碰撞、受光、破坏粒子、渲染都从这里取材质状态，所以这里保持一致是全部表现的前提。
 */
class IdeaBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState,
) : BlockEntity(type, pos, state) {

    var material: BlockState = Blocks.AIR.defaultBlockState()
        private set

    /** 是否已有外观（没有时渲染理念本体）。 */
    val hasCustomMaterial: Boolean get() = !material.isAir

    fun applyMaterial(state: BlockState) {
        material = sanitize(state)
        setChanged()
        val level = level ?: return
        if (level.isClientSide) return
        level.sendBlockUpdated(worldPosition, blockState, blockState, Block.UPDATE_CLIENTS)
        // 材质的不透明度变了，方块自身的位置要重新算一次光。
        level.lightEngine.checkBlock(worldPosition)
    }

    fun resetMaterial() = applyMaterial(Blocks.AIR.defaultBlockState())

    /**
     * 循环材质里可变的形态：朝向 → 半砖上下 → 开关状态等。
     *
     * @param face 右键的那一面；点上下两面时优先循环“半”（楼梯/活板门/半砖），更符合直觉
     * @return 是否真的换了一个状态（没有可循环属性时返回 false，交给调用方刷新连接）
     */
    fun cycleMaterial(face: Direction?): Boolean {
        if (!hasCustomMaterial) return false
        val next = nextVariant(material, face) ?: return false
        applyMaterial(next)
        return true
    }

    /**
     * 兜底：不存“不能当材质”的东西。
     *
     * <p>两种都不该出现：需要方块实体才成立的方块（链式递归渲染），以及理念方块自己
     * （[IdeaBlock.materialAt] / 形状委托会无限递归）。
     */
    private fun sanitize(state: BlockState): BlockState = when {
        state.isAir -> Blocks.AIR.defaultBlockState()
        state.block is IdeaBlock -> Blocks.AIR.defaultBlockState()
        state.block is EntityBlock -> Blocks.AIR.defaultBlockState()
        else -> state
    }

    private fun nextVariant(state: BlockState, face: Direction?): BlockState? {
        val order = if (face != null && face.axis.isVertical) {
            listOf("half", "type", "facing", "axis", "rotation", "open", "hinge", "shape")
        } else {
            listOf("facing", "axis", "type", "half", "rotation", "open", "hinge", "shape")
        }
        for (name in order) {
            // 门的上下两半是成对的，单独翻转 HALF 会把整扇门颠倒，跳过这一项。
            if (name == "half" && state.block is DoorBlock) continue
            val property = state.properties.firstOrNull { it.name == name } ?: continue
            if (property.possibleValues.size < 2) continue
            // rotation 是 0..15 的十六向属性，一格一步太慢，直接四向循环。
            val step = if (name == "rotation") 4 else 1
            val next = BlockStateCycler.cycleRaw(state, property, step)
            if (next !== state) return next
        }
        return null
    }

    // === 同步 ===

    override fun getUpdatePacket(): Packet<ClientGamePacketListener> =
        ClientboundBlockEntityDataPacket.create(this)

    override fun getUpdateTag(): CompoundTag = saveWithoutMetadata()

    override fun load(tag: CompoundTag) {
        super.load(tag)
        material = if (tag.contains("Material")) {
            // 方块注册表里已经没有这个方块（卸掉了某个模组）时，原版 readBlockState 会退回 AIR，不会崩。
            sanitize(NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag.getCompound("Material")))
        } else {
            Blocks.AIR.defaultBlockState()
        }
    }

    override fun saveAdditional(tag: CompoundTag) {
        super.saveAdditional(tag)
        tag.put("Material", NbtUtils.writeBlockState(material))
    }
}
