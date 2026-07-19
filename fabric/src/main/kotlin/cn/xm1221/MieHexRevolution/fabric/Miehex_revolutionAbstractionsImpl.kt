@file:JvmName("Miehex_revolutionAbstractionsImpl")

package cn.xm1221.MieHexRevolution.fabric

import cn.xm1221.MieHexRevolution.registry.Miehex_revolutionRegistrar
import net.minecraft.core.Registry

fun <T : Any> initRegistry(registrar: Miehex_revolutionRegistrar<T>) {
    val registry = registrar.registry
    registrar.init { id, value -> Registry.register(registry, id, value) }
}
