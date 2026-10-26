package cn.xm1221.MieHexRevolution.util

import cn.xm1221.MieHexRevolution.casting.env.ExecutionColorProvider
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player

/**
 * 玩家实体上同步到客户端的执行色数据（SynchedEntityData）。
 * 服务端在写入/清除远程施法数据时同步设置，客户端 GUI mixin 读取后替换默认执行蓝。
 *
 * accessor 由 [cn.xm1221.MieHexRevolution.mixin.MixinPlayerSyncedData] 在 Player 的
 * 静态初始化阶段注册，这里只通过 [RemoteCastColorHolder] 读写，**不要**再自行
 * defineId：1.21 的 SynchedEntityData.Builder 在构造时就按类固定了数组长度，
 * 事后注册的 id 会越界（Index 21 out of bounds for length 21）。
 */
object RemoteCastDataKeys {
    @JvmStatic
    fun getColor(player: Player): Int = (player as RemoteCastColorHolder).`miehex$getExecutionColor`()

    @JvmStatic
    fun setColor(player: Player, color: Int) {
        (player as RemoteCastColorHolder).`miehex$setExecutionColor`(color)
    }

    @JvmStatic
    fun reset(player: Player) {
        setColor(player, ExecutionColorProvider.DEFAULT)
    }

    /** 根据目标实体类型返回对应的执行色（玩家 → PLAYERS，生物 → FAKE） */
    @JvmStatic
    fun colorFor(target: Entity): Int = if (target is Player) {
        ExecutionColorProvider.PLAYERS_COLOR
    } else {
        ExecutionColorProvider.FAKE_COLOR
    }
}
