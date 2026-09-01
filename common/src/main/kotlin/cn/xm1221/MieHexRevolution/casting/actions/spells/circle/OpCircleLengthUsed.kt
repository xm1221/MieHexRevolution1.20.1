package cn.xm1221.MieHexRevolution.casting.actions.spells.circle

import at.petrak.hexcasting.api.casting.asActionResult
import at.petrak.hexcasting.api.casting.castables.ConstMediaAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.eval.env.CircleCastEnv
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.mishaps.circle.MishapNoSpellCircle

class OpCircleLengthUsed: ConstMediaAction {
    override val argc: Int
        get() = 0

    override fun execute(
        args: List<Iota>,
        env: CastingEnvironment
    ): List<Iota> {
        if(env !is CircleCastEnv){
            throw MishapNoSpellCircle()
        }
        return  env.circleState().reachedPositions.toSet().size.asActionResult
    }
}