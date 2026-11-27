package cn.xm1221.MieHexRevolution.client.pigment

import cn.xm1221.MieHexRevolution.pigment.PigmentData
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.block.model.BakedQuad
import net.minecraft.client.resources.model.BakedModel
import net.minecraft.core.Direction
import net.minecraft.util.RandomSource
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import java.util.IdentityHashMap

/**
 * Renders [cn.xm1221.MieHexRevolution.item.ItemCustomPigment] with its runtime-baked
 * [net.minecraft.client.renderer.texture.DynamicTexture] while preserving the extruded item quads.
 */
object CustomPigmentRenderer {

    private const val VERTEX_STRIDE = 8
    private const val QUAD_SEED = 42L

    private val quadCache = IdentityHashMap<BakedModel, List<BakedQuad>>()

    fun clearModelCache() {
        quadCache.clear()
    }

    fun render(
        stack: ItemStack,
        displayContext: ItemDisplayContext,
        leftHand: Boolean,
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        combinedLight: Int,
        combinedOverlay: Int,
        model: BakedModel,
    ): Boolean {
        if (!PigmentData.hasPigmentData(stack)) return false
        val quads = getRemappedQuads(model)
        if (quads.isEmpty()) return false

        val cached = CustomPigmentTextures.getOrCreate(PigmentData.read(stack))

        poseStack.pushPose()
        model.transforms.getTransform(displayContext).apply(leftHand, poseStack)
        poseStack.translate(-0.5, -0.5, -0.5)
        val pose = poseStack.last()
        val consumer = buffer.getBuffer(cached.renderType)
        for (quad in quads) {
            consumer.putBulkData(pose, quad, 1f, 1f, 1f, combinedLight, combinedOverlay)
        }
        poseStack.popPose()
        return true
    }

    private fun getRemappedQuads(model: BakedModel): List<BakedQuad> {
        quadCache[model]?.let { return it }

        val random = RandomSource.create(QUAD_SEED)
        val sourceQuads = ArrayList<BakedQuad>()
        for (direction in Direction.values()) {
            random.setSeed(QUAD_SEED)
            sourceQuads.addAll(model.getQuads(null, direction, random))
        }
        random.setSeed(QUAD_SEED)
        sourceQuads.addAll(model.getQuads(null, null, random))

        val remapped = sourceQuads.map(::remapQuadToUnitUv)
        quadCache[model] = remapped
        return remapped
    }

    private fun remapQuadToUnitUv(quad: BakedQuad): BakedQuad {
        val sprite = quad.sprite
        val u0 = sprite.u0
        val uSpan = sprite.u1 - u0
        val v0 = sprite.v0
        val vSpan = sprite.v1 - v0

        val src = quad.vertices
        val dst = src.copyOf()
        for (i in 0 until 4) {
            val base = i * VERTEX_STRIDE
            val u = Float.fromBits(src[base + 4])
            val v = Float.fromBits(src[base + 5])
            val normU = if (uSpan != 0f) ((u - u0) / uSpan).coerceIn(0f, 1f) else 0f
            val normV = if (vSpan != 0f) ((v - v0) / vSpan).coerceIn(0f, 1f) else 0f
            dst[base + 4] = normU.toRawBits()
            dst[base + 5] = normV.toRawBits()
        }
        return BakedQuad(dst, quad.tintIndex, quad.direction, sprite, quad.isShade)
    }
}
