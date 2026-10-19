package cn.xm1221.MieHexRevolution.util

/**
 * Client-side cache of the import keys active in the caster's current cast, synchronized
 * from the server via [cn.xm1221.MieHexRevolution.networking.msg.MsgSyncImportKeysS2C].
 * Keys are `startDirOrdinal:anglesSignature` strings; a missing/empty set means no imports
 * are active. Only ever touched on the client thread.
 */
object ClientImportKeys {
    private var keys: Set<String> = emptySet()

    @JvmStatic
    fun update(newKeys: List<String>) {
        keys = newKeys.toHashSet()
    }

    @JvmStatic
    fun contains(key: String): Boolean = keys.contains(key)
}