package cn.xm1221.MieHexRevolution.casting.actions.imports

import at.petrak.hexcasting.api.casting.castables.Action
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.eval.OperationResult
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage
import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.IotaType
import at.petrak.hexcasting.api.casting.iota.PatternIota
import at.petrak.hexcasting.api.casting.math.HexPattern
import at.petrak.hexcasting.api.casting.mishaps.MishapInvalidIota
import at.petrak.hexcasting.api.casting.mishaps.MishapNotEnoughArgs
import at.petrak.hexcasting.api.utils.putCompound
import at.petrak.hexcasting.common.lib.hex.HexEvalSounds
import cn.xm1221.MieHexRevolution.api.casting.iota.ImportsIota

/**
 * Writes one binding directly into this cast's user-data imports: accepts (pattern, value) and
 * merges `pattern -> value` into whatever imports the cast has defined so far (starting a fresh
 * set if none), without requiring an [ImportsIota] on the stack first. The binding stays active
 * for the remainder of the cast.
 */
class OpImportsBindDirect : Action {
    // `argc` is not part of the bare `Action` interface (only ConstMediaAction declares it),
    // so this is a plain property, matching e.g. OpEvalInList.
    val argc: Int
        get() = 2

    override fun operate(
        env: CastingEnvironment,
        image: CastingImage,
        continuation: SpellContinuation
    ): OperationResult {
        val stack = image.stack.toMutableList()
        if (argc > stack.size) throw MishapNotEnoughArgs(argc, stack.size)
        val args = stack.takeLast(argc)
        repeat(argc) { stack.removeLast() }

        val name = args[0]
        val value = args[1]
        if (name !is PatternIota) throw MishapInvalidIota.ofType(name, 1, "pattern")

        // The upstream VM hands each action its own copy of userData, but copy again to be explicit.
        val userdata = image.userData.copy()
        val existing = (IotaType.deserialize(userdata.getCompound("imports"), env.world) as? ImportsIota)?.imports
        val merged = HashMap<HexPattern, Iota>(existing ?: emptyMap())
        merged[name.pattern] = value
        userdata.putCompound("imports", IotaType.serialize(ImportsIota(merged)))

        return OperationResult(
            newImage = image.copy(userData = userdata, stack = stack),
            sideEffects = listOf(),
            newContinuation = continuation,
            sound = HexEvalSounds.NORMAL_EXECUTE,
        )
    }
}