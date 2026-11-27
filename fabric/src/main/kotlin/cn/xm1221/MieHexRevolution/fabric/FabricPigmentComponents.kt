package cn.xm1221.MieHexRevolution.fabric

import at.petrak.hexcasting.api.item.IotaHolderItem
import at.petrak.hexcasting.api.item.PigmentItem
import at.petrak.hexcasting.fabric.cc.HexCardinalComponents
import at.petrak.hexcasting.fabric.cc.adimpl.CCItemIotaHolder
import cn.xm1221.MieHexRevolution.pigment.PigmentColorExtractor
import dev.onyxstudios.cca.api.v3.item.ItemComponentFactoryRegistry
import dev.onyxstudios.cca.api.v3.item.ItemComponentInitializer

class FabricPigmentComponents : ItemComponentInitializer {
    override fun registerItemComponentFactories(registry: ItemComponentFactoryRegistry) {
        registry.register(
            { it is PigmentItem && it !is IotaHolderItem },
            HexCardinalComponents.IOTA_HOLDER,
        ) { stack ->
            CCItemIotaHolder.Static(stack) { s ->
                PigmentColorExtractor.readColorIota(s, PigmentColorExtractor.currentCasterUuid())
            }
        }
    }
}
