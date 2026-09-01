package cn.xm1221.MieHexRevolution.casting.actions.imports

import at.petrak.hexcasting.api.casting.castables.ConstMediaAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.ListIota
import at.petrak.hexcasting.api.casting.iota.PatternIota
import at.petrak.hexcasting.api.casting.math.HexPattern
import at.petrak.hexcasting.api.casting.mishaps.MishapInvalidIota
import cn.xm1221.MieHexRevolution.api.casting.iota.ImportsIota

/**
 * Builds an import set from a list of `[pattern, value]` pairs, the inverse of
 * [OpImportsToList]. Every element must be a two-element list whose first element is a
 * pattern; duplicate patterns keep the last value.
 */
class OpImportsFromList : ConstMediaAction {
    override val argc: Int
        get() = 1

    override fun execute(args: List<Iota>, env: CastingEnvironment): List<Iota> {
        val list = args[0]
        if (list !is ListIota) throw MishapInvalidIota.ofType(list, 0, "list")
        val map = HashMap<HexPattern, Iota>()
        for (element in list.list) {
            if (element !is ListIota) throw MishapInvalidIota.of(element, 0, "imports_pair")
            if (element.list.size() != 2) throw MishapInvalidIota.of(element, 0, "imports_pair")
            val pair = element.list.take(2).toList()
            val key = pair[0]
            if (key !is PatternIota) throw MishapInvalidIota.ofType(key, 0, "pattern")
            map[key.pattern] = pair[1]
        }
        return listOf(ImportsIota(map))
    }
}