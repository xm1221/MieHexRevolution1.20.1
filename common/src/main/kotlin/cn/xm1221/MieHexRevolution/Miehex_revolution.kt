package cn.xm1221.MieHexRevolution

import at.petrak.hexcasting.api.mod.HexTags
import at.petrak.hexcasting.common.lib.HexAttributes
import at.petrak.hexcasting.common.lib.HexRegistries
import dev.architectury.event.CompoundEventResult
import dev.architectury.event.events.common.InteractionEvent
import dev.architectury.event.events.common.PlayerEvent
import dev.architectury.platform.Platform
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import cn.xm1221.MieHexRevolution.compat.parse.ImportsParseCompat
import cn.xm1221.MieHexRevolution.config.Miehex_revolutionServerConfig
import cn.xm1221.MieHexRevolution.networking.Miehex_revolutionNetworking
import cn.xm1221.MieHexRevolution.registry.MieHexRevolutionIotas
import cn.xm1221.MieHexRevolution.registry.MieHexRevolutionWorkers
import cn.xm1221.MieHexRevolution.registry.Miehex_revolutionActions
import cn.xm1221.MieHexRevolution.registry.Miehex_revolutionBlocks
import cn.xm1221.MieHexRevolution.registry.Miehex_revolutionBlockEntities
import cn.xm1221.MieHexRevolution.registry.Miehex_revolutionFrames
import cn.xm1221.MieHexRevolution.util.PlayerRemoteCastData
import cn.xm1221.MieHexRevolution.util.RemoteCastDataKeys
import net.minecraft.core.Registry
import net.minecraft.core.registries.Registries
import net.minecraft.tags.TagKey
import net.minecraft.world.entity.EntityType

object Miehex_revolution {
    const val MODID = "miehex_revolution"
    val SUMMON_TAG: TagKey<EntityType<*>> = TagKey.create(
        Registries.ENTITY_TYPE,
        ResourceLocation.tryBuild(MODID, "monitor")
    )

    // hexal entity type IDs for cross-mod soft compat
    private val HEXAL_WISP_TYPES = setOf(
        ResourceLocation("hexal", "wisp/ticking"),
        ResourceLocation("hexal", "wisp/projectile"),
        ResourceLocation("hexal", "wisp/wandering"),
    )

    fun isMonitorEntity(type: EntityType<*>): Boolean {
        if (type.`is`(SUMMON_TAG)) return true
        if (Platform.isModLoaded("hexal")) {
            val key = BuiltInRegistries.ENTITY_TYPE.getKey(type)
            if (key in HEXAL_WISP_TYPES) return true
        }
        return false
    }

    @JvmField
    val LOGGER: Logger = LogManager.getLogger(MODID)

    @JvmStatic
    fun id(path: String) = ResourceLocation(MODID, path)

    fun init() {
        Miehex_revolutionServerConfig.init()
        initRegistries(
            Miehex_revolutionActions,
            Miehex_revolutionFrames,
            MieHexRevolutionIotas,
        )
        Miehex_revolutionBlocks.init()
        Miehex_revolutionBlockEntities.init()
        Miehex_revolutionNetworking.init()
        ImportsParseCompat.init()
        MieHexRevolutionWorkers.init()
        InteractionEvent.RIGHT_CLICK_ITEM.register { player, hand ->
            if (player.isShiftKeyDown && player.getItemInHand(hand).`is`(HexTags.Items.STAVES)
                && player.getAttributeValue(HexAttributes.FEEBLE_MIND) <= 0.0
            ) {
                if (player.level().isClientSide) {
                    RemoteCastDataKeys.reset(player)
                } else if (player is ServerPlayer) {
                    PlayerRemoteCastData.clear(player)
                }
            }
            CompoundEventResult.pass()
        }
        PlayerEvent.PLAYER_QUIT.register { player ->
            PlayerRemoteCastData.clear(player)
        }
        PlayerEvent.PLAYER_RESPAWN.register { player, _ ->
            PlayerRemoteCastData.clear(player)
        }
    }




    fun initServer() {
        Miehex_revolutionServerConfig.initServer()
    }
}
