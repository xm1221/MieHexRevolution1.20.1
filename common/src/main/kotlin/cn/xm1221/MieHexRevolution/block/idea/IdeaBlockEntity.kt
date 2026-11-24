package cn.xm1221.MieHexRevolution.block.idea

import cn.xm1221.MieHexRevolution.util.BlockStateCycler
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.HolderLookup
import net.minecraft.core.registries.Registries
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtUtils
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.DoorBlock
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.block.state.properties.EnumProperty
import net.minecraft.world.level.block.state.properties.Property
import net.minecraft.world.level.block.state.properties.WallSide

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
        syncLightState(level)
        val updated = level.getBlockState(worldPosition)
        level.sendBlockUpdated(worldPosition, updated, updated, Block.UPDATE_CLIENTS)
        // 材质的不透明度变了，方块自身的位置要重新算一次光。
        level.lightEngine.checkBlock(worldPosition)
    }

    /**
     * 把材质挡多少光写进方块状态（[IdeaBlock.LIGHT]）。
     *
     * <p>必须走 `setBlock`：状态换了才会带上区块数据重新送到客户端、并触发网格与光照重算。
     * 同一方块之间的状态变更不会碰掉方块实体（[IdeaBlock.onRemove] 里也按方块类型判过）。
     */
    private fun syncLightState(level: Level) {
        val current = level.getBlockState(worldPosition)
        if (!current.hasProperty(IdeaBlock.LIGHT)) return
        val light = if (material.isAir) 0 else material.getLightBlock(level, worldPosition).coerceIn(0, 15)
        if (current.getValue(IdeaBlock.LIGHT) == light) return
        level.setBlock(worldPosition, current.setValue(IdeaBlock.LIGHT, light), Block.UPDATE_ALL)
    }

    fun resetMaterial() = applyMaterial(Blocks.AIR.defaultBlockState())

    /** 右键已有材质的结果，调用方靠它决定还要不要“按邻居刷新连接”。 */
    enum class CycleOutcome {
        /** 没有能变的东西。 */
        NONE,

        /** 换的是朝向 / 半砖上下 / 开关这类自身形态。 */
        VARIANT,

        /** 手动切了某一侧的连接（已经自己同步过对面那一侧）。 */
        CONNECTION,
    }

    /**
     * 循环材质里可变的形态：朝向 → 半砖上下 → 开关状态等。
     *
     * <p>栅栏 / 墙 / 玻璃板没有能循环的朝向属性，改成本地规则：右键点哪一面就切换那一侧的
     * 连接（墙在 NONE → LOW → TALL 之间走三档），并把对面那一侧一起改掉 —— 否则下一次
     * “按邻居刷新连接”会立刻把它补回来，右键等于白点。
     *
     * @param face 右键的那一面；点上下两面时优先循环“半”（楼梯/活板门/半砖），更符合直觉
     */
    fun cycleMaterial(face: Direction?): CycleOutcome {
        if (!hasCustomMaterial) return CycleOutcome.NONE
        if (face != null) {
            val toggled = toggleConnection(material, face)
            if (toggled != null) {
                applyMaterial(toggled)
                mirrorConnection(face, toggled)
                return CycleOutcome.CONNECTION
            }
        }
        val next = nextVariant(material, face) ?: return CycleOutcome.NONE
        applyMaterial(next)
        return CycleOutcome.VARIANT
    }

    /**
     * 栅栏 / 墙 / 玻璃板：把右键那一侧的连接翻一格。
     *
     * <p>只认“名字对得上、而且确实是通/不通开关”的属性。楼梯的 north/east/south/west 是
     * StairsShape、红石线的是 RedstoneSide，名字撞上了但不是这件事，返 null 交给
     * [nextVariant] 去循环朝向。
     */
    private fun toggleConnection(state: BlockState, face: Direction): BlockState? {
        if (!face.axis.isHorizontal) return null
        val property = state.properties.firstOrNull { it.name == face.serializedName } ?: return null
        return when {
            property is BooleanProperty -> state.setValue(property, !state.getValue(property))
            property is EnumProperty<*> && property.valueClass == WallSide::class.java -> {
                @Suppress("UNCHECKED_CAST")
                val wallSide = property as Property<WallSide>
                val next = when (state.getValue(wallSide)) {
                    WallSide.NONE -> WallSide.LOW
                    WallSide.LOW -> WallSide.TALL
                    else -> WallSide.NONE
                }
                state.setValue(wallSide, next)
            }

            else -> null
        }
    }

    /**
     * 把“我刚才切掉/接上的这一侧”抄到对面那一侧的相对连接上。
     *
     * <p>两个理念方块模仿同一种方块时，连接必须两边一致才有意义：只改自己这一侧，对面
     * 那一侧会继续伸一根对着空气的横杆。
     */
    private fun mirrorConnection(face: Direction, state: BlockState) {
        val level = level ?: return
        val neighbour = level.getBlockEntity(worldPosition.relative(face)) as? IdeaBlockEntity ?: return
        val neighbourState = neighbour.material
        // 只有“同一种方块”的两个理念方块才共享一套连接属性。
        if (!neighbourState.`is`(state.block)) return
        val ours = state.properties.firstOrNull { it.name == face.serializedName } ?: return
        val theirs = neighbourState.properties.firstOrNull { it.name == face.opposite.serializedName } ?: return
        val mirrored = when {
            ours is BooleanProperty && theirs is BooleanProperty ->
                neighbourState.setValue(theirs, state.getValue(ours))

            ours is EnumProperty<*> && theirs is EnumProperty<*> &&
                ours.valueClass == WallSide::class.java && theirs.valueClass == WallSide::class.java -> {
                @Suppress("UNCHECKED_CAST")
                val ourWall = ours as Property<WallSide>
                @Suppress("UNCHECKED_CAST")
                val theirWall = theirs as Property<WallSide>
                neighbourState.setValue(theirWall, state.getValue(ourWall))
            }

            else -> neighbourState
        }
        if (mirrored !== neighbourState) neighbour.applyMaterial(mirrored)
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

    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag =
        saveWithoutMetadata(registries)

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        material = if (tag.contains("Material")) {
            // 方块注册表里已经没有这个方块（卸掉了某个模组）时，原版 readBlockState 会退回 AIR，不会崩。
            sanitize(NbtUtils.readBlockState(registries.lookupOrThrow(Registries.BLOCK), tag.getCompound("Material")))
        } else {
            Blocks.AIR.defaultBlockState()
        }
    }

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        tag.put("Material", NbtUtils.writeBlockState(material))
    }
}
