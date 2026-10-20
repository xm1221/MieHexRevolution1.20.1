package cn.xm1221.MieHexRevolution.networking.msg

import cn.xm1221.MieHexRevolution.Miehex_revolution
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerPlayer

/**
 * Server → client: the import keys active in the caster's current cast, as
 * `startDirOrdinal:anglesSignature` strings (see the same encoding in
 * `MieHexRevolutionHelpersKt` and `MixinGuiSpellcasting`). An empty list means no imports
 * are active. Sent on every resolved pattern so the client is always in sync (a new cast
 * without imports clears the previous markings).
 */
data class MsgSyncImportKeysS2C(val keys: List<String>) : Miehex_revolutionMessageS2C {
    override fun sendToPlayer(player: ServerPlayer) = Companion.sendS2C(player, this)

    companion object : Miehex_revolutionMessageCompanion<MsgSyncImportKeysS2C> {
        override val id = Miehex_revolution.id("sync_import_keys")

        fun register() = registerS2C()

        override fun decode(buf: FriendlyByteBuf) = MsgSyncImportKeysS2C(
            keys = buf.readList { it.readUtf(32767) },
        )

        override fun MsgSyncImportKeysS2C.encode(buf: FriendlyByteBuf) {
            buf.writeCollection(keys) { sink, key -> sink.writeUtf(key) }
        }
    }
}
