package cn.xm1221.MieHexRevolution.compat.parse

import at.petrak.hexcasting.api.casting.math.HexDir
import at.petrak.hexcasting.api.casting.math.HexPattern
import at.petrak.hexcasting.common.lib.hex.HexIotaTypes
import cn.xm1221.MieHexRevolution.Miehex_revolution
import cn.xm1221.MieHexRevolution.api.casting.iota.ImportsIota
import dev.architectury.platform.Platform
import io.yukkuric.hexparse.api.HexParseAPI
import io.yukkuric.hexparse.misc.StringProcessors
import io.yukkuric.hexparse.parsers.IPlayerBinder
import io.yukkuric.hexparse.parsers.IotaFactory
import io.yukkuric.hexparse.parsers.ParserMain
import io.yukkuric.hexparse.parsers.nbt2str.INbt2Str
import io.yukkuric.hexparse.parsers.str2nbt.IStr2Nbt
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.server.level.ServerPlayer
import java.lang.reflect.Method

/**
 * HexParse（io.yukkuric.hexparse）联动：把 [ImportsIota] 写成 "Sigils" 语法，
 * 例如 `Sigils:hexal:foo→5、minecraft:bar→true`（空集合写 `Sigils:`）。
 *
 * HexParse 是**软依赖**：本文件在编译期依赖它（common 里 modApi），但运行期所有对
 * hexparse 类的触碰都发生在 [init] 内、且被 `Platform.isModLoaded("hexparse")` + try/catch
 * 包住，所以没装 hexparse 时这里的一切都不会被加载（不会 NoClassDefFoundError）。
 * 两个 parser 单例只在那个受保护的分支里被引用。
 *
 * 语法（所有分隔符都在单个 CodeCutter token 内合法，且不会出现在任何图案名或
 * 内置单 token iota 里）：
 * - 前缀 `Sigils` + 边界 `:` 或 `：`（半角/全角都接受；半角冒号不会和
 *   `hexal:foo` 这类图案长名冲突，因为我们只剥**开头**的冒号）
 * - 条目用 `、`（U+3001）分隔
 * - 每条 `图案→值`，在第一个 `→`（U+2192）处切开
 * - 图案键：优先笔顺签名（见 [SigilsBackParser.strokeKey] 与
 *   [SigilsForthParser.parsePatternToken]），也兼容 `modid:path` 长名 / 无冲突短名
 * - 值：任意 HexParse 能解析的单 token iota；列表值写 `｛a，b，c｝`
 *   （全角花括号 + 全角逗号，见 [SigilsForthParser.parseValueToken]）
 *
 * 嵌套 Sigils 作为值可以递归（例如 `Sigils:a→Sigils:b→1`），由 [MAX_NESTING] 限深，
 * 防止恶意输入打爆栈。
 */
object ImportsParseCompat {

    const val TYPE_IMPORTS = "miehex_revolution:imports"

    private const val KEY_ENTRIES = "entries"
    private const val KEY_PATTERN = "pattern"
    private const val KEY_VALUE = "value"
    private const val PREFIX = "Sigils"
    private const val SEP_ENTRY = '、'
    private const val SEP_KV = '→'
    private const val SEP_LIST_OPEN = '｛'
    private const val SEP_LIST_CLOSE = '｝'
    private const val SEP_LIST_ITEM = '，'
    private const val MAX_NESTING = 16

    /**
     * 笔顺签名的方向码 → [HexDir]。
     *
     * 大写码特意避开小写 `[wedsaq]`：HexParse 自带的 TO_RAW_PATTERN 只认
     * `^_[wedsaq]*$`（且固定 EAST 起始），我们的键形如 `_NE_aqaa`，中间有 `_`、
     * 方向码是大写，绝不会被它抢先匹配。
     */
    private val HEX_DIR_BY_CODE = mapOf(
        "NE" to HexDir.NORTH_EAST,
        "E" to HexDir.EAST,
        "SE" to HexDir.SOUTH_EAST,
        "SW" to HexDir.SOUTH_WEST,
        "W" to HexDir.WEST,
        "NW" to HexDir.NORTH_WEST,
    )

    private val HEX_DIR_CODE_OF = HEX_DIR_BY_CODE.entries.associate { (code, dir) -> dir to code }

    /** 我们自己的带起始方向笔顺签名：`_` + 方向码 + `_` + 角度签名。 */
    private val RAW_PATTERN = Regex("^_(NE|E|SE|SW|W|NW)_([wedsaq]*)$")

    /** 递归值（嵌套 Sigils / 列表）的深度守卫。 */
    private val depth = ThreadLocal.withInitial { 0 }

    /**
     * 从 [Miehex_revolution.init] 调用。仅在 hexparse 确实加载时注册两个 parser；
     * 与 ParserMain.init() 的注册顺序无关——我们的 match 是排他的（`Sigils` 前缀
     * 的 token 不会被任何内置 parser 碰）。
     */
    @JvmStatic
    fun init() {
        if (!Platform.isModLoaded("hexparse")) return
        try {
            HexParseAPI.AddForthParser(SigilsForthParser)
            HexParseAPI.AddBackParser(SigilsBackParser)
            Miehex_revolution.LOGGER.info("已注册 Sigils（ImportsIota）的 hexparse 解析器")
        } catch (t: Throwable) {
            Miehex_revolution.LOGGER.warn("hexparse 已加载但 Sigils 解析器注册失败", t)
        }
    }

    /** 字符串 → NBT：`Sigils:图案→值、图案→值` 变成序列化后的 ImportsIota tag。 */
    object SigilsForthParser : IStr2Nbt {
        override fun match(node: String): Boolean =
            node == PREFIX || node.startsWith("$PREFIX:") || node.startsWith("$PREFIX：")

        override fun parse(node: String): CompoundTag {
            var body = node.removePrefix(PREFIX)
            // 剥掉边界冒号（半角或全角）
            if (body.startsWith(":") || body.startsWith("：")) body = body.substring(1)

            val entries = ListTag()
            if (body.isNotEmpty()) {
                val parts = body.split(SEP_ENTRY)
                if (parts.size > ImportsIota.MAX_IMPORTS) {
                    throw IllegalArgumentException(
                        "Sigils 条目过多：${parts.size} > ${ImportsIota.MAX_IMPORTS}"
                    )
                }
                val nest = depth.get()
                if (nest >= MAX_NESTING) throw IllegalArgumentException("Sigils 嵌套过深")
                depth.set(nest + 1)
                try {
                    for (part in parts) {
                        val arrow = part.indexOf(SEP_KV)
                        if (arrow <= 0) {
                            throw IllegalArgumentException(
                                "Sigils 条目格式错误，应为 图案→值：$part"
                            )
                        }
                        val patStr = part.substring(0, arrow)
                        val valStr = part.substring(arrow + 1)
                        if (patStr.isEmpty() || valStr.isEmpty()) {
                            throw IllegalArgumentException("Sigils 条目中图案/值为空：$part")
                        }
                        val patTag = parsePatternToken(patStr)
                        if (patTag.getString(HexIotaTypes.KEY_TYPE) != IotaFactory.TYPE_PATTERN) {
                            throw IllegalArgumentException("不是图案：$patStr")
                        }
                        val valTag = parseValueToken(valStr)
                        val entry = CompoundTag()
                        entry.put(KEY_PATTERN, patTag)
                        entry.put(KEY_VALUE, valTag)
                        entries.add(entry)
                    }
                } finally {
                    depth.set(nest)
                }
            }

            val data = CompoundTag()
            data.put(KEY_ENTRIES, entries)
            return IotaFactory.makeType(TYPE_IMPORTS, data)
        }

        /**
         * 图案键 → PatternIota。
         *
         * 优先解析我们自己的带起始方向笔顺签名（`_NE_aqaa`，见 [RAW_PATTERN]）：
         * 方向码 → [HexDir]，角度串原样交给 [IotaFactory.makePattern]（它就是 HexParse
         * 官方从角度串构造 PatternIota 的入口，`start_dir` 与角度字节表完全按
         * hexcasting 的 NBT 格式写），所以 round-trip 后起始方向分毫不差。
         *
         * 不匹配笔顺签名的，回退 [ParserMain.ParseSingleNode] 走 HexParse 原生的
         * `modid:path` 长名 / 短名（以及兼容用户手写的裸 `_aqaa`，那是 TO_RAW_PATTERN
         * 的领地，固定 EAST 起始，是 HexParse 自己的约定）。
         */
        private fun parsePatternToken(s: String): CompoundTag {
            RAW_PATTERN.matchEntire(s)?.let { m ->
                val dir = HEX_DIR_BY_CODE.getValue(m.groupValues[1])
                return IotaFactory.makePattern(m.groupValues[2], dir)
            }
            return ParserMain.ParseSingleNode(s)
                ?: throw IllegalArgumentException("未知图案：$s")
        }

        /**
         * 单条目的值。普通单 token 直接走 [ParserMain.ParseSingleNode]；
         * `｛a，b，c｝`（全角花括号 + 全角逗号）是列表值，递归解析成 ListIota。
         * 全角字符都在 CodeCutter 的 token 字符类（`[\w./\-:#\u0100-\uffff]+`）里，
         * 整个 `｛...｝` 保持为一个 token，不会和 HexParse 自己的半角 `[ ]` 嵌套冲突。
         */
        private fun parseValueToken(s: String): CompoundTag {
            if (s.startsWith(SEP_LIST_OPEN.toString())) {
                if (!s.endsWith(SEP_LIST_CLOSE.toString())) {
                    throw IllegalArgumentException("列表值缺少右花括号：$s")
                }
                val nest = depth.get()
                if (nest >= MAX_NESTING) throw IllegalArgumentException("Sigils 列表嵌套过深")
                depth.set(nest + 1)
                try {
                    val inner = s.substring(1, s.length - 1)
                    val subs = ListTag()
                    if (inner.isNotEmpty()) {
                        for (elem in splitListTopLevel(inner)) {
                            subs.add(parseValueToken(elem))
                        }
                    }
                    return IotaFactory.makeList(subs)
                } finally {
                    depth.set(nest)
                }
            }
            return ParserMain.ParseSingleNode(s)
                ?: throw IllegalArgumentException("未知值：$s")
        }

        /** 按深度 0 的 `，` 切分，让嵌套的 `｛...｝` 块保持完整。 */
        private fun splitListTopLevel(s: String): List<String> {
            val res = mutableListOf<String>()
            var d = 0
            val cur = StringBuilder()
            for (ch in s) {
                when {
                    ch == SEP_LIST_OPEN -> {
                        d++
                        cur.append(ch)
                    }
                    ch == SEP_LIST_CLOSE -> {
                        d--
                        if (d < 0) throw IllegalArgumentException("列表值括号不配对：$s")
                        cur.append(ch)
                    }
                    ch == SEP_LIST_ITEM && d == 0 -> {
                        res.add(cur.toString())
                        cur.setLength(0)
                    }
                    else -> cur.append(ch)
                }
            }
            if (d != 0) throw IllegalArgumentException("列表值括号不配对：$s")
            res.add(cur.toString())
            return res
        }
    }

    /**
     * NBT → 字符串：序列化后的 ImportsIota tag 变成 `Sigils:图案→值、图案→值`。
     *
     * 图案**键**永远以笔顺签名导出（`_` + 起始方向码 + `_` + 角度签名，见
     * [strokeKey]），绝不导出注册动作名：键的 round-trip 必须还原出当初绑定的那个
     * 精确 [HexPattern]，而名字是按世界/注册表来的、有歧义；笔顺签名则由我们自己
     * 的 [SigilsForthParser.parsePatternToken] 解析回来，带起始方向。
     *
     * 两侧都不带公共入口 [ParserMain.ParseIotaNbt] 会附加的 MetaHolder 头
     * （`// Author: ...` / `// Requires: ...`）：在解析时调用它会把头**注进**我们的
     * `Sigils:` token 里，破坏 round-trip（粘回去时 CodeCutter 会把 `Sigils://` 粘成
     * 一个 token）。
     */
    object SigilsBackParser : INbt2Str, IPlayerBinder {
        private var player: ServerPlayer? = null

        override fun BindPlayer(p: ServerPlayer) {
            player = p
        }

        override fun match(node: CompoundTag): Boolean = isType(node, TYPE_IMPORTS)

        override fun parse(node: CompoundTag): String {
            val data = node.getCompound(HexIotaTypes.KEY_DATA)
            val entries = data.getList(KEY_ENTRIES, Tag.TAG_COMPOUND.toInt())
            val sb = StringBuilder(PREFIX + ":")
            val p = player
            for (i in 0 until entries.size) {
                if (i > 0) sb.append(SEP_ENTRY)
                val entry = entries.getCompound(i)
                val patStr = strokeKey(entry.getCompound(KEY_PATTERN))
                val valStr = parseIotaNoMeta(entry.getCompound(KEY_VALUE), p)
                sb.append(patStr).append(SEP_KV).append(valStr)
            }
            return sb.toString()
        }

        /**
         * 单条目的键：`_` + 起始方向码（大写）+ `_` + 角度签名，例如 `_NE_aqaa`。
         *
         * 直接读 NBT 里的 [HexPattern]：起始方向取 `startDir`，角度用
         * `anglesSignature()`（与 `IotaFactory.ANGLE_MAP` 的 w/e/d/s/a/q 顺序一致）。
         * 这样 round-trip 连起始方向都精确还原——之前复用 TO_RAW_PATTERN（固定 EAST）
         * 时，非 EAST 起始的键会被旋转，导致 imports 的精确匹配失效。
         */
        private fun strokeKey(patTag: CompoundTag): String {
            val pattern = HexPattern.fromNBT(patTag.getCompound(HexIotaTypes.KEY_DATA))
            val dirCode = HEX_DIR_CODE_OF[pattern.startDir] ?: "E"
            return "_" + dirCode + "_" + pattern.anglesSignature()
        }

        /**
         * 值侧 iota → 代码，复刻 `ParserMain.ParseIotaNbt(..., READ_DEFAULT)`，
         * 但**不带** MetaHolder 头（公共入口每次调用都会前置 `// Author: ...`，
         * 会污染我们的 `Sigils:` token）。反射目标不可用时回退公共入口
         * （输出一致，只差装饰性的头）。
         *
         * 列表值渲染成 `｛a，b，c｝`（全角花括号/全角逗号，与 forward 语法一致）：
         * HexParse 自己的列表渲染用半角 `[ ]`，会被 CodeCutter 切成独立 token，
         * 在我们这个单 token 里破坏 round-trip。
         */
        private fun parseIotaNoMeta(tag: CompoundTag, p: ServerPlayer?): String {
            if (tag.getString(HexIotaTypes.KEY_TYPE) == IotaFactory.TYPE_LIST) {
                val subs = tag.getList(HexIotaTypes.KEY_DATA, Tag.TAG_COMPOUND.toInt())
                val sb = StringBuilder()
                sb.append(SEP_LIST_OPEN)
                for (i in 0 until subs.size) {
                    if (i > 0) sb.append(SEP_LIST_ITEM)
                    sb.append(parseIotaNoMeta(subs.getCompound(i), p))
                }
                sb.append(SEP_LIST_CLOSE)
                return sb.toString()
            }
            val raw = NBT2STR_INTERNAL?.let { m ->
                // isRoot=false 让外层读取的 meta 头不进我们的 token；只有列表渲染
                // 有差别（上面已自行处理），单个 iota 与 isRoot 无关。
                runCatching { m.invoke(null, tag, p, 0, false) as? String }.getOrNull()
            }
            if (raw != null) return StringProcessors.READ_DEFAULT.apply(raw)
            // 回退：公共入口——输出一致，只是带 MetaHolder 头。
            return ParserMain.ParseIotaNbt(tag, p, StringProcessors.READ_DEFAULT)
        }

        /** `ParserMain._parseIotaNbt`（私有）：子 iota → 字符串，不带 meta 头。 */
        private val NBT2STR_INTERNAL: Method? = runCatching {
            ParserMain::class.java.getDeclaredMethod(
                "_parseIotaNbt",
                CompoundTag::class.java,
                ServerPlayer::class.java,
                Int::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType
            ).apply { isAccessible = true }
        }.getOrNull()
    }
}
