package cn.xm1221.MieHexRevolution.api.casting.iota

import at.petrak.hexcasting.api.casting.eval.CastResult
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.eval.ResolvedPatternType
import at.petrak.hexcasting.api.casting.eval.vm.CastingVM
import at.petrak.hexcasting.api.casting.eval.vm.ImageComponentType
import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.IotaType
import at.petrak.hexcasting.api.casting.iota.PatternIota
import at.petrak.hexcasting.api.casting.math.HexPattern
import at.petrak.hexcasting.common.lib.hex.HexEvalSounds
import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.nbt.NbtOps
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TextColor
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import java.util.function.Supplier

/**
 * A set of "imports": a mapping of pattern -> iota, used to bind spell patterns to values.
 *
 * When this iota is executed it is stored into the cast's [at.petrak.hexcasting.api.casting.eval.vm.CastingImage]
 * components, and any [PatternIota] executed later in the same cast whose pattern is a key here
 * evaluates to the bound value instead of performing its usual action (see
 * [cn.xm1221.MieHexRevolution.util.executeWithImports]).
 *
 * Keys are [HexPattern] (a data class with structural equality) rather than [PatternIota], because iotas
 * use reference equality and would never match after a codec round-trip.
 *
 * Hex 0.12 removed arbitrary `CastingImage.userData`; per-cast state lives in registered
 * [ImageComponentType]s. **The [COMPONENT]/[RAW_COMPONENT] image components below are the
 * intended, permanent replacement for the old userData design — not a temporary shim.** Both
 * are registered in `MieHexRevolutionImageComponents`, and every read/write in the addon goes
 * through them ([cn.xm1221.MieHexRevolution.util.executeWithImports], the import ops and
 * `OpRawPattern`). Client-side tinting is separate: it is synced per stroke into
 * [cn.xm1221.MieHexRevolution.util.ClientImportKeys] and never reads the image.
 */
class ImportsIota(val imports: Map<HexPattern, Iota>) : Iota(Supplier { Type }) {

    override fun isTruthy(env: CastingEnvironment?): Boolean = true

    override fun executable(): Boolean = true

    override fun toleratesOther(that: Iota?): Boolean =
        that is ImportsIota && this.imports == that.imports

    override fun size(): Int {
        var size = 0
        for (i in imports.values) {
            size += i.size()
        }
        size += size + 1
        return size
    }

    override fun depth(): Int = 1 + (imports.values.maxOfOrNull { it.depth() } ?: 0)

    override fun subIotas(): Iterable<Iota> = imports.values

    override fun execute(vm: CastingVM, world: ServerLevel, continuation: SpellContinuation): CastResult {
        val newimg = vm.image.withComponent(COMPONENT, this)
        return CastResult(
            cast = this,
            continuation = continuation,
            newData = newimg,
            sideEffects = listOf(),
            resolutionType = ResolvedPatternType.EVALUATED,
            sound = HexEvalSounds.HERMES.get()
        )
    }

    override fun hashCode(): Int = imports.hashCode()

    override fun display(): Component {
        // Main text carries the entry count so two import sets can be told apart at a glance,
        // while hovering shows up to MAX_DISPLAY_ENTRIES key/value pairs.
        val body = Component.literal("")
        val shown = minOf(imports.size, MAX_DISPLAY_ENTRIES)
        for ((index, entry) in imports.entries.withIndex()) {
            if (index >= shown) break
            if (index > 0) body.append("\n")
            body.append(PatternIota(entry.key).display())
            body.append("  →  ")
            body.append(entry.value.display())
        }
        if (imports.size > shown) body.append("\n…(+${imports.size - shown})")
        val style = Style.EMPTY
            .withFont(ResourceLocation.tryParse("minecraft:illageralt"))
            .withColor(TextColor.fromRgb(Type.color()))
            .withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, body))
        val res = Component.literal("SGILIS").withStyle(style)
        if (imports.isNotEmpty()) {
            res.append(
                Component.literal(" x${imports.size}")
                    .withStyle(Style.EMPTY.withFont(ResourceLocation.tryParse("minecraft:default")))
            )
        }
        return res
    }

    /** One pattern -> value pair, as stored by [CODEC]. */
    private data class Entry(val pattern: HexPattern, val value: Iota)

    companion object {
        /**
         * Upper bound on how many bindings will be read back from a serialized import set, mirroring
         * `HexIotaTypes.MAX_SERIALIZATION_TOTAL` so a single import set cannot exceed the
         * serialization budget the rest of the ecosystem uses.
         */
        const val MAX_IMPORTS = 1024

        /** How many key/value pairs are shown in the hover tooltip before truncating. */
        const val MAX_DISPLAY_ENTRIES = 8

        private val ENTRY_CODEC: Codec<Entry> = RecordCodecBuilder.create { inst ->
            inst.group(
                HexPattern.CODEC.fieldOf("pattern").forGetter(Entry::pattern),
                IotaType.TYPED_CODEC.fieldOf("value").forGetter(Entry::value)
            ).apply(inst, ::Entry)
        }

        private val ENTRY_STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, Entry> = StreamCodec.composite(
            HexPattern.STREAM_CODEC, Entry::pattern,
            IotaType.TYPED_STREAM_CODEC, Entry::value,
            ::Entry
        )

        /** Codec used both for the iota type and for the image component. */
        @JvmField
        val CODEC: Codec<ImportsIota> = ENTRY_CODEC.listOf().xmap(
            { entries ->
                val map = LinkedHashMap<HexPattern, Iota>()
                for (entry in entries.take(MAX_IMPORTS)) map[entry.pattern] = entry.value
                ImportsIota(map)
            },
            { iota -> iota.imports.map { (pattern, value) -> Entry(pattern, value) } }
        )

        @JvmField
        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, ImportsIota> =
            ENTRY_STREAM_CODEC.apply(ByteBufCodecs.list()).map(
                { entries ->
                    val map = LinkedHashMap<HexPattern, Iota>()
                    for (entry in entries.take(MAX_IMPORTS)) map[entry.pattern] = entry.value
                    ImportsIota(map)
                },
                { iota -> iota.imports.map { (pattern, value) -> Entry(pattern, value) } }
            )

        /**
         * Per-cast storage for the imports. Marked transient so it does not leak across separate
         * staff patterns or slate jumps.
         */
        @JvmField
        val COMPONENT: ImageComponentType<ImportsIota> = ImageComponentType(CODEC, STREAM_CODEC, true)

        /**
         * Raw mode flag: while present and `true`, imports are bypassed and every pattern runs its
         * native action. Transient like [COMPONENT], since it is also per-cast state.
         */
        @JvmField
        val RAW_COMPONENT: ImageComponentType<Boolean> =
            ImageComponentType(Codec.BOOL, ByteBufCodecs.BOOL.cast(), true)
    }

    object Type : IotaType<ImportsIota>() {
        override fun codec(): MapCodec<ImportsIota> = CODEC.fieldOf("imports")

        override fun streamCodec(): StreamCodec<RegistryFriendlyByteBuf, ImportsIota> = STREAM_CODEC

        override fun color(): Int = 15631086
    }
}
