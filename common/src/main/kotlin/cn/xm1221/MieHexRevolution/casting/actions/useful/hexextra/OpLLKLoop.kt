package cn.xm1221.MieHexRevolution.casting.actions.useful.hexextra

import at.petrak.hexcasting.api.casting.castables.Action
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.eval.OperationResult
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage
import at.petrak.hexcasting.api.casting.eval.vm.FrameEvaluate
import at.petrak.hexcasting.api.casting.eval.vm.FrameFinishEval
import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation
import at.petrak.hexcasting.api.casting.getPositiveInt
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.ListIota
import at.petrak.hexcasting.api.casting.mishaps.MishapNotEnoughArgs
import at.petrak.hexcasting.api.utils.TreeList
import at.petrak.hexcasting.common.lib.hex.HexEvalSounds

class OpLLKLoop: Action {
    override fun operate(env: CastingEnvironment, image: CastingImage, continuation: SpellContinuation): OperationResult {
        val stack = image.stack.toMutableList()
        val iota = stack.removeLastOrNull() ?: throw MishapNotEnoughArgs(1, 0)
        val repeat = stack.getPositiveInt(stack.size - 1)
        stack.removeLast()
        return exec(env, image, continuation, stack, iota, repeat)
    }

    fun exec(env: CastingEnvironment, image: CastingImage, continuation: SpellContinuation, newStack: MutableList<Iota>, iota: Iota, repeat: Int): OperationResult {
        // Hex 0.12 dropped `evaluatable`; a list is evaluated item by item, anything else as a
        // single-instruction program.
        val instrs: TreeList<Iota> = if (iota is ListIota) iota.list else TreeList.from(listOf(iota))
        // also, never make a break boundary when evaluating just one pattern
        var newCont =
            if (iota !is ListIota || (continuation is SpellContinuation.NotDone && continuation.frame is FrameFinishEval)) {
                continuation
            } else {
                continuation.pushFrame(FrameFinishEval) // install a break-boundary after eval
            }

        val frame = FrameEvaluate(instrs, true)
        repeat(repeat) { newCont = newCont.pushFrame(frame) }
        val image2 = image.withUsedOp().copy(stack = TreeList.from(newStack))
        return OperationResult(image2, listOf(), newCont, HexEvalSounds.HERMES.get())
    }
}
