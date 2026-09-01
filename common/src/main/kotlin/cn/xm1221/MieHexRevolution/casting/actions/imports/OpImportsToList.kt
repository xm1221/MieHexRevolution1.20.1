package cn.xm1221.MieHexRevolution.casting.actions.imports

import at.petrak.hexcasting.api.casting.castables.ConstMediaAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.ListIota
import at.petrak.hexcasting.api.casting.iota.PatternIota
import at.petrak.hexcasting.api.casting.mishaps.MishapInvalidIota
import cn.xm1221.MieHexRevolution.api.casting.iota.ImportsIota

/**
 * Exposes an import set as a plain list of `[pattern, value]` pairs (each pair a
 * [ListIota]), so the bindings can be inspected, iterated with ordinary list operations,
 * compared, and stored. Inverse of [OpImportsFromList].
 */
class OpImportsToList : ConstMediaAction {
    override val argc: Int
        get() = 1

    override fun execute(args: List<Iota>, env: CastingEnvironment): List<Iota> {
        val set = args[0]
        if (set !is ImportsIota) throw MishapInvalidIota.ofType(set, 0, "imports_iota")
        val pairs = set.imports.map { (pattern, iota) -> ListIota(listOf(PatternIota(pattern), iota)) }
        return listOf(ListIota(pairs))
    }
}