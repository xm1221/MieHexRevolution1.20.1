package cn.xm1221.MieHexRevolution.casting.actions.spells

import at.petrak.hexcasting.api.casting.RenderedSpell
import at.petrak.hexcasting.api.casting.castables.SpellAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.getBlockPos
import at.petrak.hexcasting.api.casting.getItemEntity
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.mishaps.MishapBadCaster
import at.petrak.hexcasting.api.casting.mishaps.MishapBadLocation
import at.petrak.hexcasting.ktxt.UseOnContext
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3

class OpUseWithItemEntity() : SpellAction {
    override val argc: Int
        get() = 2

    override fun execute(
        args: List<Iota>,
        env: CastingEnvironment
    ): SpellAction.Result {
        val vec = args.getBlockPos(0,argc)
        val itemstack =args.getItemEntity(1,argc).item
        val VEC = Vec3(vec.x.toDouble(), vec.y.toDouble(), vec.z.toDouble())
        val caster = env.castingEntity
        //val blockstate = env.world.getBlockState(vec)
        if(caster !is ServerPlayer) {
            throw MishapBadCaster()
        }
        if(!env.isVecInRange(VEC)){
            throw MishapBadLocation(VEC)
        }
        return SpellAction.Result(
            effect = object: RenderedSpell {
                override fun cast(env: CastingEnvironment) {
                        itemstack.useOn(
                            UseOnContext(
                                env.world, caster, env.otherHand, itemstack,
                                BlockHitResult(VEC, Direction.NORTH, vec, false)
                            )
                        )
                }
            },
            cost = 0,
            particles = listOf(),
        )
    }
}