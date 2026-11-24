package cn.xm1221.MieHexRevolution.casting.actions.useful.envs.worker

import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
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
 * 远程施法：在原 executeInner 之前按玩家的远程施法状态同步法杖 VM 的环境。
 * 支持从 [StaffCastEnv] 进入目标环境，也支持同一条施法内退出或切换目标时恢复/更新环境。
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
        val baseEnv = unwrapStaffEnv(env) ?: return call
        val caster = baseEnv.castingEntity as? ServerPlayer ?: return call
        val target = PlayerRemoteCastData.get(caster)
        if (target == null) {
            // 同一条施法里刚解除过：把环境换回法杖环境，别把上一条图案的假环境接着用下去。
            if (env !== baseEnv) {
                vm.replaceEnv(baseEnv)
            }
            return call
        }
        val alreadyMatches = when (env) {
            is PlayersCastingEnv -> env.friend == target
            is FakeCastingEnv -> env.target == target
            else -> false
        }
        if (!alreadyMatches) {
            val next = if (target is ServerPlayer) {
                PlayersCastingEnv(baseEnv, target)
            } else {
                FakeCastingEnv(baseEnv, target)
            }
            vm.replaceEnv(next)
        }
        return call
    }

    private tailrec fun unwrapStaffEnv(env: CastingEnvironment): StaffCastEnv? = when (env) {
        is StaffCastEnv -> env
        is FakeCastingEnv -> unwrapStaffEnv(env.env)
        is PlayersCastingEnv -> unwrapStaffEnv(env.env)
        else -> null
    }
}
