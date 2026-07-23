package cn.xm1221.MieHexRevolution.block.idea

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.shapes.Shapes
import cn.xm1221.MieHexRevolution.registry.Miehex_revolutionBlockEntities

abstract class IdeaBlock(properties: Properties) : Block(properties), EntityBlock {

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        IdeaBlockEntity(Miehex_revolutionBlockEntities.IDEA_BE.get(), pos, state)

    override fun <T : BlockEntity> getTicker(level: Level, state: BlockState, type: BlockEntityType<T>): BlockEntityTicker<T>? = null

    // === Interaction (1.20.1: use, not useItemOn) ===

    override fun use(
        state: BlockState, level: Level, pos: BlockPos,
        player: Player, hand: InteractionHand, hit: BlockHitResult,
    ): InteractionResult {
        val stack = player.getItemInHand(hand)

        // Shift + empty hand = reset material
        if (player.isShiftKeyDown && stack.isEmpty) {
            val be = level.getBlockEntity(pos) as? IdeaBlockEntity
            if (be != null && be.hasCustomMaterial && !level.isClientSide) {
                be.resetMaterial()
                level.playSound(null, pos, SoundType.STONE.breakSound, SoundSource.BLOCKS, 1f, 1f)
            }
            return InteractionResult.SUCCESS
        }

        val material = getAcceptedBlockState(level, pos, stack, hit.direction)
            ?: return InteractionResult.PASS

        val be = level.getBlockEntity(pos) as? IdeaBlockEntity
            ?: return InteractionResult.PASS

        if (be.material.`is`(material.block)) {
            if (!be.cycleMaterial())
                return InteractionResult.PASS
            level.playSound(null, pos, material.soundType.placeSound, SoundSource.BLOCKS, 0.75f, 0.95f)
            return InteractionResult.SUCCESS
        }
        if (be.hasCustomMaterial)
            return InteractionResult.PASS
        if (level.isClientSide)
            return InteractionResult.SUCCESS

        be.setMaterial(material)
        level.playSound(null, pos, material.soundType.placeSound, SoundSource.BLOCKS, 1f, 0.75f)
        return InteractionResult.SUCCESS
    }

    override fun setPlacedBy(level: Level, pos: BlockPos, state: BlockState, placer: LivingEntity?, stack: ItemStack) {
        if (placer == null) return
        val offhand = placer.getItemInHand(InteractionHand.OFF_HAND)
        val applied = getAcceptedBlockState(level, pos, offhand, Direction.orderedByNearest(placer)[0]) ?: return
        val be = level.getBlockEntity(pos) as? IdeaBlockEntity ?: return
        if (be.hasCustomMaterial) return
        be.setMaterial(applied)
    }

    // === Material validation ===

    open fun getAcceptedBlockState(level: Level?, pos: BlockPos?, stack: ItemStack, face: Direction?): BlockState? {
        if (stack.item !is BlockItem) return null
        val block = (stack.item as BlockItem).block
        if (block is IdeaBlock) return null
        var state = block.defaultBlockState()

        if (block is EntityBlock) return null
        if (block is StairBlock || block is SlabBlock || block is TrapDoorBlock || block is IronBarsBlock) return null

        if (level != null && pos != null) {
            val shape = state.getShape(level, pos)
            if (shape.isEmpty || shape != Shapes.block()) return null
            val collision = state.getCollisionShape(level, pos)
            if (collision.isEmpty) return null
        }

        if (face != null) {
            val axis = face.axis
            if (state.hasProperty(BlockStateProperties.FACING))
                state = state.setValue(BlockStateProperties.FACING, face)
            if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING) && axis != Direction.Axis.Y)
                state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, face)
            if (state.hasProperty(BlockStateProperties.AXIS))
                state = state.setValue(BlockStateProperties.AXIS, axis)
        }
        return state
    }

    // === Drops ===

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, moved: Boolean) {
        if (state.block !== newState.block) {
            level.removeBlockEntity(pos)
            super.onRemove(state, level, pos, newState, moved)
        }
    }

    // === Material sound ===

    override fun getSoundType(state: BlockState): SoundType {
        // We can't access level/pos here in 1.20.1, defer with a simple override per-instance.
        // The block's base sound type is used; the BER handles visuals only.
        return super.getSoundType(state)
    }

   fun getCloneItemStack(
        blockState: BlockState, target: HitResult, level: LevelReader, pos: BlockPos, player: Player?
    ): ItemStack {
        return ItemStack(this)
    }

    companion object {
        fun getMaterial(level: BlockGetter, pos: BlockPos): BlockState {
            return (level.getBlockEntity(pos) as? IdeaBlockEntity)?.material ?: Blocks.AIR.defaultBlockState()
        }
    }
}
