package cn.xm1221.MieHexRevolution.casting.actions.spells.psy

import at.petrak.hexcasting.api.casting.asActionResult
import at.petrak.hexcasting.api.casting.castables.ConstMediaAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.getLivingEntityButNotArmorStand
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.mishaps.MishapEntityTooFarAway

class OpEntityLastHurtGet: ConstMediaAction {
    override val argc: Int
        get() = 1

    override fun execute(
        args: List<Iota>,
        env: CastingEnvironment
    ): List<Iota> {
        val target = args.getLivingEntityButNotArmorStand(0,argc)
        if(!env.isEntityInRange(target)){
            throw MishapEntityTooFarAway(target)
        }
        val res = target.lastHurtByMob
        return res.asActionResult
    }
}