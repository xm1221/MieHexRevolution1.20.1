package cn.xm1221.MieHexRevolution.networking.msg

import net.minecraft.network.FriendlyByteBuf

/**
 * Server → client: the import keys active in the caster's current cast, as
 * `startDirOrdinal:anglesSignature` strings (see the same encoding in
 * `MieHexRevolutionHelpersKt` and `MixinGuiSpellcasting`). An empty list means no imports
 * are active. Sent on every resolved pattern so the client is always in sync (a new cast
 * without imports clears the previous markings).
 */
data class MsgSyncImportKeysS2C(val keys: List<String>) : Miehex_revolutionMessageS2C {
    companion object : Miehex_revolutionMessageCompanion<MsgSyncImportKeysS2C> {
        override val type = MsgSyncImportKeysS2C::class.java

        override fun decode(buf: FriendlyByteBuf) = MsgSyncImportKeysS2C(
            keys = buf.readList { it.readUtf(32767) },
        )

        override fun MsgSyncImportKeysS2C.encode(buf: FriendlyByteBuf) {
            buf.writeCollection(keys) { sink, key -> sink.writeUtf(key) }
        }
    }
}