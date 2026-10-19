package cn.xm1221.MieHexRevolution.casting.actions.spells.idea

import at.petrak.hexcasting.api.HexAPI
import at.petrak.hexcasting.api.casting.RenderedSpell
import at.petrak.hexcasting.api.casting.castables.SpellAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.iota.EntityIota
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.mishaps.MishapBadCaster
import at.petrak.hexcasting.api.casting.mishaps.MishapBadOffhandItem
import at.petrak.hexcasting.api.casting.mishaps.MishapOthersName
import at.petrak.hexcasting.api.misc.MediaConstants
import at.petrak.hexcasting.api.utils.putLong
import at.petrak.hexcasting.common.items.magic.ItemMediaHolder
import at.petrak.hexcasting.common.lib.HexItems
import cn.xm1221.MieHexRevolution.item.ItemIdeaShard
import cn.xm1221.MieHexRevolution.registry.Miehex_revolutionBlocks
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack

class OpIdeaShardWrite: SpellAction {
    override val argc: Int
        get() = 2

    override fun execute(
        args: List<Iota>,
        env: CastingEnvironment
    ): SpellAction.Result {
        val data = args.get(0)
        val spell = args.get(1)
        val caster = env.castingEntity ?: throw MishapBadCaster()
        val itemstack = caster.getItemInHand(env.otherHand)
        if (itemstack.item !== HexItems.QUENCHED_SHARD) {
            throw MishapBadOffhandItem.of(itemstack, "quenched_shard")
        }
        if(data is EntityIota && data.entity is ServerPlayer && data.entity != env.castingEntity){
            throw MishapOthersName(data.entity as Player)
        }
        return SpellAction.Result(
            effect = Result(data, spell),
            cost = MediaConstants.QUENCHED_SHARD_UNIT,
            particles = listOf()
        )
    }

    class Result(val data: Iota, val spell: Iota) : RenderedSpell {
        override fun cast(env: CastingEnvironment) {
            val caster = env.castingEntity ?: return
            val offhand = caster.getItemInHand(env.otherHand)
            offhand.shrink(1)

            val shard = Miehex_revolutionBlocks.IDEA_SHARD.get()
            val stack = ItemStack(shard)
            shard.writeDatum(stack, data)
            shard.writeSpell(stack, spell)
            stack.putLong(ItemMediaHolder.TAG_MAX_MEDIA, MediaConstants.QUENCHED_SHARD_UNIT)
            shard.setMedia(stack, MediaConstants.QUENCHED_SHARD_UNIT)
            caster.spawnAtLocation(stack)
        }
    }
}
