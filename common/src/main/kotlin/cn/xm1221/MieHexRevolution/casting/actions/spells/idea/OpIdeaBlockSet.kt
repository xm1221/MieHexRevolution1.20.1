package cn.xm1221.MieHexRevolution.casting.actions.spells.idea

import at.petrak.hexcasting.api.casting.RenderedSpell
import at.petrak.hexcasting.api.casting.castables.SpellAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.eval.CastingEnvironmentComponent
import at.petrak.hexcasting.api.casting.getBlockPos
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.mishaps.MishapBadLocation
import cn.xm1221.MieHexRevolution.registry.Miehex_revolutionBlocks
import net.minecraft.world.level.block.AirBlock
import net.minecraft.world.phys.Vec3


class OpIdeaBlockSet: SpellAction {
    override val argc: Int
        get() = 1

    override fun execute(
        args: List<Iota>,
        env: CastingEnvironment
    ): SpellAction.Result {
        val pos = args.getBlockPos(0,argc)
        if(!env.isVecInRange(Vec3(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble()))){
            throw MishapBadLocation(Vec3(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble()))
        }
        val blockstate = env.world.getBlockState(pos)
        val block = blockstate.block
        return SpellAction.Result(
            effect = object : RenderedSpell{
                override fun cast(env: CastingEnvironment) {
                    if(block is AirBlock){
                        val ideablock = Miehex_revolutionBlocks.IDEA_FULL
                        env.world.setBlockAndUpdate(pos,ideablock.get().defaultBlockState())
                    }
                }

            },
            cost = 5,
            particles = listOf(),
        )
    }
}