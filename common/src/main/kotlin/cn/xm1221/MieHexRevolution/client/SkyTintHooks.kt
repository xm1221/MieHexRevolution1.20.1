package cn.xm1221.MieHexRevolution.client

import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.world.phys.Vec3

/** `MixinClientLevelSkyColor` 的落点，把原色和染色区混一次。 */
object SkyTintHooks {

    @JvmStatic
    fun tintSkyColor(level: ClientLevel, original: Vec3, cameraPos: Vec3): Vec3 {
        val sample = MieHexRevolutionSkyTints.sample(level, cameraPos) ?: return original
        return blend(original, sample.color, sample.weight)
    }

    /** `getCloudColor` 没有位置参数，只能向主相机要。 */
    @JvmStatic
    fun tintCloudColor(level: ClientLevel, original: Vec3): Vec3 {
        val camera = Minecraft.getInstance().gameRenderer.mainCamera.position
        val sample = MieHexRevolutionSkyTints.sample(level, camera) ?: return original
        return blend(original, sample.color, sample.weight)
    }

    private fun blend(base: Vec3, tint: Vec3, weight: Double): Vec3 {
        val w = weight.coerceIn(0.0, 1.0)
        if (w <= 0.0) return base
        return Vec3(
            base.x + (tint.x - base.x) * w,
            base.y + (tint.y - base.y) * w,
            base.z + (tint.z - base.z) * w,
        )
    }
}
