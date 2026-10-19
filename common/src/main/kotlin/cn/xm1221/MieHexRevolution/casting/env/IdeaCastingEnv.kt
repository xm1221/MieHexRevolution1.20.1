package cn.xm1221.MieHexRevolution.casting.env

import at.petrak.hexcasting.api.HexAPI
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.eval.MishapEnvironment
import at.petrak.hexcasting.api.casting.eval.env.PlayerBasedCastEnv
import at.petrak.hexcasting.api.casting.eval.env.PlayerBasedMishapEnv
import at.petrak.hexcasting.api.casting.iota.EntityIota
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.Vec3Iota
import at.petrak.hexcasting.api.pigment.FrozenPigment
import at.petrak.hexcasting.api.utils.getInt
import at.petrak.hexcasting.api.utils.getLong
import at.petrak.hexcasting.api.utils.putLong
import at.petrak.hexcasting.api.utils.hasInt
import net.minecraft.commands.arguments.coordinates.Vec2Argument.vec2
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3

class IdeaCastingEnv( caster: LivingEntity?, world: ServerLevel,val item: ItemStack,val data: Iota,hand: InteractionHand): PlayerBasedCastEnv(
    caster as ServerPlayer?,hand) {
    val TAG_MEDIA: String = "hexcasting:media"
    val TAG_MAX_MEDIA: String = "hexcasting:start_media"

    override fun getCastingEntity(): LivingEntity? {
        return caster
    }

    override fun getMishapEnvironment(): MishapEnvironment? {
        if(caster is ServerPlayer) {
            return PlayerBasedMishapEnv(caster)
        }
        return null
    }

    override fun mishapSprayPos(): Vec3? {
        if (caster != null) {
            return caster.position()
        }
        return null
    }

    override fun extractMediaEnvironment(cost: Long, simulate: Boolean): Long {
        if (caster is ServerPlayer && caster.isCreative) return 0
        val available = item.getLong(TAG_MEDIA)
        val extracted = minOf(cost, available)
        if (!simulate && extracted > 0) {
            item.putLong(TAG_MEDIA, available - extracted)
        }
        return cost - extracted
    }


    override fun isVecInRangeEnvironment(vec: Vec3?): Boolean {
        if(vec == null) {
            return false
        }
        if(data is Vec3Iota && isInSameBlock(data.vec3,vec)){
            return true
        }
        else if(data is EntityIota &&(isInSameBlock(data.entity.eyePosition,vec)|| isInSameBlock(data.entity.eyePosition,vec)) ) {
            return true
        }
        return super.isVecInRangeEnvironment(vec)
    }



    override fun getCastingHand(): InteractionHand? {
        return castingHand
    }

    override fun getPigment(): FrozenPigment? {
        if(caster !is ServerPlayer) {
            return null
        }
        return HexAPI.instance().getColorizer(caster);
    }

    fun isInSameBlock(vec1: Vec3, vec2:Vec3): Boolean{
        return BlockPos(vec1.x.toInt(), vec1.y.toInt(), vec1.z.toInt())==BlockPos(vec2.x.toInt(), vec2.y.toInt(), vec2.z.toInt())
    }



}