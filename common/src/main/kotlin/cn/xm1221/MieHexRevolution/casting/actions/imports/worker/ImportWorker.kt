package cn.xm1221.MieHexRevolution.casting.actions.imports.worker

import at.petrak.hexcasting.api.casting.eval.vm.CastingVM
import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.PatternIota
import cn.xm1221.MieHexRevolution.util.executeWithImports
import cn.xm1221.MieLib.worker.CastingVMCall
import cn.xm1221.MieLib.worker.CastingVMWorker
import net.minecraft.server.level.ServerLevel

/**
 * 在原 `PatternIota.execute` 之前尝试导入。有绑定就 [CastingVMCall.setResult]，
 * 原方法不再跑；没有绑定则保持 `result == null`，交给 Hex 自己的执行。
 */
object ImportWorker : CastingVMWorker(isAfter = false, priority = 10) {
    override fun execute(
        iota: Iota,
        world: ServerLevel,
        cont: SpellContinuation,
        call: CastingVMCall,
        vm: CastingVM,
    ): CastingVMCall {
        if (iota is PatternIota) {
            val result = iota.executeWithImports(vm, world, cont)
            if (result != null) {
                call.setResult(result)
            }
        }
        return call
    }
}
