package cn.xm1221.MieHexRevolution.networking.msg

import cn.xm1221.MieHexRevolution.config.Miehex_revolutionServerConfig
import net.minecraft.network.FriendlyByteBuf

data class MsgSyncConfigS2C(val serverConfig: Miehex_revolutionServerConfig.ServerConfig) : Miehex_revolutionMessageS2C {
    companion object : Miehex_revolutionMessageCompanion<MsgSyncConfigS2C> {
        override val type = MsgSyncConfigS2C::class.java

        override fun decode(buf: FriendlyByteBuf) = MsgSyncConfigS2C(
            serverConfig = Miehex_revolutionServerConfig.ServerConfig().decode(buf),
        )

        override fun MsgSyncConfigS2C.encode(buf: FriendlyByteBuf) {
            serverConfig.encode(buf)
        }
    }
}
