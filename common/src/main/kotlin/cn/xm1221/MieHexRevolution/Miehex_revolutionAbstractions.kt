@file:JvmName("Miehex_revolutionAbstractions")

package cn.xm1221.MieHexRevolution

import dev.architectury.injectables.annotations.ExpectPlatform
import cn.xm1221.MieHexRevolution.registry.Miehex_revolutionRegistrar

fun initRegistries(vararg registries: Miehex_revolutionRegistrar<*>) {
    for (registry in registries) {
        initRegistry(registry)
    }
}

@ExpectPlatform
fun <T : Any> initRegistry(registrar: Miehex_revolutionRegistrar<T>) {
    throw AssertionError()
}
