package cn.xm1221.MieHexRevolution.registry

import at.petrak.hexcasting.api.casting.eval.vm.ImageComponentType
import at.petrak.hexcasting.common.lib.HexRegistries
import at.petrak.hexcasting.common.lib.hex.HexImageComponents
import cn.xm1221.MieHexRevolution.api.casting.iota.ImportsIota

/**
 * Casting-image components used by this addon.
 *
 * Hex 0.12 requires every [ImageComponentType] stored in a [at.petrak.hexcasting.api.casting.eval.vm.CastingImage]
 * to be registered: the image codec dispatches on the component's registry name, and
 * `toString()` of an unregistered component throws.
 */
object MieHexRevolutionImageComponents : Miehex_revolutionRegistrar<ImageComponentType<*>>(
    HexRegistries.IMAGE_COMPONENT,
    { HexImageComponents.REGISTRY },
) {
    val IMPORTS = make("imports", ImportsIota.COMPONENT)
    val IMPORTS_RAW = make("imports_raw", ImportsIota.RAW_COMPONENT)

    private fun make(name: String, type: ImageComponentType<*>) = register(name) { type }
}
