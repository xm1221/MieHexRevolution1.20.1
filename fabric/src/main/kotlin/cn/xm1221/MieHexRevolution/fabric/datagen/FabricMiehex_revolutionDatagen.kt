package cn.xm1221.MieHexRevolution.fabric.datagen

import cn.xm1221.MieHexRevolution.datagen.Miehex_revolutionActionTags
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator

object FabricMiehex_revolutionDatagen : DataGeneratorEntrypoint {
    override fun onInitializeDataGenerator(gen: FabricDataGenerator) {
        val pack = gen.createPack()

        pack.addProvider(::Miehex_revolutionActionTags)
    }
}
