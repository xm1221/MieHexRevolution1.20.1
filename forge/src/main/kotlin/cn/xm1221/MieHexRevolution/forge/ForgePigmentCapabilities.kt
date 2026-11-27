package cn.xm1221.MieHexRevolution.forge

import at.petrak.hexcasting.api.item.IotaHolderItem
import at.petrak.hexcasting.api.item.PigmentItem
import at.petrak.hexcasting.forge.cap.ForgeCapabilityHandler
import at.petrak.hexcasting.forge.cap.HexCapabilities
import at.petrak.hexcasting.forge.cap.adimpl.CapStaticIotaHolder
import cn.xm1221.MieHexRevolution.Miehex_revolution
import cn.xm1221.MieHexRevolution.pigment.PigmentColorExtractor
import net.minecraft.world.item.ItemStack
import net.minecraftforge.event.AttachCapabilitiesEvent

object ForgePigmentCapabilities {
    private val CAP_ID = Miehex_revolution.id("pigment_iota")

    fun attachItemCaps(event: AttachCapabilitiesEvent<ItemStack>) {
        val stack = event.`object`
        val item = stack.item
        if (item is PigmentItem && item !is IotaHolderItem) {
            event.addCapability(
                CAP_ID,
                ForgeCapabilityHandler.provide(stack, HexCapabilities.IOTA) {
                    CapStaticIotaHolder(
                        { s -> PigmentColorExtractor.readColorIota(s, PigmentColorExtractor.currentCasterUuid()) },
                        stack,
                    )
                },
            )
        }
    }
}
