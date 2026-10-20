package cn.xm1221.MieHexRevolution.networking.msg

import cn.xm1221.MieHexRevolution.Miehex_revolution
import cn.xm1221.MieHexRevolution.networking.handler.applyOnClient
import cn.xm1221.MieHexRevolution.networking.handler.applyOnServer
import dev.architectury.networking.NetworkManager
import dev.architectury.networking.NetworkManager.PacketContext
import io.netty.buffer.Unpooled
import net.fabricmc.api.EnvType
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import java.util.function.Supplier

sealed interface Miehex_revolutionMessage

sealed interface Miehex_revolutionMessageC2S : Miehex_revolutionMessage

sealed interface Miehex_revolutionMessageS2C : Miehex_revolutionMessage {
    fun sendToPlayer(player: ServerPlayer)
    fun sendToPlayers(players: Iterable<ServerPlayer>) {
        for (p in players) sendToPlayer(p)
    }
}

sealed interface Miehex_revolutionMessageCompanion<T : Miehex_revolutionMessage> {
    val id: ResourceLocation

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

    /** S2C-only. Never also register as C2S (NeoForge rejects same id both ways). */
    fun registerS2C() {
        NetworkManager.registerReceiver(NetworkManager.s2c(), id) { buf, ctx ->
            apply(decode(buf), Supplier { ctx })
        }
    }

    fun sendS2C(player: ServerPlayer, msg: T) {
        val buf = RegistryFriendlyByteBuf(Unpooled.buffer(), player.registryAccess())
        msg.encode(buf)
        NetworkManager.sendToPlayer(player, id, buf)
    }
}
