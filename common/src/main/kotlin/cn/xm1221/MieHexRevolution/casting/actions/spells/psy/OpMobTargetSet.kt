package cn.xm1221.MieHexRevolution.casting.actions.spells.psy

import at.petrak.hexcasting.api.casting.ParticleSpray
import at.petrak.hexcasting.api.casting.RenderedSpell
import at.petrak.hexcasting.api.casting.castables.SpellAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.getLivingEntityButNotArmorStand
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.mishaps.MishapEntityTooFarAway
import at.petrak.hexcasting.api.casting.mishaps.MishapImmuneEntity
import at.petrak.hexcasting.api.casting.mishaps.MishapOthersName
import at.petrak.hexcasting.api.misc.MediaConstants
import at.petrak.hexcasting.api.mod.HexTags
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.animal.IronGolem
import net.minecraft.world.entity.npc.Villager
import net.minecraft.world.phys.Vec3


class OpMobTargetSet: SpellAction {
    override val argc: Int
        get() = 2

    override fun execute(
        args: List<Iota>,
        env: CastingEnvironment
    ): SpellAction.Result {
        val target = args.getLivingEntityButNotArmorStand(0,argc)
        val target1 =args.getLivingEntityButNotArmorStand(1,argc)
        if(!env.isEntityInRange(target)){
            throw MishapEntityTooFarAway(target)
        }
        if(target is ServerPlayer) {
            throw MishapOthersName(target)
        }
        var cost = MediaConstants.CRYSTAL_UNIT
        if(target == target1){
            cost = MediaConstants.CRYSTAL_UNIT*10
        }
        if(target is IronGolem && target1 is Villager) {
            cost = MediaConstants.CRYSTAL_UNIT*50
        }
        if(target.type.`is`(HexTags.Entities.CANNOT_TELEPORT)){
            throw MishapImmuneEntity(target)
        }
        return SpellAction.Result(
            object : RenderedSpell {
                override fun cast(env: CastingEnvironment) {
                    if(target is Mob){
                        target.target = target1
                    }
                }
            },
            cost = cost,
            particles = listOf(ParticleSpray(target.position(), Vec3(0.0, 0.0, 0.0), 2.0, 2.0, 20)),
        )
    }
}