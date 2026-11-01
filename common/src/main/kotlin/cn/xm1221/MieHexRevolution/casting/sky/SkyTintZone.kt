package cn.xm1221.MieHexRevolution.casting.sky

import net.minecraft.resources.ResourceLocation
import net.minecraft.world.phys.Vec3

/**
 * 一个天空染色区：球心 [center]、半径 [radius]，镜头到球心距离决定权重。
 *
 * @param strength 强度 0..1，最终权重 = 几何权重 × 强度
 */
data class SkyTintZone(
    val center: Vec3,
    val radius: Double,
    val color: Vec3,
    val dimension: ResourceLocation,
    val expireTick: Long,
    val strength: Double = 1.0,
) {

    fun isExpired(gameTime: Long): Boolean = gameTime >= expireTick

    /** 纯几何权重：球内 1，球外 [FADE_RATIO] 倍半径内 smoothstep 淡出。 */
    fun weightAt(pos: Vec3): Double {
        if (radius <= 0.0) return 0.0
        val dist = center.distanceTo(pos)
        if (dist <= radius) return 1.0
        val fade = radius * FADE_RATIO
        if (fade <= EPSILON) return 0.0
        val t = 1.0 - (dist - radius) / fade
        if (t <= 0.0) return 0.0
        return t * t * (3.0 - 2.0 * t)
    }

    fun effectiveWeightAt(pos: Vec3): Double = weightAt(pos) * strength.coerceIn(0.0, 1.0)

    /** 同锚点视为同一个区：重复施法只刷新，不叠加。 */
    fun sameAnchor(other: SkyTintZone): Boolean =
        dimension == other.dimension &&
            radius == other.radius &&
            center.distanceToSqr(other.center) < ANCHOR_EPSILON

    fun intersects(otherCenter: Vec3, otherRadius: Double): Boolean {
        if (otherRadius <= 0.0) return false
        val sum = radius + otherRadius
        return center.distanceToSqr(otherCenter) <= sum * sum
    }

    companion object {
        const val FADE_RATIO = 0.6

        private const val EPSILON = 1.0e-6
        private const val ANCHOR_EPSILON = 1.0e-4

        const val MAX_RADIUS = 256.0
        const val MAX_DURATION_TICKS = 20 * 60 * 10

        /** 三分量都 ≤ 1 按 0..1 解释，否则按 0..255；负数夹到 0。 */
        fun normalizeColor(raw: Vec3): Vec3 {
            val peak = maxOf(kotlin.math.abs(raw.x), kotlin.math.abs(raw.y), kotlin.math.abs(raw.z))
            val scale = if (peak <= 1.0) 1.0 else 1.0 / 255.0
            return Vec3(
                (raw.x * scale).coerceIn(0.0, 1.0),
                (raw.y * scale).coerceIn(0.0, 1.0),
                (raw.z * scale).coerceIn(0.0, 1.0),
            )
        }
    }
}
