package cn.xm1221.MieHexRevolution.block.idea

import cn.xm1221.MieHexRevolution.registry.Miehex_revolutionBlockEntities
import cn.xm1221.MieHexRevolution.registry.Miehex_revolutionBlocks
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtUtils
import net.minecraft.sounds.SoundSource
import net.minecraft.stats.Stats
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.ItemInteractionResult
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.AirBlock
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
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
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf
import net.minecraft.world.level.block.state.properties.EnumProperty
import net.minecraft.world.level.block.state.properties.IntegerProperty
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
 * <p>交互约定（站立 = 不潜行）：
 * - 潜行空手右键：退回“理念本体”的外观；
 * - 潜行时手持方块：交还原版处理（正常放置方块）；
 * - 站立空手右键、或站立手持**同一种**方块右键：循环当前外观的形态（朝向 / 半砖上下 / 开关），
 *   若它模仿的是栅栏、墙或玻璃板，则右键点哪一面就切换那一面的连接（对面同步跟着改）；
 * - 站立手持**另一种**方块右键：直接换上那种方块的外观。
 *
 * <p>1.21 把“手持物品右键”与“空手右键”拆成了 [useItemOn] / [useWithoutItem] 两条：
 * 空手那条在原版流程里排在 [useItemOn] 之后，所以这里按手上有无物品分工，
 * 语义与 1.20.1 的单一 `use` 完全一致。
 */
abstract class IdeaBlock(properties: BlockBehaviour.Properties) : Block(properties), EntityBlock {

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        IdeaBlockEntity(Miehex_revolutionBlockEntities.IDEA_BE.get(), pos, state)

    override fun <T : BlockEntity> getTicker(level: Level, state: BlockState, type: BlockEntityType<T>): BlockEntityTicker<T>? = null

    // === 交互 ===

    override fun useItemOn(
        stack: ItemStack, state: BlockState, level: Level, pos: BlockPos,
        player: Player, hand: InteractionHand, hit: BlockHitResult,
    ): ItemInteractionResult {
        // 空手走 useWithoutItem；潜行时整段交还原版（放方块 / 空手还原）。
        if (stack.isEmpty || player.isShiftKeyDown) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
        }
        val be = level.getBlockEntity(pos) as? IdeaBlockEntity
            ?: return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
        if (stack.item !is BlockItem) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
        }
        val material = getMaterialFor(level, pos, stack, hit, player, hand)
            ?: return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
        if (level.isClientSide) return ItemInteractionResult.sidedSuccess(true)

        if (be.hasCustomMaterial && be.material.`is`(material.block)) {
            // 同一种方块：依次循环朝向 / 台阶上下 / 开关状态等；没得循环就按当前邻居刷新连接。
            val outcome = be.cycleMaterial(hit.direction)
            if (outcome == IdeaBlockEntity.CycleOutcome.NONE) be.applyMaterial(material)
            finishCycle(level, pos, be, outcome)
            return ItemInteractionResult.sidedSuccess(false)
        }

        // 换一种方块：直接用新外观覆盖（不再需要先空手潜行还原）。
        removeDoorPartner(level, pos)
        be.applyMaterial(material)
        syncDoorPartner(level, pos, material)
        refreshNeighbourConnections(level, pos, material)
        playApplySound(level, pos, material)
        return ItemInteractionResult.sidedSuccess(false)
    }

    override fun useWithoutItem(
        state: BlockState, level: Level, pos: BlockPos,
        player: Player, hit: BlockHitResult,
    ): InteractionResult {
        val be = level.getBlockEntity(pos) as? IdeaBlockEntity ?: return InteractionResult.PASS
        // 手上还有东西（非方块物品也在此列）：让原版继续走那个物品自己的用法。
        if (!player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty) return InteractionResult.PASS

        // 潜行空手：退回理念本体。
        if (player.isShiftKeyDown) {
            if (!be.hasCustomMaterial) return InteractionResult.PASS
            if (level.isClientSide) return InteractionResult.SUCCESS
            removeDoorPartner(level, pos)
            be.resetMaterial()
            level.playSound(null, pos, state.soundType.placeSound, SoundSource.BLOCKS, 1f, 1f)
            return InteractionResult.SUCCESS
        }

        // 站立空手：和“手持同一种方块”一样，循环当前外观的形态 / 连接。
        if (!be.hasCustomMaterial) return InteractionResult.PASS
        if (level.isClientSide) return InteractionResult.SUCCESS
        val outcome = be.cycleMaterial(hit.direction)
        if (outcome == IdeaBlockEntity.CycleOutcome.NONE) return InteractionResult.PASS
        finishCycle(level, pos, be, outcome)
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

    /**
     * 挡光量直接读方块状态里存好的 [LIGHT]，不现算材质。
     *
     * <p>这条是环境光遮蔽与光照引擎共用的判据，两边都会在“拿不到方块实体”的时机问它；
     * 详见 [LIGHT] 的说明。
     */
    override fun getLightBlock(state: BlockState, level: BlockGetter, pos: BlockPos): Int =
        state.getValue(LIGHT)

    override fun propagatesSkylightDown(state: BlockState, level: BlockGetter, pos: BlockPos): Boolean =
        materialAt(level, pos)?.propagatesSkylightDown(level, pos)
            ?: super.propagatesSkylightDown(state, level, pos)

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        super.createBlockStateDefinition(builder)
        builder.add(LIGHT)
    }

    // === 掉落 / 多格方块 ===

    override fun playerWillDestroy(level: Level, pos: BlockPos, state: BlockState, player: Player): BlockState {
        if (!level.isClientSide) removeDoorPartner(level, pos)
        return super.playerWillDestroy(level, pos, state, player)
    }

    /**
     * 挖掘掉落：把材质一起带进掉落物。
     *
     * <p>1.20.1 是靠掉落表里的 `copy_nbt`（`source: block_entity`）做的，1.21 把它拆掉了：
     * `copy_custom_data` 只认 `storage` / `context` 两种 nbt 来源，读不到方块实体；
     * `copy_components` 只搬“组件”，材质存在我们自己的一层 NBT 里，不是组件。
     * 所以这一路改成自己发掉落物：材质写进 [net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA]，
     * 放下时原版 `BlockItem` 会自动把它灌回方块实体，与“破坏再放回去还能保持外观”的旧行为一致。
     *
     * <p>爆炸那一路仍走掉落表（掉落表保持原样，材质丢失），与“爆炸炸掉内容物”的直觉一致。
     */
    override fun playerDestroy(
        level: Level, player: Player, pos: BlockPos, state: BlockState,
        blockEntity: BlockEntity?, tool: ItemStack,
    ) {
        player.awardStat(Stats.BLOCK_MINED.get(this))
        player.causeFoodExhaustion(0.005f)
        val be = blockEntity as? IdeaBlockEntity
        val drop = ItemStack(this)
        if (be != null && be.hasCustomMaterial) {
            val tag = CompoundTag()
            tag.put("Material", NbtUtils.writeBlockState(be.material))
            CustomData.set(DataComponents.BLOCK_ENTITY_DATA, drop, tag)
        }
        popResource(level, pos, drop)
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

    /** 切完形态后的收尾：补门另一半、按邻居刷新连接、放声音。手动切连接时对面已经同步过，别再刷回去。 */
    private fun finishCycle(
        level: Level,
        pos: BlockPos,
        be: IdeaBlockEntity,
        outcome: IdeaBlockEntity.CycleOutcome,
    ) {
        if (outcome != IdeaBlockEntity.CycleOutcome.CONNECTION) {
            syncDoorPartner(level, pos, be.material)
            refreshNeighbourConnections(level, pos, be.material)
        }
        playApplySound(level, pos, be.material)
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

    /**
     * 材质挡多少光（0..15），跟着材质一起写进方块状态。
     *
     * <p>为什么不能像形状那样现查方块实体：光照引擎与环境光遮蔽两边都会问这个值，
     * 而客户端重建区块网格时拿到的是
     * [net.minecraft.client.renderer.chunk.RenderChunkRegion]，它的 `getBlockEntity`
     * 借的是这个渲染块**上一次编译**留下的实体表 —— 刚放下的方块查不到，于是同一块
     * 方块会时而被当成实心、时而被当成透光，四周的接触阴影跟着忽深忽浅。
     * 把值放进状态（状态一定随区块数据一起送到重建现场）两边就都稳定了。
     */
    companion object {
        val LIGHT: IntegerProperty = IntegerProperty.create("light", 0, 15)

        /** 被模仿的方块状态；没有材质时返回 null（避免调用方到处判空）。 */
        fun materialAt(level: BlockGetter, pos: BlockPos): BlockState? {
            val material = (level.getBlockEntity(pos) as? IdeaBlockEntity)?.material ?: return null
            if (material.isAir || material.block is IdeaBlock) return null
            return material
        }

        /** 兼容入口：拿不到时给空气。 */
        fun getMaterial(level: BlockGetter, pos: BlockPos): BlockState =
            (level.getBlockEntity(pos) as? IdeaBlockEntity)?.material ?: Blocks.AIR.defaultBlockState()
    }
}
