package cn.xm1221.MieHexRevolution.networking

import cn.xm1221.MieHexRevolution.networking.msg.MsgSyncConfigS2C
import cn.xm1221.MieHexRevolution.networking.msg.MsgSyncImportKeysS2C
import java.util.concurrent.atomic.AtomicBoolean

object Miehex_revolutionNetworking {
    private val initialized = AtomicBoolean(false)

    fun init() {
        if (!initialized.compareAndSet(false, true)) return
        MsgSyncConfigS2C.register()
        MsgSyncImportKeysS2C.register()
    }
}
