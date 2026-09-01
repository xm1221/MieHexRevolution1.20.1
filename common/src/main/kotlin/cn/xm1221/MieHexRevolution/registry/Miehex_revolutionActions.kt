package cn.xm1221.MieHexRevolution.registry

import at.petrak.hexcasting.api.casting.ActionRegistryEntry
import at.petrak.hexcasting.api.casting.castables.Action
import at.petrak.hexcasting.api.casting.math.HexDir
import at.petrak.hexcasting.api.casting.math.HexPattern
import at.petrak.hexcasting.common.lib.HexRegistries
import at.petrak.hexcasting.common.lib.hex.HexActions
import cn.xm1221.MieHexRevolution.casting.actions.imports.OpImportsBind
import cn.xm1221.MieHexRevolution.casting.actions.imports.OpImportsBindDirect
import cn.xm1221.MieHexRevolution.casting.actions.imports.OpImportsCreate
import cn.xm1221.MieHexRevolution.casting.actions.imports.OpRawPattern
import cn.xm1221.MieHexRevolution.casting.actions.spells.OpEntityInteract
import cn.xm1221.MieHexRevolution.casting.actions.spells.psy.OpEntityMoveStep
import cn.xm1221.MieHexRevolution.casting.actions.spells.psy.OpEntityMoveTo
import cn.xm1221.MieHexRevolution.casting.actions.spells.psy.OpEntityLastHurtGet
import cn.xm1221.MieHexRevolution.casting.actions.spells.psy.OpMobTargetGet
import cn.xm1221.MieHexRevolution.casting.actions.spells.psy.OpMobTargetSet
import cn.xm1221.MieHexRevolution.casting.actions.spells.OpUseBlock
import cn.xm1221.MieHexRevolution.casting.actions.spells.OpUseItemOn
import cn.xm1221.MieHexRevolution.casting.actions.spells.OpUseWithItemEntity
import cn.xm1221.MieHexRevolution.casting.actions.spells.OpWorldLoader
import cn.xm1221.MieHexRevolution.casting.actions.spells.circle.OpCircleLength
import cn.xm1221.MieHexRevolution.casting.actions.spells.circle.OpCircleLengthUsed
import cn.xm1221.MieHexRevolution.casting.actions.spells.idea.OpIdeaBlockSet
import cn.xm1221.MieHexRevolution.casting.actions.spells.idea.OpIdeaShardWrite
import cn.xm1221.MieHexRevolution.casting.actions.spells.monitor.OpMonitorGet
import cn.xm1221.MieHexRevolution.casting.actions.spells.monitor.OpMonitorSet
import cn.xm1221.MieHexRevolution.casting.actions.spells.psy.OpEntityRot
import cn.xm1221.MieHexRevolution.casting.actions.useful.OpMax
import cn.xm1221.MieHexRevolution.casting.actions.useful.OpMin
import cn.xm1221.MieHexRevolution.casting.actions.useful.OpPages
import cn.xm1221.MieHexRevolution.casting.actions.useful.envs.OpFaker
import cn.xm1221.MieHexRevolution.casting.actions.useful.list.OpEvalInList
import cn.xm1221.MieHexRevolution.casting.actions.useful.list.OpIndexes

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
    val USE_ITEM_ON = make("use_on",HexDir.NORTH_EAST,"aqqweed", OpUseItemOn())
    val USE_ON_WITH = make("use_on/with", HexDir.WEST,"daqqqeeeda", OpUseWithItemEntity())
    val CAMERA_SET = make("monitor/set", HexDir.NORTH_EAST,"wqqqqwqedwwde", OpMonitorSet())
    val CAMERA_GET = make("monitor/get",HexDir.NORTH_EAST,"qwawqwaqewdwewd", OpMonitorGet())
    val IDEA_SET = make("idea/set", HexDir.EAST,"wqaqqqqqeawqwqwqwqwqw", OpIdeaBlockSet())
    val EVAL_IN = make("eval_in", HexDir.SOUTH_EAST,"wdwewawqwqw", OpEvalInList())
    val INDEXES = make("indexes", HexDir.EAST,"qqqqqdeee", OpIndexes())

    val ENTITY_MOVE= make("entity/move",HexDir.EAST,"deeedawedeeee", OpEntityMoveTo())
    val ENTITY_MOVE_STEP = make("entity/step",HexDir.SOUTH_WEST,"qqqqaqwqaawdd", OpEntityMoveStep())
    val ENTITY_LAST_HURT = make("entity/lasthurt",HexDir.EAST,"wadwdqdwd", OpEntityLastHurtGet())
    val MOB_TARGET_GET = make("entity/target/get",HexDir.EAST,"qqqaqww", OpMobTargetGet())
    val MOB_TARGET_SET = make("entity/target/set",HexDir.EAST,"addqaqwawqeawa", OpMobTargetSet())
    val ENTITY_ROT = make("entity/rot",HexDir.SOUTH_EAST,"qqqqaqwdwd", OpEntityRot())
    val ENTITY_IN = make("interact", HexDir.SOUTH_WEST,"dwqqwqwqwaweaqqqqedwqqwqwqwaqeeedqaedwqqwqwqwaweaqqqq",
        OpEntityInteract())

    val IDEA_WRITE = make("idea_shard/write", HexDir.EAST,"waqqqq", OpIdeaShardWrite())

    val CIRCLE_LENGTH = make("circle/length",HexDir.SOUTH_WEST,"eaqdqaqdqae",OpCircleLength())
    val CIRCLE_USED = make("circle/used",HexDir.SOUTH_EAST,"qdeqqaqqedq", OpCircleLengthUsed())

    val CHANGE_ENV = make("changeenv",HexDir.EAST,"edwaq", OpFaker())

    // 导入（imports）系统：图案笔顺留空，待补充（TODO 笔顺）
    val IMPORTS_CREATE = make("imports/create", HexDir.WEST, "qqaedwaqdee", OpImportsCreate())
    val IMPORTS_BIND = make("imports/bind", HexDir.NORTH_EAST, "aqdeeqqaed", OpImportsBind())
    val IMPORTS_BIND_DIRECT = make("imports/bind_direct", HexDir.SOUTH_EAST, "aqdeeqawqqeqqwqqeq", OpImportsBindDirect())
    val RAW_PATTERN = make("raw/pattern", HexDir.WEST, "wqqqwaqe", OpRawPattern())
    private fun make(name: String, startDir: HexDir, signature: String, action: Action) =
        make(name, startDir, signature) { action }

    private fun make(name: String, startDir: HexDir, signature: String, getAction: () -> Action) = register(name) {
        ActionRegistryEntry(HexPattern.fromAngles(signature, startDir), getAction())
    }
}
