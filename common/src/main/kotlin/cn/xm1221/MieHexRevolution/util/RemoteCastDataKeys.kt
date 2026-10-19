package cn.xm1221.MieHexRevolution.util

import at.petrak.hexcasting.api.HexAPI
import cn.xm1221.MieHexRevolution.casting.env.ExecutionColorProvider
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player

/**
 * 玩家实体上同步到客户端的执行色数据（SynchedEntityData）。
 * 服务端在写入/清除远程施法数据时同步设置，客户端 GUI mixin 读取后替换默认执行蓝。
 */
object RemoteCastDataKeys {
    /** 执行色（ARGB），0 = 默认执行蓝 */
    @JvmField
    val EXECUTION_COLOR: EntityDataAccessor<Int> =
        SynchedEntityData.defineId(Player::class.java, EntityDataSerializers.INT)

    @JvmStatic
    fun getColor(entity: Entity): Int = entity.entityData.get(EXECUTION_COLOR)

    @JvmStatic
    fun setColor(entity: Entity, color: Int) {
        entity.entityData.set(EXECUTION_COLOR, color)
    }

    @JvmStatic
    fun reset(entity: Entity) {
        entity.entityData.set(EXECUTION_COLOR, ExecutionColorProvider.DEFAULT)
    }

    /** 根据目标实体类型返回对应的执行色（玩家 → PLAYERS，生物 → FAKE） */
    @JvmStatic
    fun colorFor(target: Entity): Int = if (target is Player) {
        ExecutionColorProvider.PLAYERS_COLOR
    } else {
        ExecutionColorProvider.FAKE_COLOR
    }
}
