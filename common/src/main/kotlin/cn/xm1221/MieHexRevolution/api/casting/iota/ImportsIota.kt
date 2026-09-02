package cn.xm1221.MieHexRevolution.api.casting.iota

import at.petrak.hexcasting.api.casting.eval.CastResult
import at.petrak.hexcasting.api.casting.eval.ResolvedPatternType
import at.petrak.hexcasting.api.casting.eval.vm.CastingVM
import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation
import at.petrak.hexcasting.api.casting.iota.GarbageIota
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.IotaType
import at.petrak.hexcasting.api.casting.iota.PatternIota
import at.petrak.hexcasting.api.casting.math.HexPattern
import at.petrak.hexcasting.api.utils.putCompound
import at.petrak.hexcasting.common.lib.hex.HexEvalSounds
import net.minecraft.ChatFormatting
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.Style
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel

/**
 * A set of "imports": a mapping of pattern -> iota, used to bind spell patterns to values.
 *
 * When this iota is executed it is stored into the cast's [net.minecraft.nbt.CompoundTag] user data,
 * and any [PatternIota] executed later in the same cast whose pattern is a key here evaluates to the
 * bound value instead of performing its usual action (see `PatternIota.executeWithImports`).
 *
 * Keys are [HexPattern] (a data class with structural equality) rather than [PatternIota], because iotas
 * use reference equality and would never match after an NBT round-trip.
 */
 class ImportsIota(val imports: Map<HexPattern, Iota>) : Iota(Type, imports) {

    override fun isTruthy(): Boolean = true

    override  fun executable(): Boolean {
        return true
    }

    override fun toleratesOther(that: Iota?): Boolean =
        that is ImportsIota && this.imports == that.imports

    override fun serialize(): Tag {
        val entries = ListTag()
        for ((pattern, iota) in imports) {
            val entry = CompoundTag()
            entry.put("pattern", IotaType.serialize(PatternIota(pattern)))
            entry.put("value", IotaType.serialize(iota))
            entries.add(entry)
        }
        val tag = CompoundTag()
        tag.put("entries", entries)
        return tag
    }

    override fun getType(): IotaType<ImportsIota> = Type

    override fun size(): Int {
        var size = 0
        for (i in imports.values) {
            size += i.size()
        }
        size += size+1
        return size
    }

    override fun depth(): Int = 1 + (imports.values.maxOfOrNull { it.depth() } ?: 0)

    override fun subIotas(): Iterable<Iota> = imports.values

    override fun execute(vm: CastingVM?, world: ServerLevel?, continuation: SpellContinuation?): CastResult {
        if (vm == null || world == null || continuation == null) return super.execute(vm, world, continuation)
        val img = vm.image
        val data = img.userData
        data.putCompound("imports", IotaType.serialize(this))
        val newimg = img.copy(userData = data)
        return CastResult(
            cast = this,
            continuation = continuation,
            newData = newimg,
            sideEffects = listOf(),
            resolutionType = ResolvedPatternType.EVALUATED,
            sound = HexEvalSounds.HERMES
        )
    }

    object Type : IotaType<ImportsIota>() {
        override fun deserialize(
            tag: Tag?,
            world: ServerLevel?
        ): ImportsIota? {
            if (tag !is CompoundTag) return null
            val entries = tag.getList("entries", Tag.TAG_COMPOUND.toInt())
            val map = HashMap<HexPattern, Iota>()
            // Defensive cap: `entries.size` is trusted from NBT, which may be player-crafted.
            val count = minOf(entries.size, MAX_IMPORTS)
            for (index in 0 until count) {
                val entry = entries.getCompound(index)
                val patternIota = IotaType.deserialize(entry.getCompound("pattern"), world)
                val value = IotaType.deserialize(entry.getCompound("value"), world)
                if (patternIota !is PatternIota || value is GarbageIota) continue
                map[patternIota.pattern] = value
            }
            return ImportsIota(map)
        }

        override fun display(tag: Tag?): Component? {
            return try {
                // Main text carries the entry count so two import sets can be told apart at a glance,
                // while hovering shows up to MAX_DISPLAY_ENTRIES key/value pairs.
                val body = Component.literal("")
                val entries = (tag as? CompoundTag)?.getList("entries", Tag.TAG_COMPOUND.toInt()) ?: ListTag()
                val count = entries.size
                val shown = minOf(count, MAX_DISPLAY_ENTRIES)
                for (i in 0 until shown) {
                    if (i > 0) body.append("\n")
                    val entry = entries.getCompound(i)
                    body.append(IotaType.getDisplay(entry.getCompound("pattern")))
                    body.append("  →  ")
                    body.append(IotaType.getDisplay(entry.getCompound("value")))
                }
                if (count > shown) body.append("\n…(+${count - shown})")
                val style = Style.EMPTY
                    .withFont(ResourceLocation.tryParse("minecraft:illageralt"))
                    .withColor(ChatFormatting.LIGHT_PURPLE)
                    .withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, body))
                val res = Component.literal( "IMPORTS").withStyle(style)
                if(count>0){
                    res.append(Component.literal("x $count")).withStyle(ChatFormatting.LIGHT_PURPLE)
                }
                else res
            } catch (e: Exception) {
                // Never let a display issue crash the GUI; fall back to the plain label.
                Component.literal("IMPORTS").withStyle(ChatFormatting.LIGHT_PURPLE)
            }
        }

        override fun color(): Int = 15631086


    }

    companion object {
        /**
         * Upper bound on how many bindings will be read back from NBT, mirroring
         * `HexIotaTypes.MAX_SERIALIZATION_TOTAL` so a single import set cannot exceed the
         * serialization budget the rest of the ecosystem uses.
         */
        const val MAX_IMPORTS = 1024

        /** How many key/value pairs are shown in the hover tooltip before truncating. */
        const val MAX_DISPLAY_ENTRIES = 8
    }
}