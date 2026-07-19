package cn.xm1221.MieHexRevolution.registry

import at.petrak.hexcasting.api.casting.ActionRegistryEntry
import at.petrak.hexcasting.api.casting.castables.Action
import at.petrak.hexcasting.api.casting.math.HexDir
import at.petrak.hexcasting.api.casting.math.HexPattern
import at.petrak.hexcasting.common.lib.HexRegistries
import at.petrak.hexcasting.common.lib.hex.HexActions
import cn.xm1221.MieHexRevolution.casting.actions.spells.OpUseBlock
import cn.xm1221.MieHexRevolution.casting.actions.spells.OpWorldLoader
import cn.xm1221.MieHexRevolution.casting.actions.spells.monitor.OpMonitorGet
import cn.xm1221.MieHexRevolution.casting.actions.spells.monitor.OpMonitorSet
import cn.xm1221.MieHexRevolution.casting.actions.useful.OpMax
import cn.xm1221.MieHexRevolution.casting.actions.useful.OpMin
import cn.xm1221.MieHexRevolution.casting.actions.useful.OpPages

object Miehex_revolutionActions : Miehex_revolutionRegistrar<ActionRegistryEntry>(
    HexRegistries.ACTION,
    { HexActions.REGISTRY },
) {
    //val CONGRATULATE = make("congratulate", HexDir.WEST, "eed", OpCongratulate)

    //val GREAT_CONGRATULATE = make("congratulate/great", HexDir.EAST, "qwwqqqwwqwded", OpCongratulate)
    val MAX_VALUE = make("const/max", HexDir.SOUTH_EAST,"eeee", OpMax())
    val MIN_VALUE = make("const/min", HexDir.SOUTH_WEST,"qqqq", OpMin())
    val PAGE = make("page", HexDir.WEST,"qqadad", OpPages())
    val LOAD = make("loader/time", HexDir.NORTH_EAST,"aawewewaqweedeewqawewe", OpWorldLoader.Load)
    val USE_BLOCK = make("use", HexDir.NORTH_EAST,"deewqqa", OpUseBlock())
    val CAMERA_SET = make("monitor/set", HexDir.NORTH_EAST,"wqqqqwqedwwde", OpMonitorSet())
    val CAMERA_GET = make("monitor/get",HexDir.NORTH_EAST,"qwawqwaqewdwewd", OpMonitorGet())

    private fun make(name: String, startDir: HexDir, signature: String, action: Action) =
        make(name, startDir, signature) { action }

    private fun make(name: String, startDir: HexDir, signature: String, getAction: () -> Action) = register(name) {
        ActionRegistryEntry(HexPattern.fromAngles(signature, startDir), getAction())
    }
}
