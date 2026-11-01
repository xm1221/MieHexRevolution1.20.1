package cn.xm1221.MieHexRevolution.networking.msg

import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.phys.Vec3

/** S2C：新增/刷新一个天空染色区。服务端无状态，[durationTicks] 由客户端自己计时。 */
data class MsgSkyTintS2C(
    val dimension: ResourceLocation,
    val center: Vec3,
    val radius: Double,
    val color: Vec3,
    val durationTicks: Int,
    val strength: Double,
) : Miehex_revolutionMessageS2C {
    companion object : Miehex_revolutionMessageCompanion<MsgSkyTintS2C> {
        override val type = MsgSkyTintS2C::class.java

        override fun decode(buf: FriendlyByteBuf) = MsgSkyTintS2C(
            dimension = buf.readResourceLocation(),
            center = Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()),
            radius = buf.readDouble(),
            color = Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()),
            durationTicks = buf.readVarInt(),
            strength = buf.readDouble(),
        )

        override fun MsgSkyTintS2C.encode(buf: FriendlyByteBuf) {
            buf.writeResourceLocation(dimension)
            buf.writeDouble(center.x)
            buf.writeDouble(center.y)
            buf.writeDouble(center.z)
            buf.writeDouble(radius)
            buf.writeDouble(color.x)
            buf.writeDouble(color.y)
            buf.writeDouble(color.z)
            buf.writeVarInt(durationTicks)
            buf.writeDouble(strength)
        }
    }
}
