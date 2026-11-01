package cn.xm1221.MieHexRevolution.networking.msg

import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.phys.Vec3

/** S2C：清除染色区。[radius] ≤ 0 表示清空该维度全部，否则只清与球相交的。 */
data class MsgClearSkyTintS2C(
    val dimension: ResourceLocation,
    val center: Vec3,
    val radius: Double,
) : Miehex_revolutionMessageS2C {
    companion object : Miehex_revolutionMessageCompanion<MsgClearSkyTintS2C> {
        override val type = MsgClearSkyTintS2C::class.java

        override fun decode(buf: FriendlyByteBuf) = MsgClearSkyTintS2C(
            dimension = buf.readResourceLocation(),
            center = Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()),
            radius = buf.readDouble(),
        )

        override fun MsgClearSkyTintS2C.encode(buf: FriendlyByteBuf) {
            buf.writeResourceLocation(dimension)
            buf.writeDouble(center.x)
            buf.writeDouble(center.y)
            buf.writeDouble(center.z)
            buf.writeDouble(radius)
        }
    }
}
