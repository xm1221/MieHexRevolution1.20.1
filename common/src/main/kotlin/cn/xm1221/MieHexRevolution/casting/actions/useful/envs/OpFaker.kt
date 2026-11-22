package cn.xm1221.MieHexRevolution.casting.actions.useful.envs

import at.petrak.hexcasting.api.casting.castables.ConstMediaAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.getLivingEntityButNotArmorStand
import at.petrak.hexcasting.api.casting.iota.EntityIota
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.mishaps.MishapBadCaster
import at.petrak.hexcasting.api.casting.mishaps.MishapOthersName
import at.petrak.hexcasting.api.item.IotaHolderItem
import cn.xm1221.MieHexRevolution.casting.env.FakeCastingEnv
import cn.xm1221.MieHexRevolution.casting.env.PlayersCastingEnv
import cn.xm1221.MieHexRevolution.util.PlayerRemoteCastData
import net.minecraft.server.level.ServerPlayer

class OpFaker: ConstMediaAction {
    override val argc: Int
        get() = 1

    override fun execute(
        args: List<Iota>,
        env: CastingEnvironment
    ): List<Iota> {
        val entity = args.getLivingEntityButNotArmorStand(0, argc)
        val realEnv = resolveRealEnv(env)
        val caster = realEnv.castingEntity
        if (caster !is ServerPlayer) {
            throw MishapBadCaster()
        }
        if (entity == caster) {
            PlayerRemoteCastData.clear(caster)
            return listOf()
        }
        if (entity !is ServerPlayer) {
            PlayerRemoteCastData.set(caster, entity)
            return listOf()
        }
        val stack = caster.getItemInHand(realEnv.otherHand)
        val item = stack.item
        if (item is IotaHolderItem) {
            val iota = item.readIota(stack, realEnv.world)
            if (!(iota is EntityIota && iota.entity == entity)) {
                throw MishapOthersName(entity)
            }
            PlayerRemoteCastData.set(caster, entity)
            return listOf()
        }
        throw MishapOthersName(entity)
    }

    private tailrec fun resolveRealEnv(env: CastingEnvironment): CastingEnvironment = when (env) {
        is FakeCastingEnv -> resolveRealEnv(env.env)
        is PlayersCastingEnv -> resolveRealEnv(env.env)
        else -> env
    }
}