@file:JvmName("Miehex_revolutionAbstractionsImpl")

package cn.xm1221.MieHexRevolution.forge

import cn.xm1221.MieHexRevolution.registry.Miehex_revolutionRegistrar
import net.minecraftforge.registries.RegisterEvent
import thedarkcolour.kotlinforforge.forge.MOD_BUS

fun <T : Any> initRegistry(registrar: Miehex_revolutionRegistrar<T>) {
    MOD_BUS.addListener { event: RegisterEvent ->
        event.register(registrar.registryKey) { helper ->
            registrar.init(helper::register)
        }
    }
}
