package cn.xm1221.MieHexRevolution.networking.handler

import dev.architectury.networking.NetworkManager.PacketContext
import cn.xm1221.MieHexRevolution.config.Miehex_revolutionServerConfig
import cn.xm1221.MieHexRevolution.networking.msg.*
import cn.xm1221.MieHexRevolution.util.ClientImportKeys

fun Miehex_revolutionMessageS2C.applyOnClient(ctx: PacketContext) = ctx.queue {
    when (this) {
        is MsgSyncConfigS2C -> {
            Miehex_revolutionServerConfig.onSyncConfig(serverConfig)
        }

        is MsgSyncImportKeysS2C -> {
            ClientImportKeys.update(keys)
        }

        // add more client-side message handlers here
    }
}
