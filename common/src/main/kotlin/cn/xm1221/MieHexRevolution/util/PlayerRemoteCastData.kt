package cn.xm1221.MieHexRevolution.util

import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.LivingEntity
import java.util.concurrent.ConcurrentHashMap

/**
 * 存储玩家实体上的"施法目标实体"数据（即要代替其施法的实体）。
 * 玩家持有该数据时，GUI 施法从 StaffCastEnv 切换为：
 * - 目标是玩家 → PlayersCastingEnv（代替好友施法）
 * - 目标是其他生物 → FakeCastingEnv（假借该生物之身施法）
 * 同时向客户端同步对应的执行色（见 RemoteCastDataKeys）。
 */
object PlayerRemoteCastData {
    private val targets = ConcurrentHashMap<ServerPlayer, LivingEntity>()

    /** 写入该玩家的施法目标实体（并同步对应执行色到客户端） */
    @JvmStatic
    fun set(player: ServerPlayer, target: LivingEntity) {
        targets[player] = target
        RemoteCastDataKeys.setColor(player, RemoteCastDataKeys.colorFor(target))
    }

    /** 读取该玩家的施法目标实体，无则返回 null */
    @JvmStatic
    fun get(player: ServerPlayer): LivingEntity? = targets[player]

    /** 玩家是否持有施法目标实体 */
    @JvmStatic
    fun has(player: ServerPlayer): Boolean = targets.containsKey(player)

    /** 清除该玩家的施法目标实体（并把执行色重置为默认） */
    @JvmStatic
    fun clear(player: ServerPlayer) {
        targets.remove(player)
        RemoteCastDataKeys.reset(player)
    }
}
