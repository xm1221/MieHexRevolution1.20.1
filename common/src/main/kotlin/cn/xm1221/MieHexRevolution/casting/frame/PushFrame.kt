package cn.xm1221.MieHexRevolution.casting.frame

import at.petrak.hexcasting.api.casting.eval.CastResult
import at.petrak.hexcasting.api.casting.eval.ResolvedPatternType
import at.petrak.hexcasting.api.casting.eval.vm.CastingVM
import at.petrak.hexcasting.api.casting.eval.vm.ContinuationFrame
import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.IotaType
import at.petrak.hexcasting.api.casting.iota.ListIota
import at.petrak.hexcasting.common.lib.hex.HexEvalSounds
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel

class PushFrame(val iotas: List<Iota>): ContinuationFrame {
    override val type: ContinuationFrame.Type<*>
        get() = TYPE

    override fun breakDownwards(stack: List<Iota>): Pair<Boolean, List<Iota>> {
        return false to stack
    }

    override fun evaluate(
        continuation: SpellContinuation,
        level: ServerLevel,
        harness: CastingVM
    ): CastResult {
        val stack = harness.image.stack.toMutableList()
        val newStack = iotas.toMutableList().plus(stack)
        val newimage = harness.image.copy(stack = newStack)
        return CastResult(
            cast = ListIota(iotas),
            continuation = continuation,
            newData = newimage,
            sideEffects = listOf(),
            resolutionType = ResolvedPatternType.EVALUATED,
            sound = HexEvalSounds.HERMES
        )
    }

    override fun serializeToNBT(): CompoundTag {
        return IotaType.serialize(ListIota(iotas))
    }

    override fun size(): Int {
        return 1
    }

    companion object {
        @JvmField
        val TYPE: ContinuationFrame.Type<PushFrame> = object : ContinuationFrame.Type<PushFrame> {
            override fun deserializeFromNBT(tag: CompoundTag, world: ServerLevel): PushFrame {
               val iota = IotaType.deserialize(tag,world) as ListIota
                return PushFrame(iota.list.toList())
            }

        }
    }
}