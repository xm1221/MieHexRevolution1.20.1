package cn.xm1221.MieHexRevolution.block.idea

import cn.xm1221.MieHexRevolution.registry.Miehex_revolutionBlockEntities
import cn.xm1221.MieHexRevolution.registry.Miehex_revolutionBlocks
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.AirBlock
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.DoorBlock
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.LiquidBlock
import net.minecraft.world.level.block.MultifaceBlock
import net.minecraft.world.level.block.VineBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf
import net.minecraft.world.level.block.state.properties.EnumProperty
import net.minecraft.world.level.block.state.properties.Property
import net.minecraft.world.level.block.state.properties.WallSide
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.VoxelShape

/**
 * 理念方块。
 *
 * <p>本体只有一个方块与一个方块实体：真正被“模仿”的方块状态存在方块实体里，
 * 形状、碰撞、受光、破坏粒子都按那份状态转发，渲染由 [IdeaBlockEntityRenderer] 贴上去。
 * 因此它可以伪装成不满一格的方块（半砖、楼梯、栅栏、玻璃板、门……），碰撞也随之一同改变。
 *
 * <p>交互约定：
 * - 空手潜行右键：退回“理念本体”的外观；
 * - 潜行时手持方块：交还原版处理（正常放置方块）；
 * - 非潜行手持方块：换上该方块外观；持同一种方块再右键则循环它的朝向/半砖位置等形态，
 *   没有可循环的属性时按当前邻居刷新连接（栅栏、墙、玻璃板）。
 */
abstract class IdeaBlock(properties: BlockBehaviour.Properties) : Block(properties), EntityBlock {

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        IdeaBlockEntity(Miehex_revolutionBlockEntities.IDEA_BE.get(), pos, state)

    override fun <T : BlockEntity> getTicker(level: Level, state: BlockState, type: BlockEntityType<T>): BlockEntityTicker<T>? = null

    // === 交互 ===

    override fun use(
        state: BlockState, level: Level, pos: BlockPos,
        player: Player, hand: InteractionHand, hit: BlockHitResult,
    ): InteractionResult {
        val be = level.getBlockEntity(pos) as? IdeaBlockEntity ?: return InteractionResult.PASS
        val stack = player.getItemInHand(hand)

        // 潜行：把手交还给原版。空手 = 退回理念本体；手持方块 = 原版放置方块（潜行放置）。
        if (player.isShiftKeyDown) {
            if (!stack.isEmpty || !be.hasCustomMaterial) return InteractionResult.PASS
            if (level.isClientSide) return InteractionResult.SUCCESS
            removeDoorPartner(level, pos)
            be.resetMaterial()
            level.playSound(null, pos, state.soundType.placeSound, SoundSource.BLOCKS, 1f, 1f)
            return InteractionResult.SUCCESS
        }

        if (stack.item !is BlockItem) return InteractionResult.PASS
        val material = getMaterialFor(level, pos, stack, hit, player, hand) ?: return InteractionResult.PASS
        if (level.isClientSide) return InteractionResult.SUCCESS

        if (be.hasCustomMaterial && be.material.`is`(material.block)) {
            // 同一种方块：依次循环朝向 / 台阶上下 / 开关状态等；没得循环就按当前邻居刷新连接。
            if (!be.cycleMaterial(hit.direction)) {
                be.applyMaterial(material)
            }
            syncDoorPartner(level, pos, be.material)
            refreshNeighbourConnections(level, pos, be.material)
            playApplySound(level, pos, be.material)
            return InteractionResult.SUCCESS
        }

        // 换一种方块：直接用新外观覆盖（不再需要先空手潜行还原）。
        removeDoorPartner(level, pos)
        be.applyMaterial(material)
        syncDoorPartner(level, pos, material)
        refreshNeighbourConnections(level, pos, material)
        playApplySound(level, pos, material)
        return InteractionResult.SUCCESS
    }

    override fun setPlacedBy(level: Level, pos: BlockPos, state: BlockState, placer: LivingEntity?, stack: ItemStack) {
        if (level.isClientSide) return
        val be = level.getBlockEntity(pos) as? IdeaBlockEntity ?: return
        // 物品自带 BlockEntityTag（破坏回收或创造模式中键拾取）时以它为准。
        if (be.hasCustomMaterial) return
        val player = placer as? Player ?: return
        val offhand = player.getItemInHand(InteractionHand.OFF_HAND)
        val facing = Direction.orderedByNearest(player).firstOrNull() ?: Direction.UP
        val hit = BlockHitResult(Vec3.atCenterOf(pos), facing, pos, false)
        val material = getMaterialFor(level, pos, offhand, hit, player, InteractionHand.OFF_HAND) ?: return
        be.applyMaterial(material)
        syncDoorPartner(level, pos, material)
        refreshNeighbourConnections(level, pos, material)
    }

    // === 形状 / 光照跟随被模仿的方块 ===

    override fun getShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape {
        val material = materialAt(level, pos) ?: return super.getShape(state, level, pos, context)
        val shape = material.getShape(level, pos, context)
        // 形状为空会让方块连右键都点不到（例如光源方块），退回满方块保证始终可交互。
        return if (shape.isEmpty) super.getShape(state, level, pos, context) else shape
    }

    override fun getCollisionShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape =
        materialAt(level, pos)?.getCollisionShape(level, pos, context)
            ?: super.getCollisionShape(state, level, pos, context)

    override fun getVisualShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape =
        materialAt(level, pos)?.getVisualShape(level, pos, context)
            ?: super.getVisualShape(state, level, pos, context)

    override fun getOcclusionShape(state: BlockState, level: BlockGetter, pos: BlockPos): VoxelShape =
        materialAt(level, pos)?.getOcclusionShape(level, pos)
            ?: super.getOcclusionShape(state, level, pos)

    override fun getLightBlock(state: BlockState, level: BlockGetter, pos: BlockPos): Int =
        materialAt(level, pos)?.getLightBlock(level, pos)
            ?: super.getLightBlock(state, level, pos)

    override fun propagatesSkylightDown(state: BlockState, level: BlockGetter, pos: BlockPos): Boolean =
        materialAt(level, pos)?.propagatesSkylightDown(level, pos)
            ?: super.propagatesSkylightDown(state, level, pos)

    // === 掉落 / 多格方块 ===

    override fun playerWillDestroy(level: Level, pos: BlockPos, state: BlockState, player: Player) {
        if (!level.isClientSide) removeDoorPartner(level, pos)
        super.playerWillDestroy(level, pos, state, player)
    }

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, moved: Boolean) {
        if (state.block !== newState.block) {
            level.removeBlockEntity(pos)
            super.onRemove(state, level, pos, newState, moved)
        }
    }

    // === 材质解析 ===

    /**
     * 把手里那叠方块解析成它“在原位被放置时”的状态：朝向、半砖上下、楼梯朝向、
     * 栅栏/墙/玻璃板的连接、门的铰链方向全部交给原版 [Block.getStateForPlacement] 决定，
     * 再按右键的面修正朝向，最后补上“相邻理念方块也算连接”的那部分。
     */
    fun getMaterialFor(
        level: Level,
        pos: BlockPos,
        stack: ItemStack,
        hit: BlockHitResult,
        player: Player,
        hand: InteractionHand,
    ): BlockState? {
        val block = (stack.item as? BlockItem)?.block ?: return null
        if (!isSupportedMaterial(block)) return null

        val hitAtSelf = BlockHitResult(hit.location, hit.direction, pos, hit.isInside)
        val context = MaterialPlaceContext(player, hand, stack, hitAtSelf, pos)

        val placed = block.getStateForPlacement(context)
        var state = if (placed != null && placed.block === block) placed else block.defaultBlockState()
        if (state.isAir) return null

        state = orientToFace(state, hit.direction)
        state = connectIdeaNeighbours(level, pos, state)
        return state
    }

    /**
     * 能模仿的范围：需要方块实体才能正常工作的方块（箱子、熔炉、床、告示牌……）一律拒绝；
     * 默认状态模型完全由邻居决定的方块（藤蔓、发光地衣）拒绝，否则会渲染成一片空。
     */
    private fun isSupportedMaterial(block: Block): Boolean = when {
        block is IdeaBlock -> false
        block is EntityBlock -> false
        block is AirBlock -> false
        block is LiquidBlock -> false
        block is MultifaceBlock -> false
        block is VineBlock -> false
        else -> true
    }

    /** 右键哪个面，就让可旋转的方块朝哪个面（水平朝向不会被垂直点击写坏）。 */
    private fun orientToFace(state: BlockState, face: Direction): BlockState {
        var result = state
        for (property in state.properties) {
            when (property.name) {
                "facing" -> {
                    @Suppress("UNCHECKED_CAST")
                    val facing = property as Property<Direction>
                    if (face in facing.possibleValues) result = result.setValue(facing, face)
                }

                "axis" -> {
                    @Suppress("UNCHECKED_CAST")
                    val axis = property as Property<Direction.Axis>
                    if (face.axis in axis.possibleValues) result = result.setValue(axis, face.axis)
                }
            }
        }
        return result
    }

    /** 原版的连接计算只认真正的栅栏/墙/玻璃板；相邻理念方块若模仿同一种方块，这里补上连接。 */
    private fun connectIdeaNeighbours(level: Level, pos: BlockPos, state: BlockState): BlockState {
        var result = state
        for (direction in Direction.Plane.HORIZONTAL) {
            val property = state.properties.firstOrNull { it.name == direction.serializedName } ?: continue
            val neighbour = level.getBlockEntity(pos.relative(direction)) as? IdeaBlockEntity ?: continue
            if (!neighbour.material.`is`(state.block)) continue
            when (property) {
                is BooleanProperty -> if (!result.getValue(property)) result = result.setValue(property, true)
                is EnumProperty<*> -> if (property.valueClass == WallSide::class.java) {
                    @Suppress("UNCHECKED_CAST")
                    val wallSide = property as Property<WallSide>
                    if (result.getValue(wallSide) == WallSide.NONE) result = result.setValue(wallSide, WallSide.LOW)
                }
            }
        }
        return result
    }

    private fun playApplySound(level: Level, pos: BlockPos, material: BlockState) {
        if (material.isAir) return
        level.playSound(null, pos, material.soundType.placeSound, SoundSource.BLOCKS, 1f, 0.75f)
    }

    /** 相邻的理念方块若也模仿同一种方块，让它们把“我们”算进连接里（栅栏、墙、玻璃板）。 */
    private fun refreshNeighbourConnections(level: Level, pos: BlockPos, material: BlockState) {
        for (direction in Direction.Plane.HORIZONTAL) {
            val neighbour = level.getBlockEntity(pos.relative(direction)) as? IdeaBlockEntity ?: continue
            val neighbourMaterial = neighbour.material
            if (!neighbourMaterial.`is`(material.block)) continue
            val refreshed = connectIdeaNeighbours(level, pos.relative(direction), neighbourMaterial)
            if (refreshed !== neighbourMaterial) neighbour.applyMaterial(refreshed)
        }
    }

    // === 双格方块（门）：另一半自动补上 / 一起移除 ===

    /** 让门的另一半跟当前形态一致：没有就补一块，有就同步朝向/铰链/开关。 */
    private fun syncDoorPartner(level: Level, pos: BlockPos, material: BlockState) {
        if (material.block !is DoorBlock) return
        val half = material.getValue(DoorBlock.HALF)
        val partnerHalf = if (half == DoubleBlockHalf.LOWER) DoubleBlockHalf.UPPER else DoubleBlockHalf.LOWER
        val partnerPos = pos.relative(if (partnerHalf == DoubleBlockHalf.UPPER) Direction.UP else Direction.DOWN)
        val partnerMaterial = material.setValue(DoorBlock.HALF, partnerHalf)

        val existing = level.getBlockState(partnerPos)
        if (existing.block is IdeaBlock) {
            (level.getBlockEntity(partnerPos) as? IdeaBlockEntity)?.applyMaterial(partnerMaterial)
            return
        }
        if (!existing.canBeReplaced()) return
        level.setBlockAndUpdate(partnerPos, Miehex_revolutionBlocks.IDEA_FULL.get().defaultBlockState())
        (level.getBlockEntity(partnerPos) as? IdeaBlockEntity)?.applyMaterial(partnerMaterial)
    }

    private fun removeDoorPartner(level: Level, pos: BlockPos) {
        val be = level.getBlockEntity(pos) as? IdeaBlockEntity ?: return
        val material = be.material
        if (material.block !is DoorBlock) return
        val half = material.getValue(DoorBlock.HALF)
        val partnerHalf = if (half == DoubleBlockHalf.LOWER) DoubleBlockHalf.UPPER else DoubleBlockHalf.LOWER
        val partnerPos = pos.relative(if (partnerHalf == DoubleBlockHalf.UPPER) Direction.UP else Direction.DOWN)
        val partner = level.getBlockEntity(partnerPos) as? IdeaBlockEntity ?: return
        val partnerMaterial = partner.material
        if (partnerMaterial.block !is DoorBlock) return
        if (partnerMaterial.getValue(DoorBlock.HALF) != partnerHalf) return
        // 只掉一份理念方块，和原版门一致。
        level.removeBlock(partnerPos, false)
    }

    private class MaterialPlaceContext(
        player: Player,
        hand: InteractionHand,
        stack: ItemStack,
        hit: BlockHitResult,
        private val target: BlockPos,
    ) : BlockPlaceContext(player, hand, stack, hit) {
        // 原版上下文会把“非替换方块”的点击位置挪到相邻格，这里必须钉在理念方块自己身上。
        override fun getClickedPos(): BlockPos = target
    }

    companion object {
        /** 被模仿的方块状态；没有材质时返回 null（避免调用方到处判空）。 */
        fun materialAt(level: BlockGetter, pos: BlockPos): BlockState? {
            val material = (level.getBlockEntity(pos) as? IdeaBlockEntity)?.material ?: return null
            if (material.isAir || material.block is IdeaBlock) return null
            return material
        }
    }
}
