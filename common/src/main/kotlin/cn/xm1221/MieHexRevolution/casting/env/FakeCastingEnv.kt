package cn.xm1221.MieHexRevolution.casting.env

import at.petrak.hexcasting.api.HexAPI
import at.petrak.hexcasting.api.casting.ParticleSpray
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.eval.MishapEnvironment
import at.petrak.hexcasting.api.casting.eval.env.PlayerBasedCastEnv
import at.petrak.hexcasting.api.pigment.FrozenPigment
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3
import java.util.function.Predicate

class FakeCastingEnv(val env: CastingEnvironment,val target: LivingEntity): CastingEnvironment(env.world), ExecutionColorProvider {
    override fun getCastingEntity(): LivingEntity {
        return target
    }

    override fun getMishapEnvironment(): MishapEnvironment? {
     return env.mishapEnvironment
    }

    override fun mishapSprayPos(): Vec3? {
        return env.mishapSprayPos()
    }

    override fun extractMediaEnvironment(cost: Long, simulate: Boolean): Long {
        return env.extractMedia(cost, simulate)
    }

    /**
     * 跳过对目标实体的 MEDIA_CONSUMPTION_MODIFIER 属性查询（普通生物没有该属性，会内部错误），
     * 直接委托原 env（对真正的施法者应用一次倍率并抽取媒质）。
     */
    override fun extractMedia(cost: Long, simulate: Boolean): Long {
        return env.extractMedia(cost, simulate)
    }

    override fun isVecInRangeEnvironment(vec: Vec3?): Boolean {
        return env.isVecInRange(vec)

    }

    override fun hasEditPermissionsAtEnvironment(pos: BlockPos?): Boolean {
        return env.hasEditPermissionsAt(pos)
    }

    override fun getCastingHand(): InteractionHand? {
       return env.castingHand
    }

    override fun getUsableStacks(mode: StackDiscoveryMode?): List<ItemStack?>? {
        return getUsableStacksForPlayer(mode, env.castingHand,env.castingEntity as ServerPlayer);
    }


    override fun getPrimaryStacks(): List<HeldItemInfo?>? {
      return  getPrimaryStacksForPlayer(env.castingHand, env.castingEntity as ServerPlayer)
    }

    override fun replaceItem(
        stackOk: Predicate<ItemStack?>?,
        replaceWith: ItemStack?,
        hand: InteractionHand?
    ): Boolean {
        return env.replaceItem(stackOk, replaceWith, hand)
    }

    override fun getPigment(): FrozenPigment? {
        return env.pigment
    }

    override fun setPigment(pigment: FrozenPigment?): FrozenPigment? {
        return env.setPigment(pigment)
    }

    override fun produceParticles(
        particles: ParticleSpray?,
        colorizer: FrozenPigment?
    ) {
        return env.produceParticles(particles, colorizer)
    }

    override fun printMessage(message: Component?) {
        target.sendSystemMessage(message)
       return env.printMessage(message)
    }

    override fun getExecutionColor(): Int {
        return ExecutionColorProvider.FAKE_COLOR
    }
}