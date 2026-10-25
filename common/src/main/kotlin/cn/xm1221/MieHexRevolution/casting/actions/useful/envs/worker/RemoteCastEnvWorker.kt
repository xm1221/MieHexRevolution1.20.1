package cn.xm1221.MieHexRevolution.casting.actions.useful.envs.worker

import at.petrak.hexcasting.api.casting.eval.env.StaffCastEnv
import at.petrak.hexcasting.api.casting.eval.vm.CastingVM
import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation
import at.petrak.hexcasting.api.casting.iota.Iota
import cn.xm1221.MieHexRevolution.casting.env.FakeCastingEnv
import cn.xm1221.MieHexRevolution.casting.env.PlayersCastingEnv
import cn.xm1221.MieHexRevolution.util.PlayerRemoteCastData
import cn.xm1221.MieLib.vm.replaceEnv
import cn.xm1221.MieLib.worker.CastingVMCall
import cn.xm1221.MieLib.worker.CastingVMWorker
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer

/**
 * 远程施法：在原 executeInner 之前把法杖 VM 的环境换成目标实体的环境。
 * 只在当前仍是 [StaffCastEnv] 时换一次，避免同一条施法里反复嵌套。
 */
object RemoteCastEnvWorker : CastingVMWorker(isAfter = false, priority = 1) {
    override fun execute(
        iota: Iota,
        world: ServerLevel,
        cont: SpellContinuation,
        call: CastingVMCall,
        vm: CastingVM,
    ): CastingVMCall {
        val env = vm.env
        val caster = env.castingEntity
        if (env is StaffCastEnv && caster is ServerPlayer) {
            val target = PlayerRemoteCastData.get(caster)
            if (target != null) {
                val next = if (target is ServerPlayer) {
                    PlayersCastingEnv(env, target)
                } else {
                    FakeCastingEnv(env, target)
                }
                vm.replaceEnv(next)
            }
        }
        return call
    }
}
