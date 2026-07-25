package cn.xm1221.MieHexRevolution.registry

import at.petrak.hexcasting.api.casting.eval.vm.ContinuationFrame
import at.petrak.hexcasting.common.lib.HexRegistries
import at.petrak.hexcasting.common.lib.hex.HexContinuationTypes
import cn.xm1221.MieHexRevolution.casting.frame.PushFrame
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceKey

object Miehex_revolutionFrames : Miehex_revolutionRegistrar<ContinuationFrame.Type<*>>(
    registryKey = HexRegistries.CONTINUATION_TYPE,
  getRegistry = { HexContinuationTypes.REGISTRY}
){
    val PUSH_FRAME = make("push", PushFrame.TYPE)

    private fun make(name: String, FrameType: ContinuationFrame.Type<*>) : ContinuationFrame.Type<*> {
        register(name) {
            FrameType
        }
        return FrameType
    }
}