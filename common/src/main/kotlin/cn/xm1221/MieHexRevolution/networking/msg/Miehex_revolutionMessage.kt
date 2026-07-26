package cn.xm1221.MieHexRevolution.networking.msg

import dev.architectury.networking.NetworkChannel
import dev.architectury.networking.NetworkManager.PacketContext
import cn.xm1221.MieHexRevolution.Miehex_revolution
import cn.xm1221.MieHexRevolution.networking.Miehex_revolutionNetworking
import cn.xm1221.MieHexRevolution.networking.handler.applyOnClient
import cn.xm1221.MieHexRevolution.networking.handler.applyOnServer
import net.fabricmc.api.EnvType
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerPlayer
import java.util.function.Supplier

sealed interface Miehex_revolutionMessage

sealed interface Miehex_revolutionMessageC2S : Miehex_revolutionMessage {
    fun sendToServer() {
        Miehex_revolutionNetworking.CHANNEL.sendToServer(this)
    }
}

sealed interface Miehex_revolutionMessageS2C : Miehex_revolutionMessage {
    fun sendToPlayer(player: ServerPlayer) {
        Miehex_revolutionNetworking.CHANNEL.sendToPlayer(player, this)
    }

    fun sendToPlayers(players: Iterable<ServerPlayer>) {
        Miehex_revolutionNetworking.CHANNEL.sendToPlayers(players, this)
    }
}

sealed interface Miehex_revolutionMessageCompanion<T : Miehex_revolutionMessage> {
    val type: Class<T>

    fun decode(buf: FriendlyByteBuf): T

    fun T.encode(buf: FriendlyByteBuf)

    fun apply(msg: T, supplier: Supplier<PacketContext>) {
        val ctx = supplier.get()
        when (ctx.env) {
            EnvType.SERVER, null -> {
                Miehex_revolution.LOGGER.debug("Server received packet from {}: {}", ctx.player.name.string, this)
                when (msg) {
                    is Miehex_revolutionMessageC2S -> msg.applyOnServer(ctx)
                    else -> Miehex_revolution.LOGGER.warn("Message not handled on server: {}", msg::class)
                }
            }
            EnvType.CLIENT -> {
                Miehex_revolution.LOGGER.debug("Client received packet: {}", this)
                when (msg) {
                    is Miehex_revolutionMessageS2C -> msg.applyOnClient(ctx)
                    else -> Miehex_revolution.LOGGER.warn("Message not handled on client: {}", msg::class)
                }
            }
        }
    }

    fun register(channel: NetworkChannel) {
        channel.register(type, { msg, buf -> msg.encode(buf) }, ::decode, ::apply)
    }
}
