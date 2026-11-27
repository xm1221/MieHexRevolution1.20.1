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
import cn.xm1221.MieHexRevolution.registry.MieHexRevolutionImageComponents
import cn.xm1221.MieHexRevolution.registry.Miehex_revolutionActions
import cn.xm1221.MieHexRevolution.registry.Miehex_revolutionBlocks
import cn.xm1221.MieHexRevolution.registry.Miehex_revolutionBlockEntities
import cn.xm1221.MieHexRevolution.registry.MieHexRevolutionWorkers
import cn.xm1221.MieHexRevolution.registry.Miehex_revolutionFrames
import cn.xm1221.MieHexRevolution.util.PlayerRemoteCastData
import cn.xm1221.MieHexRevolution.util.RemoteCastDataKeys
import net.minecraft.core.Registry
import net.minecraft.core.registries.Registries
import net.minecraft.tags.TagKey
import net.minecraft.world.entity.EntityType

object Miehex_revolution {
    const val MODID = "miehex_revolution"
    val SUMMON_TAG: TagKey<EntityType<*>> by lazy {
        TagKey.create(
            Registries.ENTITY_TYPE,
            ResourceLocation.tryBuild(MODID, "monitor")
        )
    }

    // hexal entity type IDs for cross-mod soft compat
    private val HEXAL_WISP_TYPES = setOf(
        ResourceLocation.fromNamespaceAndPath("hexal", "wisp/ticking"),
        ResourceLocation.fromNamespaceAndPath("hexal", "wisp/projectile"),
        ResourceLocation.fromNamespaceAndPath("hexal", "wisp/wandering"),
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
    fun id(path: String) = ResourceLocation.fromNamespaceAndPath(MODID, path)

    fun init() {
        Miehex_revolutionServerConfig.init()
        initRegistries(
            Miehex_revolutionActions,
            Miehex_revolutionFrames,
            MieHexRevolutionIotas,
            MieHexRevolutionImageComponents
        )
        Miehex_revolutionBlocks.init()
        Miehex_revolutionBlockEntities.init()
        Miehex_revolutionNetworking.init()
        ImportsParseCompat.init()
        MieHexRevolutionWorkers.init()
        registerRemoteCastLifecycle()
    }

    /**
     * 远程施法的生命周期收尾。
     *
     * <p>三条路都要清：潜行右键法杖（直接退出远程施法）、退出游戏、重生 —— 后两条不清的话，
     * 目标实体的引用会一直留在表里，玩家下次上线还在“替别人施法”。
     *
     * <p>右键这里和 [cn.xm1221.MieHexRevolution.mixin.MixinItemStaff] 是重复的两道保险：
     * mixin 只包得住 Hex 自家的 [at.petrak.hexcasting.common.items.ItemStaff]，
     * 附属模组另加的法杖（也在 `hexcasting:staves` 标签里）只有这个事件拦得住。
     */
    private fun registerRemoteCastLifecycle() {
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
        PlayerEvent.PLAYER_RESPAWN.register { player, _, _ ->
            PlayerRemoteCastData.clear(player)
        }
    }

    fun initServer() {
        Miehex_revolutionServerConfig.initServer()
    }
}
