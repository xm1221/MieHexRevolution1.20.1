package cn.xm1221.MieHexRevolution.networking

import dev.architectury.networking.NetworkChannel
import cn.xm1221.MieHexRevolution.Miehex_revolution
import cn.xm1221.MieHexRevolution.networking.msg.Miehex_revolutionMessageCompanion

object Miehex_revolutionNetworking {
    val CHANNEL: NetworkChannel = NetworkChannel.create(Miehex_revolution.id("networking_channel"))

    fun init() {
        for (subclass in Miehex_revolutionMessageCompanion::class.sealedSubclasses) {
            subclass.objectInstance?.register(CHANNEL)
        }
    }
}
