package cn.xm1221.MieHexRevolution.pigment

import at.petrak.hexcasting.api.casting.eval.vm.CastingVM
import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation
import at.petrak.hexcasting.api.casting.iota.Iota
import cn.xm1221.MieLib.worker.CastingVMCall
import cn.xm1221.MieLib.worker.CastingVMWorker
import net.minecraft.Util
import net.minecraft.server.level.ServerLevel

/**
 * Tracks the active casting entity's UUID on the VM thread via MieLib's [cn.xm1221.MieLib.registry.CastingVMHook]
 * so [at.petrak.hexcasting.common.items.pigment.ItemUUIDPigment] resolves the real caster's colors
 * when read through native `ADIotaHolder` capabilities/lookups.
 */
object PigmentCasterContextWorker {

    object Before : CastingVMWorker(isAfter = false, priority = 5) {
        override fun execute(
            iota: Iota,
            world: ServerLevel,
            cont: SpellContinuation,
            call: CastingVMCall,
            vm: CastingVM,
        ): CastingVMCall {
            PigmentColorExtractor.pushCasterUuid(call, vm.env.castingEntity?.uuid ?: Util.NIL_UUID)
            return call
        }
    }

    object After : CastingVMWorker(isAfter = true, priority = 1000) {
        override fun execute(
            iota: Iota,
            world: ServerLevel,
            cont: SpellContinuation,
            call: CastingVMCall,
            vm: CastingVM,
        ): CastingVMCall {
            PigmentColorExtractor.popCasterUuid(call)
            return call
        }
    }
}
