package cn.xm1221.MieHexRevolution.api.casting.mishap

import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.mishaps.Mishap
import at.petrak.hexcasting.api.pigment.FrozenPigment
import at.petrak.hexcasting.common.items.pigment.ItemPridePigment
import at.petrak.hexcasting.common.lib.HexItems
import cn.xm1221.MieHexRevolution.pigment.PigmentData
import net.minecraft.Util
import net.minecraft.network.chat.Component
import net.minecraft.world.item.DyeColor
import net.minecraft.world.item.ItemStack

/**
 * Thrown when writing more than [PigmentData.MAX_COLORS] color vectors into a Raw Pigment.
 */
class MishapTooManyPigmentColors(
    val count: Int,
    val maxColors: Int = PigmentData.MAX_COLORS,
) : Mishap() {

    override fun accentColor(env: CastingEnvironment, errorCtx: Context): FrozenPigment =
        HexItems.PRIDE_PIGMENTS[ItemPridePigment.Type.GAY]
            ?.let { FrozenPigment(ItemStack(it), Util.NIL_UUID) }
            ?: dyeColor(DyeColor.MAGENTA)

    override fun execute(env: CastingEnvironment, errorCtx: Context, stack: MutableList<Iota>) {
        env.mishapEnvironment.blind(100)
    }

    override fun errorMessage(env: CastingEnvironment, errorCtx: Context): Component =
        Component.translatable("hexcasting.mishap.miehex_revolution.too_many_pigment_colors", count, maxColors)
}
