package cn.xm1221.MieHexRevolution

import cn.xm1221.MieHexRevolution.config.Miehex_revolutionClientConfig
import me.shedaniel.autoconfig.AutoConfig
import net.minecraft.client.gui.screens.Screen

object Miehex_revolutionClient {
    fun init() {
        Miehex_revolutionClientConfig.init()
    }

    fun getConfigScreen(parent: Screen): Screen {
        return AutoConfig.getConfigScreen(Miehex_revolutionClientConfig.GlobalConfig::class.java, parent).get()
    }
}
