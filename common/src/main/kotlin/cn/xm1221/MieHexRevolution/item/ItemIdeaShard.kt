package cn.xm1221.MieHexRevolution.item

import at.petrak.hexcasting.api.casting.eval.vm.CastingVM
import at.petrak.hexcasting.api.casting.iota.EntityIota
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.IotaType
import at.petrak.hexcasting.api.casting.iota.ListIota
import at.petrak.hexcasting.api.casting.iota.NullIota
import at.petrak.hexcasting.api.item.IotaHolderItem
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
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData
import cn.xm1221.MieHexRevolution.util.IotaNbt
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.Item
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.Level
import net.minecraft.world.phys.BlockHitResult

class ItemIdeaShard(properties: Properties): IotaHolderItem, ItemMediaHolder(properties) {
    override fun readIota(stack: ItemStack): Iota? =
        IotaNbt.deserialize(stack.get(DataComponents.CUSTOM_DATA)?.copyTag()?.get("iota"))

    override fun writeable(stack: ItemStack): Boolean {
        return false
    }


    override fun canWrite(
        stack: ItemStack,
        iota: Iota?
    ): Boolean {
        return false
    }

    override fun writeDatum(
        stack: ItemStack,
        iota: Iota?
    ) {
        if (iota == null) return
        writeTag(stack, "iota", IotaNbt.serialize(iota))
    }

    override fun canProvideMedia(stack: ItemStack): Boolean {
        return false
    }

    override fun canRecharge(stack: ItemStack): Boolean {
       return false
    }

    override fun useOn(context: UseOnContext): InteractionResult {
        if(context.level.isClientSide){
            return InteractionResult.SUCCESS
        }
        val stack = context.itemInHand
        val world = context.level as ServerLevel
        val data = readIota(stack) ?: return InteractionResult.PASS
        val env = IdeaCastingEnv(context.player, world, stack, data, context.hand)
        val vm = CastingVM.empty(env)
        vm.queueExecuteAndWrapIotas(readSpell(stack), world)
        stack.shrink(1)
        return InteractionResult.CONSUME
    }

    override fun use(level: Level, player: Player, usedHand: InteractionHand): InteractionResultHolder<ItemStack> {
        val stack = player.getItemInHand(usedHand)
        if(stack.item is ItemIdeaShard){
            val context = UseOnContext(player, usedHand,
                BlockHitResult(player.position(), Direction.NORTH, player.blockPosition(), false)
            )
            useOn(context)
            return InteractionResultHolder.success(stack)
        }
        return super.use(level, player, usedHand)
    }

    fun readSpell(stack: ItemStack): List<Iota> {
        val spell = IotaNbt.deserialize(stack.get(DataComponents.CUSTOM_DATA)?.copyTag()?.get("spell"))
        if(spell is ListIota){
            return spell.list.toList()
        }
        return listOfNotNull(spell)
    }

    fun writeSpell(stack: ItemStack,data:Iota) {
        writeTag(stack, "spell", IotaNbt.serialize(data))
    }

    /** Merge one entry into the stack's custom NBT component (1.21 has no `stack.tag`). */
    private fun writeTag(stack: ItemStack, key: String, value: net.minecraft.nbt.Tag) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack) { it.put(key, value) }
    }

    override fun isBarVisible(pStack: ItemStack): Boolean {
        return true
    }



}