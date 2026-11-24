package cn.xm1221.MieHexRevolution.networking

import cn.xm1221.MieHexRevolution.networking.msg.MsgClearSkyTintS2C
import cn.xm1221.MieHexRevolution.networking.msg.MsgSkyTintS2C
import cn.xm1221.MieHexRevolution.networking.msg.MsgSyncConfigS2C
import cn.xm1221.MieHexRevolution.networking.msg.MsgSyncImportKeysS2C
import java.util.concurrent.atomic.AtomicBoolean

object Miehex_revolutionNetworking {
    private val initialized = AtomicBoolean(false)

    fun init() {
        if (!initialized.compareAndSet(false, true)) return
        MsgSyncConfigS2C.register()
        MsgSyncImportKeysS2C.register()
        // 天空染色：S2C-only，服务端无状态（计时在客户端）。
        MsgSkyTintS2C.register()
        MsgClearSkyTintS2C.register()
    }
}
