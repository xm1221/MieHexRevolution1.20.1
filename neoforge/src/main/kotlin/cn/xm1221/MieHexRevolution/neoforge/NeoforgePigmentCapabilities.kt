package cn.xm1221.MieHexRevolution.neoforge

import at.petrak.hexcasting.api.item.IotaHolderItem
import at.petrak.hexcasting.api.item.PigmentItem
import at.petrak.hexcasting.forge.cap.HexCapabilities
import at.petrak.hexcasting.forge.cap.adimpl.CapStaticIotaHolder
import cn.xm1221.MieHexRevolution.pigment.PigmentColorExtractor
import net.minecraft.core.registries.BuiltInRegistries
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent

object NeoforgePigmentCapabilities {
    fun registerCaps(event: RegisterCapabilitiesEvent) {
        for (item in BuiltInRegistries.ITEM) {
            if (item is PigmentItem && item !is IotaHolderItem) {
                event.registerItem(
                    HexCapabilities.Item.IOTA,
                    { stack, _ ->
                        CapStaticIotaHolder(
                            { s -> PigmentColorExtractor.readColorIota(s, PigmentColorExtractor.currentCasterUuid()) },
                            stack,
                        )
                    },
                    item,
                )
            }
        }
    }
}
