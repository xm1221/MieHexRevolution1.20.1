package cn.xm1221.MieHexRevolution.casting.actions.imports

import at.petrak.hexcasting.api.casting.castables.ConstMediaAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.iota.Iota
import cn.xm1221.MieHexRevolution.api.casting.iota.ImportsIota

class OpImportsCreate() : ConstMediaAction {
    override val argc: Int
        get() = 0

    override fun execute(
        args: List<Iota>,
        env: CastingEnvironment
    ): List<Iota> {
        return listOf(ImportsIota(HashMap()))
    }
}