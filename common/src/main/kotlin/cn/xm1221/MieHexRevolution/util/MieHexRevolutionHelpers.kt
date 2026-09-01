package cn.xm1221.MieHexRevolution.util

import at.petrak.hexcasting.api.casting.eval.CastResult
import at.petrak.hexcasting.api.casting.eval.ResolvedPatternType
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage
import at.petrak.hexcasting.api.casting.eval.vm.CastingVM
import at.petrak.hexcasting.api.casting.eval.vm.ContinuationFrame
import at.petrak.hexcasting.api.casting.eval.vm.FrameEvaluate
import at.petrak.hexcasting.api.casting.eval.vm.FrameFinishEval
import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation
import at.petrak.hexcasting.api.casting.iota.IotaType
import at.petrak.hexcasting.api.casting.iota.ListIota
import at.petrak.hexcasting.api.casting.iota.PatternIota
import at.petrak.hexcasting.common.lib.hex.HexEvalSounds
import cn.xm1221.MieHexRevolution.api.casting.iota.ImportsIota
import net.minecraft.server.level.ServerLevel

/**
 * If the current cast has an [ImportsIota] bound in its user data and [this] pattern is one of its
 * keys, evaluate to the bound value instead of the pattern's usual action. Returns `null` to signal
 * that the normal pattern execution should proceed.
 */
fun PatternIota.executeWithImports(vm: CastingVM?, world: ServerLevel?, continuation: SpellContinuation?): CastResult? {
    if (vm == null || world == null || continuation == null) return null
    val img = vm.image
    val userdata = img.userData
    // Fast path: this cast never defined any imports. `CompoundTag.getCompound` returns an empty tag
    // (not null) for a missing key, so we must check containment before deserializing.
    if (!userdata.contains("imports")) return null
    val deserialized = IotaType.deserialize(userdata.getCompound("imports"), world)
    if (deserialized !is ImportsIota) return null
    val funs = deserialized.imports[this.pattern] ?: return null

    val frameEvaluate: ContinuationFrame
    val newimg: CastingImage
    when {
        funs is ListIota -> {
            // evaluate the imported function body as a sub-spell
            frameEvaluate = FrameEvaluate(funs.list, true)
            newimg = img
        }
        !funs.executable() -> {
            // plain datum: push it onto the stack (empty sub-eval keeps the resolution shape)
            frameEvaluate = FrameEvaluate(ListIota(listOf()).list, true)
            newimg = img.copy(stack = img.stack.plus(funs))
        }
        else -> {
            // single executable iota (e.g. another pattern): evaluate it
            frameEvaluate = FrameEvaluate(ListIota(listOf(funs)).list, true)
            newimg = img
        }
    }
    var newcont = continuation.pushFrame(FrameFinishEval)
    newcont = newcont.pushFrame(frameEvaluate)
    return CastResult(
        cast = this,
        continuation = newcont,
        // Consume one op per fired import, exactly like a normal operation does. Hex Casting
        // itself stops any cast once `opsConsumed` exceeds the env's op limit (MishapEvalTooMuch),
        // so a bound function that (transitively) calls its own key fails cleanly instead of
        // growing the continuation forever and exhausting the heap.
        newData = newimg.withUsedOp(),
        sideEffects = listOf(),
        resolutionType = ResolvedPatternType.EVALUATED,
        sound = HexEvalSounds.NORMAL_EXECUTE
    )
}