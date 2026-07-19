package cn.xm1221.MieHexRevolution.casting.actions.spells

import at.petrak.hexcasting.api.casting.RenderedSpell
import at.petrak.hexcasting.api.casting.castables.SpellAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.getBlockPos
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.mishaps.MishapBadCaster
import at.petrak.hexcasting.api.casting.mishaps.MishapBadLocation
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import org.joml.Vector3d

class OpUseBlock : SpellAction{
    override val argc: Int
        get() = 1

    override fun execute(
        args: List<Iota>,
        env: CastingEnvironment
    ): SpellAction.Result {
        val vec = args.getBlockPos(0,argc)
        val VEC = Vec3(vec.x.toDouble(), vec.y.toDouble(), vec.z.toDouble())
        val blockstate = env.world.getBlockState(vec)
        val caster = env.castingEntity
        if(caster !is ServerPlayer) {
            throw MishapBadCaster()
        }
        if(!env.isVecInRange(VEC)){
            throw MishapBadLocation(VEC)
        }
        return SpellAction.Result(
            effect = object: RenderedSpell{
                override fun cast(env: CastingEnvironment) {
                    blockstate.block.use(blockstate,env.world,vec, caster,env.castingHand, BlockHitResult(VEC,
                        Direction.NORTH,vec,false)
                          )
                }
            },
            cost = 0,
            particles = listOf(),
        )
    }

}