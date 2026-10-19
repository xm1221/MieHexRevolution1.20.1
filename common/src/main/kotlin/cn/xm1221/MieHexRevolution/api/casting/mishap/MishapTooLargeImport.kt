package cn.xm1221.MieHexRevolution.api.casting.mishap

import at.petrak.hexcasting.api.HexAPI
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.mishaps.Mishap
import at.petrak.hexcasting.api.pigment.FrozenPigment
import net.minecraft.network.chat.Component

class MishapTooLargeImport: Mishap() {
    override fun accentColor(
        env: CastingEnvironment,
        errorCtx: Context
    ): FrozenPigment {
        return env.pigment
    }

    override fun execute(
        env: CastingEnvironment,
        errorCtx: Context,
        stack: MutableList<Iota>
    ) {
        env.mishapEnvironment.dropHeldItems()
    }

    override fun errorMessage(
        env: CastingEnvironment,
        errorCtx: Context
    ): Component? {
        return Component.translatable("hexcasting.mishap.too_large_import")
    }
}