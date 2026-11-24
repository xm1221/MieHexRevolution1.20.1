package cn.xm1221.MieHexRevolution.client

import cn.xm1221.MieHexRevolution.casting.sky.SkyTintZone
import cn.xm1221.MieHexRevolution.networking.msg.MsgClearSkyTintS2C
import cn.xm1221.MieHexRevolution.networking.msg.MsgSkyTintS2C
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.world.phys.Vec3
import java.util.concurrent.CopyOnWriteArrayList

data class SkyTintSample(val color: Vec3, val weight: Double)

/**
 * 客户端染色区列表。
 *
 * 区绑定到 [ClientLevel] 实例：换了实例（换维度/重进）整份作废，否则上个世界算出来的
 * `expireTick` 会变成永远清不掉的幽灵区。
 */
object MieHexRevolutionSkyTints {

    private const val MAX_ZONES = 32

    private val zones = CopyOnWriteArrayList<SkyTintZone>()

    private var ownedLevel: ClientLevel? = null

    fun add(msg: MsgSkyTintS2C) {
        val level = Minecraft.getInstance().level ?: return
        bindLevel(level)
        val zone = SkyTintZone(
            center = msg.center,
            radius = msg.radius,
            color = msg.color,
            dimension = msg.dimension,
            expireTick = level.gameTime + msg.durationTicks,
            strength = msg.strength,
        )
        zones.removeAll { it.sameAnchor(zone) }
        zones.add(zone)
        while (zones.size > MAX_ZONES) {
            zones.removeAt(0)
        }
    }

    fun clear(msg: MsgClearSkyTintS2C) {
        val level = Minecraft.getInstance().level ?: return
        bindLevel(level)
        if (msg.radius <= 0.0) {
            zones.removeAll { it.dimension == msg.dimension }
        } else {
            zones.removeAll { it.dimension == msg.dimension && it.intersects(msg.center, msg.radius) }
        }
    }

    fun clearAll() {
        zones.clear()
        ownedLevel = null
    }

    /** 多个区重叠时取权重最大的那个，不做叠加混色。 */
    fun sample(level: ClientLevel, pos: Vec3): SkyTintSample? {
        bindLevel(level)
        if (zones.isEmpty()) return null
        purge(level.gameTime)
        if (zones.isEmpty()) return null

        val dimension = level.dimension().location()
        var bestColor: Vec3? = null
        var bestWeight = 0.0
        // 倒序：权重相同时后加入的胜出
        for (index in zones.indices.reversed()) {
            val zone = zones[index]
            if (zone.dimension != dimension) continue
            val weight = zone.effectiveWeightAt(pos)
            if (weight <= 0.0) continue
            if (weight > bestWeight) {
                bestWeight = weight
                bestColor = zone.color
                if (weight >= 1.0) break
            }
        }
        val color = bestColor ?: return null
        return SkyTintSample(color, bestWeight)
    }

    private fun bindLevel(level: ClientLevel) {
        if (ownedLevel !== level) {
            zones.clear()
            ownedLevel = level
        }
    }

    private fun purge(gameTime: Long) {
        zones.removeAll { it.isExpired(gameTime) }
    }
}
