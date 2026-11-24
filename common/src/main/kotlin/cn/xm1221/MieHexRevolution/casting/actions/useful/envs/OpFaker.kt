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

/**
 * `changeenv`：把施法者的“远程施法目标”设成给定实体，之后同一条施法里
 * 法杖 VM 的环境会被换成那个实体的环境（见 [cn.xm1221.MieHexRevolution.casting.actions.useful.envs.worker.RemoteCastEnvWorker]）。
 *
 * <p>注意：这条动作自己在远程环境里执行时，`env.castingEntity` 已经是**目标**而不是施法者，
 * 所以要先 [resolveRealEnv] 剥回真正的施法环境，否则会把自己的身份当成目标、越套越远。
 */
class OpFaker : ConstMediaAction {
    override val argc: Int
        get() = 1

    override fun execute(
        args: List<Iota>,
        env: CastingEnvironment
    ): List<Iota> {
        val entity = args.getLivingEntityButNotArmorStand(env.world, 0, argc)
        val realEnv = resolveRealEnv(env)
        val caster = realEnv.castingEntity
        if (caster !is ServerPlayer) {
            throw MishapBadCaster()
        }
        // 指自己 = 解除远程施法。
        if (entity == caster) {
            PlayerRemoteCastData.clear(caster)
            return listOf()
        }
        if (entity !is ServerPlayer) {
            PlayerRemoteCastData.set(caster, entity)
            return listOf()
        }
        // 代替别的玩家施法需要对方的真名（媒介里存着的那个实体 iota），否则任何人都能冒名。
        val stack = caster.getItemInHand(realEnv.otherHand)
        val item = stack.item
        if (item is IotaHolderItem) {
            val iota = item.readIota(stack)
            if (!(iota is EntityIota && iota.entityId == entity.uuid)) {
                throw MishapOthersName(entity)
            }
            PlayerRemoteCastData.set(caster, entity)
            return listOf()
        }
        throw MishapOthersName(entity)
    }

    /** 远程环境里 `castingEntity` 已经被换成了目标，要一路剥回原环境才能拿到真正的施法者。 */
    private tailrec fun resolveRealEnv(env: CastingEnvironment): CastingEnvironment = when (env) {
        is FakeCastingEnv -> resolveRealEnv(env.env)
        is PlayersCastingEnv -> resolveRealEnv(env.env)
        else -> env
    }
}
