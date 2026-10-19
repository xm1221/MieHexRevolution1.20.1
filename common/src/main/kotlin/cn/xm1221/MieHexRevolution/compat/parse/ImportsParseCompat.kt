package cn.xm1221.MieHexRevolution.compat.parse

import cn.xm1221.MieHexRevolution.Miehex_revolution
import dev.architectury.platform.Platform

/**
 * HexParse（io.yukkuric.hexparse）联动入口。
 *
 * HexParse 只有 1.20 的发行版，其 API 依赖大量 1.20 专有类型（NBT 序列化的 iota、
 * `IStr2Nbt` / `INbt2Str` / `IPlayerBinder` 等），在 1.21 上既没有可用 jar，也无法
 * 用同一套接口编译。因此这里退化成**空实现**：
 *
 * - 不再硬 import 任何 `io.yukkuric.hexparse.*` 类（避免编译/类加载失败）；
 * - [init] 只在 hexparse 真的加载时打一条提示，说明 Sigils 语法在本版本不可用。
 *
 * `ImportsIota` 本身以及它在施法 VM 中的行为不受影响；丢失的只是 HexParse 的
 * 字符串 ↔ NBT 互转（`Sigils:图案→值、...`）。
 */
object ImportsParseCompat {

    const val TYPE_IMPORTS = "miehex_revolution:imports"

    @JvmStatic
    fun init() {
        if (!Platform.isModLoaded("hexparse")) return
        Miehex_revolution.LOGGER.warn(
            "检测到 hexparse，但本 1.21 移植版未提供 Sigils 字符串解析器；" +
                "ImportsIota 仍可正常施法，仅 hexparse 的文本互转不可用"
        )
    }
}
