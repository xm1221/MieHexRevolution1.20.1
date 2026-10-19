package cn.xm1221.MieHexRevolution.casting.frame

import at.petrak.hexcasting.api.casting.eval.CastResult
import at.petrak.hexcasting.api.casting.eval.ResolvedPatternType
import at.petrak.hexcasting.api.casting.eval.vm.CastingVM
import at.petrak.hexcasting.api.casting.eval.vm.ContinuationFrame
import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.IotaType
import at.petrak.hexcasting.api.casting.iota.ListIota
import at.petrak.hexcasting.api.utils.TreeList
import at.petrak.hexcasting.common.lib.hex.HexEvalSounds
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.server.level.ServerLevel

/**
 * Pushes [iotas] onto the top of the casting stack when evaluated.
 */
class PushFrame(val iotas: List<Iota>) : ContinuationFrame {
    override val type: ContinuationFrame.Type<*>
        get() = TYPE

    override fun breakDownwards(stack: TreeList<Iota>): Pair<Boolean, TreeList<Iota>> =
        false to stack

    override fun evaluate(
        continuation: SpellContinuation,
        level: ServerLevel,
        harness: CastingVM
    ): CastResult {
        val newStack = TreeList.from(iotas).appendedAll(harness.image.stack)
        val newimage = harness.image.copy(stack = newStack)
        return CastResult(
            cast = ListIota(iotas),
            continuation = continuation,
            newData = newimage,
            sideEffects = listOf(),
            resolutionType = ResolvedPatternType.EVALUATED,
            sound = HexEvalSounds.HERMES.get()
        )
    }

    override fun size(): Int = 1

    companion object {
        private val LIST_CODEC = ListIota.TYPE.codec()

        @JvmField
        val TYPE: ContinuationFrame.Type<PushFrame> = object : ContinuationFrame.Type<PushFrame> {
            override fun codec(): MapCodec<PushFrame> =
                LIST_CODEC.xmap(
                    { listIota -> PushFrame(listIota.list.toList()) },
                    { frame -> ListIota(frame.iotas) }
                )

            override fun streamCodec(): StreamCodec<RegistryFriendlyByteBuf, PushFrame> =
                ListIota.TYPE.streamCodec().map(
                    { listIota -> PushFrame(listIota.list.toList()) },
                    { frame -> ListIota(frame.iotas) }
                )
        }
    }
}
