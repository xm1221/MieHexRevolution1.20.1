package cn.xm1221.MieHexRevolution.util

import at.petrak.hexcasting.api.casting.eval.CastResult
import at.petrak.hexcasting.api.casting.eval.ResolvedPatternType
import at.petrak.hexcasting.api.casting.eval.sideeffects.OperatorSideEffect
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage
import at.petrak.hexcasting.api.casting.eval.vm.CastingVM
import at.petrak.hexcasting.api.casting.eval.vm.ContinuationFrame
import at.petrak.hexcasting.api.casting.eval.vm.FrameEvaluate
import at.petrak.hexcasting.api.casting.eval.vm.FrameFinishEval
import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation
import at.petrak.hexcasting.api.casting.iota.IotaType
import at.petrak.hexcasting.api.casting.iota.ListIota
import at.petrak.hexcasting.api.casting.iota.PatternIota
import at.petrak.hexcasting.api.casting.mishaps.Mishap
import at.petrak.hexcasting.api.casting.mishaps.MishapEvalTooMuch
import at.petrak.hexcasting.common.lib.hex.HexEvalSounds
import cn.xm1221.MieHexRevolution.api.casting.iota.ImportsIota
import net.minecraft.server.level.ServerLevel

/**
 * If the current cast has an [ImportsIota] bound in its user data and [this] pattern is one of its
 * keys, evaluate to the bound value instead of the pattern's usual action. Returns `null` to signal
 * that the normal pattern execution should proceed.
 *
 * Firing an import consumes one op, and we apply the same op limit a normal cast has
 * ([at.petrak.hexcasting.api.casting.eval.CastingEnvironment.maxOpCount], MishapEvalTooMuch).
 * In 0.11.3 that check only runs inside `PatternIota.lookupAndOperate` for the normal action path,
 * which this import path never reaches -- so without the check here, a bound function that
 * (transitively) calls its own key would grow the continuation forever and exhaust the heap.
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
    // Consume one op per fired import (same accounting as a normal ConstMediaAction), then enforce
    // the env op limit ourselves -- see the KDoc above for why hexmod's own check cannot reach here.
    val newimg2 = newimg.copy(opsConsumed = newimg.opsConsumed + 1)
    if (newimg2.opsConsumed > vm.env.maxOpCount()) {
        return CastResult(
            cast = this,
            continuation = continuation,
            newData = null,
            sideEffects = listOf(
                OperatorSideEffect.DoMishap(
                    MishapEvalTooMuch(),
                    Mishap.Context(this.pattern, null)
                )
            ),
            resolutionType = ResolvedPatternType.ERRORED,
            sound = HexEvalSounds.MISHAP,
        )
    }
    return CastResult(
        cast = this,
        continuation = newcont,
        newData = newimg2,
        sideEffects = listOf(),
        resolutionType = ResolvedPatternType.EVALUATED,
        sound = HexEvalSounds.NORMAL_EXECUTE
    )
}