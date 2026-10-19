package cn.xm1221.MieHexRevolution.casting.env

import at.petrak.hexcasting.api.casting.eval.env.PlayerBasedCastEnv
import at.petrak.hexcasting.api.pigment.FrozenPigment
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand

class PlayersCastingEnv(val env: PlayerBasedCastEnv, val friend: ServerPlayer): PlayerBasedCastEnv(friend,env.castingHand), ExecutionColorProvider {
    override fun extractMediaEnvironment(cost: Long, simulate: Boolean): Long {
       return env.extractMedia(cost, simulate)
    }

    /** 媒质消耗直接委托原 env（对真正的施法者应用倍率并抽取），避免按好友属性再算一次 */
    override fun extractMedia(cost: Long, simulate: Boolean): Long {
        return env.extractMedia(cost, simulate)
    }

    override fun getCastingHand(): InteractionHand? {
        return env.castingHand
    }

    override fun getPigment(): FrozenPigment? {
       return env.pigment
    }

    override fun getExecutionColor(): Int {
        return ExecutionColorProvider.PLAYERS_COLOR
    }
}