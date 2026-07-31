package cn.xm1221.MieHexRevolution.item

import at.petrak.hexcasting.api.casting.SpellList
import at.petrak.hexcasting.api.casting.eval.vm.CastingVM
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.IotaType
import at.petrak.hexcasting.api.casting.iota.ListIota
import at.petrak.hexcasting.api.item.IotaHolderItem
import at.petrak.hexcasting.api.utils.getTag
import at.petrak.hexcasting.api.utils.putTag
import at.petrak.hexcasting.common.items.magic.ItemMediaHolder
import cn.xm1221.MieHexRevolution.casting.env.IdeaCastingEnv
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.Vec3i
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import at.petrak.hexcasting.ktxt.UseOnContext
import net.minecraft.network.chat.Component
import net.minecraft.world.item.Item
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.Level
import net.minecraft.world.phys.BlockHitResult

class ItemIdeaShard(properties: Properties): IotaHolderItem, ItemMediaHolder(properties) {
    override fun readIotaTag(stack: ItemStack?): CompoundTag? {
        if (stack != null) {
            return stack.getTag("iota") as CompoundTag
        }
        return null
    }

    override fun writeable(stack: ItemStack?): Boolean {
        return false
    }

    override fun canWrite(
        stack: ItemStack?,
        iota: Iota?
    ): Boolean {
        return false
    }

    override fun writeDatum(
        stack: ItemStack?,
        iota: Iota?
    ) {
        stack?.putTag("iota", IotaType.serialize(iota))
    }

    override fun canProvideMedia(stack: ItemStack?): Boolean {
        return false
    }

    override fun canRecharge(stack: ItemStack?): Boolean {
       return false
    }

    override fun useOn(context: UseOnContext?): InteractionResult? {
        if(context==null || context.level.isClientSide){
            return InteractionResult.SUCCESS
        }
        val stack = context.itemInHand
        val world = context.level as ServerLevel
        val data = readIota(stack, world) ?: return InteractionResult.PASS
        val env = IdeaCastingEnv(context.player, world, stack, data, context.hand)
        val vm = CastingVM.empty(env)
        vm.queueExecuteAndWrapIotas(readSpell(stack, world), world)
        stack.shrink(1)
        return InteractionResult.CONSUME
    }

    override fun use(level: Level?, player: Player?, usedHand: InteractionHand?): InteractionResultHolder<ItemStack?>? {
        if(player==null || level==null||usedHand==null){
          return super.use(level, player, usedHand)
        }
        val stack = player.getItemInHand(usedHand)
        if(stack.item is ItemIdeaShard){
            val context = at.petrak.hexcasting.ktxt.UseOnContext(level, player, usedHand, stack,
                BlockHitResult(player.position(), Direction.NORTH, player.blockPosition(), false)
            )
            useOn(context)
            return InteractionResultHolder.success(stack)
        }
        return super.use(level, player, usedHand)
    }

    fun readSpell(stack: ItemStack,world: ServerLevel): List<Iota> {
        val tag = stack.getTag("spell") as CompoundTag
        val spell = IotaType.deserialize(tag,world)
        if(spell is ListIota){
            return spell.list.toList()
        }
        return listOf(spell)
    }

    fun writeSpell(stack: ItemStack,data:Iota) {
        val tag = IotaType.serialize(data)
        stack.putTag("spell", tag)
    }

    override fun isBarVisible(pStack: ItemStack?): Boolean {
        return true
    }



}