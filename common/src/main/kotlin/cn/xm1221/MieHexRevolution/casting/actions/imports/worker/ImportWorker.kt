package cn.xm1221.MieHexRevolution.casting.actions.imports.worker

import at.petrak.hexcasting.api.casting.eval.vm.CastingVM
import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.PatternIota
import cn.xm1221.MieHexRevolution.Miehex_revolution
import cn.xm1221.MieHexRevolution.util.executeWithImports
import cn.xm1221.MieLib.worker.CastingVMCall
import cn.xm1221.MieLib.worker.CastingVMWorker
import net.minecraft.server.level.ServerLevel

object ImportWorker: CastingVMWorker(false,10) {
    override fun execute(
        iota: Iota,
        world: ServerLevel,
        cont: SpellContinuation,
        call: CastingVMCall,
        vm: CastingVM
    ): CastingVMCall {
        if (iota is PatternIota){
            val result = iota.executeWithImports(vm,world,cont)
            if (result != null) {
                call.setResult(result)
            }
        }
        return call
    }
}