package cn.xm1221.MieHexRevolution.compat.parse

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
 * HexParse (io.yukkuric.hexparse) integration that spells [ImportsIota] as "Sigils" inside
 * HexParse code, e.g. `Sigils:hexal:foo→5、minecraft:bar→true` (empty set is `Sigils:`).
 *
 * HexParse is a *soft* dependency: this file compiles against it (`modApi` in common), but at
 * runtime every touch of hexparse classes happens inside [init] behind
 * `Platform.isModLoaded("hexparse")` + try/catch, so with hexparse absent nothing here is ever
 * loaded (no NoClassDefFoundError). The two parser singletons are only referenced from that
 * guarded branch.
 *
 * Syntax (all separators are legal inside a single CodeCutter token, and can never appear in a
 * pattern name or any built-in single-token iota):
 * - prefix `Sigils` + boundary `:` or `：` (half/full-width, both accepted; the half-width colon
 *   never collides because pattern long names like `hexal:foo` keep their own `:` and we only
 *   strip the *leading* colon)
 * - entries separated by `、` (U+3001)
 * - each entry `pattern→value` split on the first `→` (U+2192)
 * - patterns support long names (`modid:path`, always registered by PatternMapper) and
 *   conflict-free short names; values are any single-token iota HexParse understands
 *
 * Nested Sigils as a value works recursively (e.g. `Sigils:a→Sigils:b→1`), bounded by
 * [MAX_NESTING] to keep adversarial input from overflowing the stack.
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

    /** Depth guard for recursive values (nested Sigils). */
    private val depth = ThreadLocal.withInitial { 0 }

    /**
     * Call from [Miehex_revolution.init]. Registers the two parsers only when hexparse is
     * actually loaded; registration order vs ParserMain.init() does not matter because our
     * match is exclusive (`Sigils`-prefixed tokens are never touched by any built-in parser).
     */
    @JvmStatic
    fun init() {
        if (!Platform.isModLoaded("hexparse")) return
        try {
            HexParseAPI.AddForthParser(SigilsForthParser)
            HexParseAPI.AddBackParser(SigilsBackParser)
            Miehex_revolution.LOGGER.info("Registered Sigils (ImportsIota) hexparse parsers")
        } catch (t: Throwable) {
            Miehex_revolution.LOGGER.warn("hexparse present but Sigils parser registration failed", t)
        }
    }

    /** String → NBT: `Sigils:pat→val、pat→val` becomes the serialized ImportsIota tag. */
    object SigilsForthParser : IStr2Nbt {
        override fun match(node: String): Boolean =
            node == PREFIX || node.startsWith("$PREFIX:") || node.startsWith("$PREFIX：")

        override fun parse(node: String): CompoundTag {
            var body = node.removePrefix(PREFIX)
            // strip the boundary colon (half- or full-width)
            if (body.startsWith(":") || body.startsWith("：")) body = body.substring(1)

            val entries = ListTag()
            if (body.isNotEmpty()) {
                val parts = body.split(SEP_ENTRY)
                if (parts.size > ImportsIota.MAX_IMPORTS) {
                    throw IllegalArgumentException(
                        "too many Sigils entries: ${parts.size} > ${ImportsIota.MAX_IMPORTS}"
                    )
                }
                val nest = depth.get()
                if (nest >= MAX_NESTING) throw IllegalArgumentException("Sigils nested too deeply")
                depth.set(nest + 1)
                try {
                    for (part in parts) {
                        val arrow = part.indexOf(SEP_KV)
                        if (arrow <= 0) {
                            throw IllegalArgumentException(
                                "malformed Sigils entry, expected pattern→value: $part"
                            )
                        }
                        val patStr = part.substring(0, arrow)
                        val valStr = part.substring(arrow + 1)
                        if (patStr.isEmpty() || valStr.isEmpty()) {
                            throw IllegalArgumentException("empty pattern/value in Sigils entry: $part")
                        }
                        val patTag = ParserMain.ParseSingleNode(patStr)
                            ?: throw IllegalArgumentException("unknown pattern: $patStr")
                        if (patTag.getString(HexIotaTypes.KEY_TYPE) != IotaFactory.TYPE_PATTERN) {
                            throw IllegalArgumentException("not a pattern: $patStr")
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
         * Value side of one entry. A plain single token goes straight through
         * [ParserMain.ParseSingleNode]; a `｛a，b，c｝` (full-width braces + full-width comma)
         * block is a list value, recursively parsed into a ListIota. Full-width chars are inside
         * CodeCutter's token class (`[\w./\-:#\u0100-\uffff]+`), so the whole `｛...｝` block stays
         * one token and never collides with HexParse's own half-width `[ ]` nesting.
         */
        private fun parseValueToken(s: String): CompoundTag {
            if (s.startsWith(SEP_LIST_OPEN.toString())) {
                if (!s.endsWith(SEP_LIST_CLOSE.toString())) {
                    throw IllegalArgumentException("unclosed list value: $s")
                }
                val nest = depth.get()
                if (nest >= MAX_NESTING) throw IllegalArgumentException("Sigils list nested too deeply")
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
                ?: throw IllegalArgumentException("unknown value: $s")
        }

        /** Split on depth-0 `，` so nested `｛...｝` blocks stay intact. */
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
                        if (d < 0) throw IllegalArgumentException("unbalanced list value: $s")
                        cur.append(ch)
                    }
                    ch == SEP_LIST_ITEM && d == 0 -> {
                        res.add(cur.toString())
                        cur.setLength(0)
                    }
                    else -> cur.append(ch)
                }
            }
            if (d != 0) throw IllegalArgumentException("unbalanced list value: $s")
            res.add(cur.toString())
            return res
        }
    }

    /**
     * NBT → String: serialized ImportsIota tag becomes `Sigils:pat→val、pat→val`.
     *
     * The pattern KEY is always exported in its stroke-direction form (`_` + angles signature),
     * never as a registered action name: a key round-trip must reproduce the exact [HexPattern]
     * that was bound, and names are ambiguous / per-world, while the raw `_aqaa/...` token is
     * parsed back by HexParse's own TO_RAW_PATTERN on import (see `SigilsForthParser.parse`).
     *
     * Both sides are rendered WITHOUT the `MetaHolder` header that the public
     * [ParserMain.ParseIotaNbt] entry point prepends (`// Author: ...` / `// Requires: ...`):
     * calling it here would inject the header *inside* our `Sigils:` token and break the
     * round-trip (the pasted code would glue `Sigils://` into one CodeCutter token).
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
         * The key side of one entry: `"_" + anglesSignature()`, HexParse's own raw-pattern syntax
         * (TO_RAW_PATTERN parses exactly `^_[wedsaq]*$` back into a PatternIota). Reading the
         * pattern straight from NBT also avoids the public ParseIotaNbt meta header entirely.
         */
        private fun strokeKey(patTag: CompoundTag): String {
            val pattern = HexPattern.fromNBT(patTag.getCompound(HexIotaTypes.KEY_DATA))
            return "_" + pattern.anglesSignature()
        }

        /**
         * Iota → code for the value side, replicating `ParserMain.ParseIotaNbt(...,
         * READ_DEFAULT)` but WITHOUT the MetaHolder header injection (the public entry point
         * prepends `// Author: ...` on every call, which would corrupt our `Sigils:` token).
         * Falls back to the public call if the reflection target is unavailable (same output,
         * only the cosmetic header differs).
         *
         * A list value is rendered as `｛a，b，c｝` (full-width braces/comma, matching the forward
         * syntax): HexParse's own list renderer uses half-width `[ ]` brackets, which CodeCutter
         * would split into separate tokens and break the round-trip inside our single token.
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
                // isRoot=false keeps the outer read's meta header off our token; only list
                // rendering differs (handled above), single iotas ignore isRoot.
                runCatching { m.invoke(null, tag, p, 0, false) as? String }.getOrNull()
            }
            if (raw != null) return StringProcessors.READ_DEFAULT.apply(raw)
            // Fallback: public entry point — same output, but carries the MetaHolder header.
            return ParserMain.ParseIotaNbt(tag, p, StringProcessors.READ_DEFAULT)
        }

        /** `ParserMain._parseIotaNbt` (private): sub-iota → string with no meta header. */
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
