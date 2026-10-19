@file:JvmName("Miehex_revolutionAbstractionsImpl")

package cn.xm1221.MieHexRevolution.neoforge

import cn.xm1221.MieHexRevolution.Miehex_revolution
import cn.xm1221.MieHexRevolution.registry.Miehex_revolutionRegistrar
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredRegister

private lateinit var modEventBus: IEventBus

/** Called from the mod constructor, before any registry is initialized. */
fun setModEventBus(bus: IEventBus) {
    modEventBus = bus
}

/**
 * NeoForge implementation of the common `@ExpectPlatform initRegistry`.
 *
 * Architectury resolves `initRegistry` to this class by the
 * `<common package>.<platform>.<file>Impl` naming convention. Each registrar gets its own
 * [DeferredRegister] on our own mod event bus, so entries keep the `miehex_revolution`
 * namespace (Hex's own `createRegistar` helper hardcodes the `hexcasting` namespace).
 */
fun <T : Any> initRegistry(registrar: Miehex_revolutionRegistrar<T>) {
    val deferred = DeferredRegister.create<T>(registrar.registryKey, Miehex_revolution.MODID)
    registrar.init { id, value ->
        deferred.register(id.path, java.util.function.Supplier { value })
    }
    deferred.register(modEventBus)
}
