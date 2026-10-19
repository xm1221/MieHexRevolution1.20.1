package cn.xm1221.MieHexRevolution.casting.actions.imports

import at.petrak.hexcasting.api.casting.castables.Action
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.eval.OperationResult
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage
import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation
import at.petrak.hexcasting.common.lib.hex.HexEvalSounds
import cn.xm1221.MieHexRevolution.api.casting.iota.ImportsIota

class OpImportGet: Action {
    override fun operate(
        env: CastingEnvironment,
        image: CastingImage,
        continuation: SpellContinuation
    ): OperationResult {
        val importsIota = image.getComponent(ImportsIota.COMPONENT) ?: ImportsIota(emptyMap())
        val stack = image.stack.appended(importsIota)
        return OperationResult(image.copy(stack = stack), listOf(), continuation, HexEvalSounds.NORMAL_EXECUTE.get())
    }
}
