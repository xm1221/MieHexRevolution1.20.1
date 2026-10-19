package cn.xm1221.MieHexRevolution.casting.actions.imports

import at.petrak.hexcasting.api.casting.castables.ConstMediaAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.getBlockPos
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.mishaps.MishapNoAkashicRecord
import at.petrak.hexcasting.common.blocks.akashic.BlockAkashicRecord
import cn.xm1221.MieHexRevolution.api.casting.iota.ImportsIota
import cn.xm1221.MieHexRevolution.util.getMaps

/**
 * Loads the whole Akashic Record at the given position into an [ImportsIota] set
 * (pattern -> stored datum), ready to be cast so the current cast imports from the library.
 */
class OpImportsFromAkashic: ConstMediaAction {
    override val argc: Int
        get() = 1

    override fun execute(
        args: List<Iota>,
        env: CastingEnvironment
    ): List<Iota> {
        val pos = args.getBlockPos(0,argc)
        val record = env.world.getBlockState(pos).block
        if (record !is BlockAkashicRecord) {
            throw MishapNoAkashicRecord(pos)
        }
        return listOf(ImportsIota(record.getMaps(env.world, pos)))
    }
}