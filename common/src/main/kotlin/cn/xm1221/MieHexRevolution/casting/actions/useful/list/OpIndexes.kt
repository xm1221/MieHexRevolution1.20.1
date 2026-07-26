package cn.xm1221.MieHexRevolution.casting.actions.useful.list

import at.petrak.hexcasting.api.casting.asActionResult
import at.petrak.hexcasting.api.casting.castables.ConstMediaAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.getList
import at.petrak.hexcasting.api.casting.getPositiveLong
import at.petrak.hexcasting.api.casting.iota.DoubleIota
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.ListIota
import jdk.incubator.vector.VectorShuffle.iota

class OpIndexes: ConstMediaAction {
    override val argc: Int
        get() = 2

    override fun execute(
        args: List<Iota>,
        env: CastingEnvironment
    ): List<Iota> {
        val list = args.getList(0, argc)
        val iota = args.get(1)
        var res =listOf<Iota>()
        for (e in list) {
            if(Iota.tolerates(e,iota)) {
                res= res.plus(DoubleIota(list.indexOf(e).toDouble()))
            }
        }
        return res.asActionResult
    }
}