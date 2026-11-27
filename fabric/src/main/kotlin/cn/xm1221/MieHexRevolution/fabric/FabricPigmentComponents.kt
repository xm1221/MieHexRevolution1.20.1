package cn.xm1221.MieHexRevolution.fabric

import at.petrak.hexcasting.api.item.IotaHolderItem
import at.petrak.hexcasting.api.item.PigmentItem
import at.petrak.hexcasting.fabric.cc.HexCardinalComponents
import at.petrak.hexcasting.fabric.cc.adimpl.CCItemIotaHolder
import cn.xm1221.MieHexRevolution.item.ItemCustomPigment
import cn.xm1221.MieHexRevolution.pigment.PigmentColorExtractor

object FabricPigmentComponents {
    fun register() {
        HexCardinalComponents.IOTA_HOLDER_LOOKUP.registerFallback { stack, _ ->
            val item = stack.item
            when {
                item is ItemCustomPigment -> CCItemIotaHolder.ItemBased(stack)
                item is PigmentItem && item !is IotaHolderItem -> CCItemIotaHolder.Static(stack) { s ->
                    PigmentColorExtractor.readColorIota(s, PigmentColorExtractor.currentCasterUuid())
                }
                else -> null
            }
        }
    }
}
