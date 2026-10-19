package cn.xm1221.MieHexRevolution.casting.actions.imports

import at.petrak.hexcasting.api.casting.castables.Action
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.eval.OperationResult
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage
import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation
import at.petrak.hexcasting.common.lib.hex.HexEvalSounds
import cn.xm1221.MieHexRevolution.api.casting.iota.ImportsIota

/**
 * Toggles "raw mode": while active, import bindings are bypassed and patterns run their native
 * action. The flag lives in a transient [CastingImage] component (see [ImportsIota.RAW_COMPONENT]).
 */
class OpRawPattern: Action {
    override fun operate(
        env: CastingEnvironment,
        image: CastingImage,
        continuation: SpellContinuation
    ): OperationResult {
        val current = image.getComponent(ImportsIota.RAW_COMPONENT) == true
        val newImage = image.withComponent(ImportsIota.RAW_COMPONENT, !current).withUsedOp()
        return OperationResult(newImage, listOf(), continuation, HexEvalSounds.NORMAL_EXECUTE.get())
    }
}
