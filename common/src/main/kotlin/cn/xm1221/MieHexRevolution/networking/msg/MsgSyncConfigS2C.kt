package cn.xm1221.MieHexRevolution.networking.msg

import cn.xm1221.MieHexRevolution.Miehex_revolution
import cn.xm1221.MieHexRevolution.config.Miehex_revolutionServerConfig
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerPlayer

data class MsgSyncConfigS2C(val serverConfig: Miehex_revolutionServerConfig.ServerConfig) : Miehex_revolutionMessageS2C {
    override fun sendToPlayer(player: ServerPlayer) = Companion.sendS2C(player, this)

    companion object : Miehex_revolutionMessageCompanion<MsgSyncConfigS2C> {
        override val id = Miehex_revolution.id("sync_config")

        fun register() = registerS2C()

        override fun decode(buf: FriendlyByteBuf) = MsgSyncConfigS2C(
            serverConfig = Miehex_revolutionServerConfig.ServerConfig().decode(buf),
        )

        override fun MsgSyncConfigS2C.encode(buf: FriendlyByteBuf) {
            serverConfig.encode(buf)
        }
    }
}
