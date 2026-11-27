package cn.xm1221.MieHexRevolution.item

import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.IotaType
import at.petrak.hexcasting.api.casting.iota.ListIota
import at.petrak.hexcasting.api.casting.iota.Vec3Iota
import at.petrak.hexcasting.api.item.IotaHolderItem
import at.petrak.hexcasting.api.item.PigmentItem
import at.petrak.hexcasting.api.pigment.ColorProvider
import cn.xm1221.MieHexRevolution.pigment.PigmentData
import cn.xm1221.MieHexRevolution.pigment.PigmentNaming
import cn.xm1221.MieHexRevolution.pigment.PigmentProviderCache
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import java.util.UUID

class ItemCustomPigment(properties: Properties) : Item(properties), PigmentItem, IotaHolderItem {

    override fun getName(stack: ItemStack): Component {
        val data = PigmentData.readOrNull(stack) ?: return super.getName(stack)
        return PigmentNaming.buildName(data)
    }

    override fun provideColor(stack: ItemStack, owner: UUID): ColorProvider =
        PigmentProviderCache.getOrBuild(PigmentData.read(stack))

    override fun readIotaTag(stack: ItemStack): CompoundTag? {
        val data = PigmentData.readOrNull(stack) ?: return null
        return IotaType.serialize(PigmentData.colorsToIota(data.colors))
    }

    override fun emptyIota(stack: ItemStack): Iota =
        Vec3Iota(PigmentData.argbToVec(PigmentData.DEFAULT_COLOR))

    override fun writeable(stack: ItemStack): Boolean = !PigmentData.hasPigmentData(stack)

    override fun canWrite(stack: ItemStack, iota: Iota?): Boolean =
        iota != null && writeable(stack) && extractInputColors(iota) != null

    override fun writeDatum(stack: ItemStack, iota: Iota?) {
        if (iota == null || !writeable(stack)) return
        val colors = extractInputColors(iota) ?: return
        val mode = if (colors.size == 1) PigmentData.Mode.SOLID else PigmentData.Mode.MORPH
        val mask = PigmentData.pickRandomMask(colors.size)
        PigmentData.write(stack, PigmentData(colors = colors, mode = mode, mask = mask))
    }

    private fun extractInputColors(iota: Iota): IntArray? = when (iota) {
        is Vec3Iota -> {
            val v = iota.vec3
            if (!v.x.isFinite() || !v.y.isFinite() || !v.z.isFinite()) null
            else intArrayOf(PigmentData.vecToArgb(v))
        }
        is ListIota -> {
            val size = iota.list.size()
            if (size !in 1..PigmentData.MAX_COLORS) {
                null
            } else {
                val out = IntArray(size)
                var idx = 0
                var valid = true
                for (elem in iota.list) {
                    if (elem !is Vec3Iota) {
                        valid = false
                        break
                    }
                    val v = elem.vec3
                    if (!v.x.isFinite() || !v.y.isFinite() || !v.z.isFinite()) {
                        valid = false
                        break
                    }
                    out[idx++] = PigmentData.vecToArgb(v)
                }
                if (valid) out else null
            }
        }
        else -> null
    }
}
