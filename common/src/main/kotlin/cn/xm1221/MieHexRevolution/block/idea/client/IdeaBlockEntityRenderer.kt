package cn.xm1221.MieHexRevolution.block.idea.client

import cn.xm1221.MieHexRevolution.block.idea.IdeaBlockEntity
import cn.xm1221.MieHexRevolution.registry.Miehex_revolutionBlockEntities
import cn.xm1221.MieHexRevolution.registry.Miehex_revolutionBlocks
import com.mojang.blaze3d.vertex.PoseStack
import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.state.BlockState

class IdeaBlockEntityRenderer(ctx: BlockEntityRendererProvider.Context) : BlockEntityRenderer<IdeaBlockEntity> {
    override fun render(
        be: IdeaBlockEntity, partialTick: Float,
        poseStack: PoseStack, bufferSource: MultiBufferSource,
        packedLight: Int, packedOverlay: Int,
    ) {
        val material: BlockState
        val model: net.minecraft.client.resources.model.BakedModel

        if (be.hasCustomMaterial) {
            material = be.material
            model = Minecraft.getInstance().blockRenderer.getBlockModel(material)
        } else {
            // Default: use item model which has the idea texture
            val stack = ItemStack(Miehex_revolutionBlocks.IDEA_FULL.get())
            model = Minecraft.getInstance().itemRenderer.getModel(stack, be.level, null, 0)
            material = be.blockState
        }

        val level = be.level ?: return
        for (type in listOf(RenderType.solid(), RenderType.cutout())) {
            Minecraft.getInstance().blockRenderer.modelRenderer.tesselateBlock(
                level, model, material, be.blockPos, poseStack,
                bufferSource.getBuffer(type), true, level.random,
                material.getSeed(be.blockPos), OverlayTexture.NO_OVERLAY
            )
        }
    }

    override fun shouldRenderOffScreen(be: IdeaBlockEntity) = true
    override fun getViewDistance() = 256

    companion object {
        fun register() {
            BlockEntityRendererRegistry.register(
                Miehex_revolutionBlockEntities.IDEA_BE.get(), ::IdeaBlockEntityRenderer
            )
        }
    }
}
