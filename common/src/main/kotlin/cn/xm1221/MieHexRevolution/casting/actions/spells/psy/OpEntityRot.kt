package cn.xm1221.MieHexRevolution.casting.actions.spells.psy

import at.petrak.hexcasting.api.casting.RenderedSpell
import at.petrak.hexcasting.api.casting.castables.SpellAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.getEntity
import at.petrak.hexcasting.api.casting.getLivingEntityButNotArmorStand
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.mishaps.MishapBadCaster
import net.minecraft.world.entity.LivingEntity

class OpEntityRot: SpellAction {
    override val argc: Int
        get() = 1

    override fun execute(
        args: List<Iota>,
        env: CastingEnvironment
    ): SpellAction.Result {
        val target = args.getEntity(0,argc)
        val caster = env.castingEntity ?: throw MishapBadCaster()
        return SpellAction.Result(
            effect = object : RenderedSpell{
                override fun cast(env: CastingEnvironment) {
                    target.yHeadRot = caster.yHeadRot
                    if(target is LivingEntity) {
                        target.yHeadRotO = caster.yHeadRotO
                    }
                    target.xRotO = caster.xRotO
                    target.yRotO = caster.yRotO
                    target.xRot = caster.xRot
                    target.yRot = caster.yRot
                    target.setYBodyRot(caster.yBodyRot)
                }

            },
            cost = 0,
            particles = listOf()
        )
    }
}