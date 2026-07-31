package cn.xm1221.MieHexRevolution.casting.actions.spells.psy

import at.petrak.hexcasting.api.casting.asActionResult
import at.petrak.hexcasting.api.casting.castables.ConstMediaAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.getLivingEntityButNotArmorStand
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.mishaps.MishapEntityTooFarAway
import net.minecraft.world.entity.Mob

class OpMobTargetGet: ConstMediaAction {
    override val argc: Int
        get() = 1

    override fun execute(
        args: List<Iota>,
        env: CastingEnvironment
    ): List<Iota> {
        val entity = args.getLivingEntityButNotArmorStand(0,argc)
        if(!env.isEntityInRange(entity)){
            throw MishapEntityTooFarAway(entity)
        }
        if(entity is Mob) {
            return entity.target.asActionResult
        }
        return null.asActionResult
    }
}