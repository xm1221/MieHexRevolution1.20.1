package cn.xm1221.MieHexRevolution.casting.actions.useful

import at.petrak.hexcasting.api.casting.castables.ConstMediaAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.iota.DoubleIota
import at.petrak.hexcasting.api.casting.iota.Iota

class OpMax: ConstMediaAction {
    override val argc: Int
        get() = 0

    override fun execute(
        args: List<Iota>,
        env: CastingEnvironment
    ): List<Iota> {
        return listOf(DoubleIota(Double.MAX_VALUE))
    }

}