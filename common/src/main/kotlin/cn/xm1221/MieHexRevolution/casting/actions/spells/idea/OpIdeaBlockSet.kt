package cn.xm1221.MieHexRevolution.casting.actions.spells.idea

import at.petrak.hexcasting.api.casting.RenderedSpell
import at.petrak.hexcasting.api.casting.castables.SpellAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.getBlockPos
import at.petrak.hexcasting.api.casting.iota.Iota
import cn.xm1221.MieHexRevolution.registry.Miehex_revolutionBlocks

class OpIdeaBlockSet : SpellAction {
    override val argc: Int
        get() = 1

    override fun execute(
        args: List<Iota>,
        env: CastingEnvironment
    ): SpellAction.Result {
        val pos = args.getBlockPos(0, argc)
        // assertPosInRangeForEditing = 施法范围 + 编辑权限（领地、冒险模式、出生点保护），
        // 比手写 isVecInRange 更完整，缺了它咒术就能无视保护放方块。
        env.assertPosInRangeForEditing(pos)
        return SpellAction.Result(
            effect = object : RenderedSpell {
                override fun cast(env: CastingEnvironment) {
                    val level = env.world
                    if (!level.getBlockState(pos).canBeReplaced()) return
                    level.setBlockAndUpdate(pos, Miehex_revolutionBlocks.IDEA_FULL.get().defaultBlockState())
                }
            },
            cost = 5,
            particles = listOf(),
        )
    }
}
