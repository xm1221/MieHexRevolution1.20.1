package cn.xm1221.MieHexRevolution.casting.actions.imports

import at.petrak.hexcasting.api.casting.castables.ConstMediaAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.PatternIota
import at.petrak.hexcasting.api.casting.math.HexPattern
import at.petrak.hexcasting.api.casting.mishaps.MishapInvalidIota
import at.petrak.hexcasting.api.casting.mishaps.MishapOthersName
import cn.xm1221.MieHexRevolution.api.casting.iota.ImportsIota
import cn.xm1221.MieHexRevolution.util.TrueNameProtection
import net.minecraft.server.level.ServerPlayer

/**
 * Stack-based "add one binding": accepts (imports, pattern, value) and returns a NEW imports set
 * with `pattern -> value` added, replacing any previous binding for that same pattern.
 *
 * Compose with [OpImportsCreate] to build an import set on the stack, then cast it (Hermes)
 * to load it into the current cast.
 */
class OpImportsBind : ConstMediaAction {
    override val argc: Int
        get() = 3

    override fun execute(args: List<Iota>, env: CastingEnvironment): List<Iota> {
       TrueNameProtection(env, args)
        val set = args[0]
        val name = args[1]
        val value = args[2]
        if (set !is ImportsIota) throw MishapInvalidIota.ofType(set, 2, "imports_iota")
        if (name !is PatternIota) throw MishapInvalidIota.ofType(name, 1, "pattern")
        val merged = HashMap<HexPattern, Iota>(set.imports)

        merged[name.pattern] = value
        return listOf(ImportsIota(merged))
    }
}