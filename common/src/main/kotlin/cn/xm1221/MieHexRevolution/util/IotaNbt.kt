package cn.xm1221.MieHexRevolution.util

import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.IotaType
import net.minecraft.nbt.NbtOps
import net.minecraft.nbt.Tag

/**
 * Hex 0.12 removed `IotaType.serialize`/`IotaType.deserialize`; iotas are now serialised
 * through the codec-backed [IotaType.TYPED_CODEC]. These two helpers keep the call sites
 * that still think in terms of NBT tags readable.
 */
object IotaNbt {
    /** Encode an iota to an NBT tag (or throw if the codec rejects it). */
    @JvmStatic
    fun serialize(iota: Iota): Tag =
        IotaType.TYPED_CODEC.encodeStart(NbtOps.INSTANCE, iota).getOrThrow()

    /** Decode an iota from an NBT tag, returning `null` if it is not a valid iota. */
    @JvmStatic
    fun deserialize(tag: Tag?): Iota? {
        if (tag == null) return null
        return IotaType.TYPED_CODEC.parse(NbtOps.INSTANCE, tag).result().orElse(null)
    }
}
