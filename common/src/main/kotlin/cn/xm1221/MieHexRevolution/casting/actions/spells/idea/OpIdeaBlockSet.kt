package cn.xm1221.MieHexRevolution.casting.actions.spells.idea

import at.petrak.hexcasting.api.casting.RenderedSpell
import at.petrak.hexcasting.api.casting.ParticleSpray
import at.petrak.hexcasting.api.casting.castables.SpellAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.getBlockPos
import at.petrak.hexcasting.api.casting.iota.Iota
import cn.xm1221.MieHexRevolution.registry.Miehex_revolutionBlocks


class OpIdeaBlockSet: SpellAction {
    override val argc: Int
        get() = 1

    override fun execute(
        args: List<Iota>,
        env: CastingEnvironment
    ): SpellAction.Result {
        val pos = args.getBlockPos(0,argc)
        val blockstate = env.world.getBlockState(pos)
        return SpellAction.Result(
            effect = object : RenderedSpell{
                override fun cast(env: CastingEnvironment) {
                    if(blockstate.isAir){
                        val ideablock = Miehex_revolutionBlocks.IDEA_FULL
                        env.world.setBlockAndUpdate(pos,ideablock.get().defaultBlockState())
                    }
                }

            },
            cost = 5,
            emptyList<ParticleSpray>(), 0,
        )
    }
}