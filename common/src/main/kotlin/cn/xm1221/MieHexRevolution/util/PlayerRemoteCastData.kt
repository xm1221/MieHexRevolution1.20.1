package cn.xm1221.MieHexRevolution.util

import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.LivingEntity
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 存储玩家实体上的"施法目标实体"数据（即要代替其施法的实体）。
 * 玩家持有该数据时，GUI 施法从 StaffCastEnv 切换为：
 * - 目标是玩家 → PlayersCastingEnv（代替好友施法）
 * - 目标是其他生物 → FakeCastingEnv（假借该生物之身施法）
 * 同时向客户端同步对应的执行色（见 RemoteCastDataKeys）。
 *
 * 键用 UUID 而不是 [ServerPlayer] 实例：玩家重登会换一个新的实体对象（旧对象留在表里
 * 就是永久泄漏，而且新对象查不到旧数据），UUID 跨会话稳定。
 */
object PlayerRemoteCastData {
    private val targets = ConcurrentHashMap<UUID, LivingEntity>()

    /** 写入该玩家的施法目标实体（并同步对应执行色到客户端）；目标为自身时视为解除 */
    @JvmStatic
    fun set(player: ServerPlayer, target: LivingEntity) {
        if (target == player) {
            clear(player)
            return
        }
        targets[player.uuid] = target
        RemoteCastDataKeys.setColor(player, RemoteCastDataKeys.colorFor(target))
    }

    /** 读取该玩家的施法目标实体，无或已失效则清除并返回 null */
    @JvmStatic
    fun get(player: ServerPlayer): LivingEntity? {
        val target = targets[player.uuid] ?: return null
        // 目标死掉 / 被移除 / 下线之后不能再拿它施法，否则会对着空气改方块。
        if (target == player || !target.isAlive || target.isRemoved || (target is ServerPlayer && target.hasDisconnected())) {
            clear(player)
            return null
        }
        return target
    }

    /** 玩家是否持有施法目标实体 */
    @JvmStatic
    fun has(player: ServerPlayer): Boolean = get(player) != null

    /** 清除该玩家的施法目标实体（并把执行色重置为默认） */
    @JvmStatic
    fun clear(player: ServerPlayer) {
        targets.remove(player.uuid)
        RemoteCastDataKeys.reset(player)
    }
}
