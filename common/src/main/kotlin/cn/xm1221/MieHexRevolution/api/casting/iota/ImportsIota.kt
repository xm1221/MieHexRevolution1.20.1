package cn.xm1221.MieHexRevolution.api.casting.iota

import at.petrak.hexcasting.api.casting.eval.CastResult
import at.petrak.hexcasting.api.casting.eval.ResolvedPatternType
import at.petrak.hexcasting.api.casting.eval.vm.CastingVM
import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.IotaType
import at.petrak.hexcasting.api.casting.iota.PatternIota
import at.petrak.hexcasting.api.utils.putCompound
import at.petrak.hexcasting.common.lib.hex.HexEvalSounds
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.Font
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel

class ImportsIota(val imports : Map<PatternIota, Iota>) : Iota(Type, imports) {

    val keys = imports.keys

    val funs = imports.values

    override fun isTruthy(): Boolean {
        return true
    }

    override fun toleratesOther(that: Iota?): Boolean {
        return that is ImportsIota && this.imports==that.imports
    }

    override fun serialize(): Tag {
        val tag = CompoundTag()
        for ((key, iota) in imports) {
            var subtag = CompoundTag()
            subtag.putCompound("pattern", IotaType.serialize(key))
            subtag.putCompound("values",IotaType.serialize(iota))
            tag.putCompound(imports.keys.indexOf(key).toString(),subtag)
        }
        return tag
    }

    override fun getType(): IotaType<ImportsIota> {
        return Type
    }

    override fun size(): Int {
        return keys.size
    }

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

    object Type: IotaType<ImportsIota>() {
        override fun deserialize(
            tag: Tag?,
            world: ServerLevel?
        ): ImportsIota? {
            if (tag !is CompoundTag) return null
            val range = IntRange(0,1024)
            val map = HashMap<PatternIota, Iota>()
            for (index in range) {
                val subtag = tag.getCompound(index.toString()) ?: break
                val key = subtag.getCompound("pattern") ?: continue
                val value = subtag.getCompound("values") ?: continue
                val patternIota = IotaType.deserialize(key,world)
                val iota = IotaType.deserialize(value,world)
                if(patternIota!is PatternIota)continue
                map[patternIota] = iota
            }
           val res = ImportsIota(map)
            if (res.keys.size != res.funs.size) return null
            return res
        }

        override fun display(tag: Tag?): Component? {
            val style = Style.EMPTY
            style.withFont(ResourceLocation.tryParse("minecraft:illageralt"))
            return Component.literal("[IMPORTS]").withStyle(style).withStyle(ChatFormatting.LIGHT_PURPLE)
        }

        override fun color(): Int {
            return 	15631086
        }

    }
}