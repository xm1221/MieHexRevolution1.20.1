package cn.xm1221.MieHexRevolution.casting.actions.imports

import at.petrak.hexcasting.api.casting.castables.ConstMediaAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.math.HexPattern
import at.petrak.hexcasting.api.casting.mishaps.MishapInvalidIota
import cn.xm1221.MieHexRevolution.api.casting.iota.ImportsIota

/**
 * Combines two import sets: accepts (base, overlay) and returns a new set with `overlay`'s
 * bindings on top (overlay wins for the same pattern). Use to merge e.g. an Akashic library
 * import with hand-made bindings.
 */
class OpImportsMerge : ConstMediaAction {
    override val argc: Int
        get() = 2

    override fun execute(args: List<Iota>, env: CastingEnvironment): List<Iota> {
        val base = args[0]
        val overlay = args[1]
        if (base !is ImportsIota) throw MishapInvalidIota.ofType(base, 1, "imports_iota")
        if (overlay !is ImportsIota) throw MishapInvalidIota.ofType(overlay, 0, "imports_iota")
        val merged = HashMap<HexPattern, Iota>(base.imports)
        merged.putAll(overlay.imports)
        return listOf(ImportsIota(merged))
    }
}