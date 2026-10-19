@file:JvmName("Miehex_revolutionAbstractionsImpl")

package cn.xm1221.MieHexRevolution.fabric

import cn.xm1221.MieHexRevolution.registry.Miehex_revolutionRegistrar
import net.minecraft.core.Registry

/**
 * Fabric implementation of the common `@ExpectPlatform initRegistry`.
 *
 * Architectury resolves `initRegistry` to this class by the
 * `<common package>.<platform>.<file>Impl` naming convention. Fabric has no mod event bus:
 * entries are written straight into the (frozen-free during mod init) registry, exactly like
 * Hex's own Fabric `IXplatRegister`.
 */
fun <T : Any> initRegistry(registrar: Miehex_revolutionRegistrar<T>) {
    val registry = registrar.registry
    registrar.init { id, value -> Registry.register(registry, id, value) }
}
