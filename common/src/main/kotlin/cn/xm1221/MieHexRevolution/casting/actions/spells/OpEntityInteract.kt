package cn.xm1221.MieHexRevolution.casting.actions.spells

import at.petrak.hexcasting.api.casting.ParticleSpray
import at.petrak.hexcasting.api.casting.RenderedSpell
import at.petrak.hexcasting.api.casting.castables.SpellAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.getBool
import at.petrak.hexcasting.api.casting.getEntity
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.mishaps.MishapBadCaster
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import kotlin.math.abs
import kotlin.math.pow

class OpEntityInteract: SpellAction {
    override val argc: Int
        get() = 2

    override fun execute(
        args: List<Iota>,
        env: CastingEnvironment
    ): SpellAction.Result {
        val target = args.getEntity(0,argc)
        val ops = args.getBool(1,argc)
        val caster =env.castingEntity
        if(caster !is Player ) {
            throw MishapBadCaster()
        }
        val cost :Long = 2.0.pow(abs(target.x - caster.x).toLong()+ abs(target.z - caster.z)).toLong()
        return SpellAction.Result(
            effect = object : RenderedSpell{
                override fun cast(env: CastingEnvironment) {
                    if(ops){
                        caster.attack(target)
                    }
                    else{
                        target.interact(caster,env.otherHand)
                        if(target is LivingEntity) {
                            caster.getItemInHand(env.otherHand).interactLivingEntity(caster,target,env.otherHand)
                        }
                    }
                }
            },
            cost = cost,
            particles = listOf(ParticleSpray(target.position(),caster.lookAngle,2.0,2.0,20)),
        )

    }
}