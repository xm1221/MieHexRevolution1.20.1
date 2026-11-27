package cn.xm1221.MieHexRevolution.forge

import dev.architectury.platform.forge.EventBuses
import cn.xm1221.MieHexRevolution.Miehex_revolution
import cn.xm1221.MieHexRevolution.forge.datagen.ForgeMiehex_revolutionDatagen
import net.minecraft.world.item.ItemStack
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.Mod
import thedarkcolour.kotlinforforge.forge.MOD_BUS

@Mod(Miehex_revolution.MODID)
class ForgeMiehex_revolution {
    init {
        MOD_BUS.apply {
            EventBuses.registerModEventBus(Miehex_revolution.MODID, this)
            addListener(ForgeMiehex_revolutionClient::init)
            addListener(ForgeMiehex_revolutionDatagen::init)
            addListener(ForgeMiehex_revolutionServer::init)
            register(ForgeMiehex_revolutionClient)
        }
        MinecraftForge.EVENT_BUS.addGenericListener(ItemStack::class.java, ForgePigmentCapabilities::attachItemCaps)
        Miehex_revolution.init()
    }
}
