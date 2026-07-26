package cn.xm1221.MieHexRevolution.casting.actions.useful.list

import at.petrak.hexcasting.api.casting.castables.Action
import at.petrak.hexcasting.api.casting.castables.ConstMediaAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.eval.OperationResult
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage
import at.petrak.hexcasting.api.casting.eval.vm.ContinuationFrame
import at.petrak.hexcasting.api.casting.eval.vm.FrameEvaluate
import at.petrak.hexcasting.api.casting.eval.vm.FrameFinishEval
import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation
import at.petrak.hexcasting.api.casting.getList
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.mishaps.MishapNotEnoughArgs
import at.petrak.hexcasting.common.lib.hex.HexEvalSounds
import cn.xm1221.MieHexRevolution.casting.frame.PushFrame

class OpEvalInList: Action{

    val argc: Int
        get() = 2


    override fun operate(
        env: CastingEnvironment,
        image: CastingImage,
        continuation: SpellContinuation
    ): OperationResult {
        val stack = image.stack.toMutableList()
        if (this.argc > stack.size)
            throw MishapNotEnoughArgs(this.argc, stack.size)
        val args = stack.takeLast(this.argc)
        repeat(this.argc) { stack.removeLast() }
        val list = args.getList(0, argc)
        val code = args.getList(1, argc)
        val cstack  = list.toList()
        val frameEvaluate = FrameEvaluate(code,true)
        val ostack=   stack
        val pushFrame = PushFrame(ostack)
        val newcont=   continuation.pushFrame(pushFrame).pushFrame(FrameFinishEval).pushFrame(frameEvaluate)
        return OperationResult(
            image.copy(stack = cstack),
            sideEffects = listOf(),
            newContinuation = newcont,
            sound = HexEvalSounds.HERMES,
        )
    }
}