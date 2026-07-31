package cn.xm1221.MieHexRevolution.casting.actions.spells

import at.petrak.hexcasting.api.casting.ParticleSpray
import at.petrak.hexcasting.api.casting.RenderedSpell
import at.petrak.hexcasting.api.casting.castables.SpellAction
import at.petrak.hexcasting.api.casting.castables.SpellAction.Result
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.getLivingEntityButNotArmorStand
import at.petrak.hexcasting.api.casting.getVec3
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.mishaps.MishapBadEntity
import at.petrak.hexcasting.api.casting.mishaps.MishapEntityTooFarAway
import at.petrak.hexcasting.api.casting.mishaps.MishapOthersName
import net.minecraft.server.level.ServerPlayer

class OpEntityMove: SpellAction {
    override val argc: Int
        get() = 2

    override fun execute(
        args: List<Iota>,
        env: CastingEnvironment
    ): Result {

        val target = args.getLivingEntityButNotArmorStand(0,argc)
        val vec = args.getVec3(1,argc)
        if(!env.isEntityInRange(target)){
            throw MishapEntityTooFarAway(target)
        }
        if(target is ServerPlayer) {
            throw MishapOthersName(target)
        }
        return Result(
            object : RenderedSpell {
                override fun cast(env: CastingEnvironment) {
                   target.travel(vec)
                }
            },
            cost = 5,
            particles = listOf(ParticleSpray(target.position(), vec, 2.0, 2.0, 20)),
        )
    }
}