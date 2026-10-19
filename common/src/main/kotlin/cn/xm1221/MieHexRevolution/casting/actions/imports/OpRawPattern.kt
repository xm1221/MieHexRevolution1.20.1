package cn.xm1221.MieHexRevolution.casting.actions.imports

import at.petrak.hexcasting.api.casting.castables.Action
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.eval.OperationResult
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage
import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation
import at.petrak.hexcasting.common.lib.hex.HexEvalSounds

class OpRawPattern: Action {
    override fun operate(
        env: CastingEnvironment,
        image: CastingImage,
        continuation: SpellContinuation
    ): OperationResult {
        val userdata = image.userData
        if(userdata.contains("run_raw") && userdata.getBoolean("run_raw")) {
            userdata.putBoolean("run_raw", false)
        }
        else{
            userdata.putBoolean("run_raw",true)
        }
        return OperationResult(image.copy(userData = userdata).withUsedOp(),listOf(),continuation, HexEvalSounds.NORMAL_EXECUTE)
    }
}