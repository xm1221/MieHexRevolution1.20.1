package cn.xm1221.MieHexRevolution.util

import at.petrak.hexcasting.api.casting.asActionResult
import at.petrak.hexcasting.api.casting.eval.CastResult
import at.petrak.hexcasting.api.casting.eval.ResolvedPatternType
import at.petrak.hexcasting.api.casting.eval.vm.CastingVM
import at.petrak.hexcasting.api.casting.eval.vm.ContinuationFrame
import at.petrak.hexcasting.api.casting.eval.vm.FrameEvaluate
import at.petrak.hexcasting.api.casting.eval.vm.FrameFinishEval
import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.IotaType
import at.petrak.hexcasting.api.casting.iota.ListIota
import at.petrak.hexcasting.api.casting.iota.PatternIota
import at.petrak.hexcasting.common.lib.hex.HexEvalSounds
import cn.xm1221.MieHexRevolution.api.casting.iota.ImportsIota
import cn.xm1221.MieHexRevolution.casting.frame.PushFrame
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel

fun PatternIota.executeWithImports(vm: CastingVM?, world: ServerLevel?, continuation: SpellContinuation?): CastResult?{
    if(vm == null || world == null|| continuation == null) return null
    val img =vm.image
    var newimg = img
    val userdata = img.userData
    val imports = (IotaType.deserialize(userdata.getCompound("imports"),world) as ImportsIota).imports
    if (imports[this]!= null){
        val funs = imports[this]!!
        var frameEvaluate : ContinuationFrame
        if(funs is ListIota){
            frameEvaluate = FrameEvaluate(funs.list,true)
        }
        else if(!funs.executable()) {
           // frameEvaluate = FrameEvaluate(ListIota(listOf(funs)).list,true)
            frameEvaluate = FrameEvaluate(ListIota(listOf()).list,true)
            newimg = img.copy(stack = img.stack.plus(funs))
        }
        else  {
            frameEvaluate = FrameEvaluate(ListIota(listOf(funs)).list,true)
        }
        var newcont = continuation.pushFrame(FrameFinishEval)
        newcont = newcont.pushFrame(frameEvaluate)
        return CastResult(
            cast = this,
            continuation = newcont,
            newData = newimg,
            sideEffects = listOf(),
            resolutionType = ResolvedPatternType.EVALUATED,
            sound = HexEvalSounds.NORMAL_EXECUTE
        )
    }
    else return null
}

