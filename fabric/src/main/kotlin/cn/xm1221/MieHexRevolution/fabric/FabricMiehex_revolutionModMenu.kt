package cn.xm1221.MieHexRevolution.fabric

import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import cn.xm1221.MieHexRevolution.Miehex_revolutionClient

object FabricMiehex_revolutionModMenu : ModMenuApi {
    override fun getModConfigScreenFactory() = ConfigScreenFactory(Miehex_revolutionClient::getConfigScreen)
}
