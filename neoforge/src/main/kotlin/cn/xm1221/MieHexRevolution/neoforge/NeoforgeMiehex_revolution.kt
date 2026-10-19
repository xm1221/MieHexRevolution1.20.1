package cn.xm1221.MieHexRevolution.neoforge

import cn.xm1221.MieHexRevolution.Miehex_revolution
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModContainer
import net.neoforged.fml.common.Mod

@Mod(Miehex_revolution.MODID)
class NeoforgeMiehex_revolution(modBus: IEventBus, container: ModContainer) {
    init {
        setModEventBus(modBus)
        Miehex_revolution.init()
    }
}
