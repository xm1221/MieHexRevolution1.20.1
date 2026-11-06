package cn.xm1221.MieHexRevolution.block.idea.client

import cn.xm1221.MieHexRevolution.block.idea.IdeaBlockEntity
import cn.xm1221.MieHexRevolution.registry.Miehex_revolutionBlockEntities
import cn.xm1221.MieHexRevolution.registry.Miehex_revolutionBlocks
import com.mojang.blaze3d.vertex.PoseStack
import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.ItemBlockRenderTypes
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.client.resources.model.BakedModel
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.state.BlockState

/**
 * 理念方块的外观渲染。
 *
 * <p>方块自身的模型是空的（models/block/idea_base.json），几何全部在这里按材质状态重新拼。
 * 关键是**只走一条** {@link ItemBlockRenderTypes#getRenderType} 通道：以前 solid + cutout
 * 画两遍，镂空方块（树叶）会被 solid 画成不透明并叠出两层几何。
 */
class IdeaBlockEntityRenderer(ctx: BlockEntityRendererProvider.Context) :
    BlockEntityRenderer<IdeaBlockEntity> {

    override fun render(
        be: IdeaBlockEntity, partialTick: Float,
        poseStack: PoseStack, bufferSource: MultiBufferSource,
        packedLight: Int, packedOverlay: Int,
    ) {
        val level = be.level ?: return
        val minecraft = Minecraft.getInstance()
        val material = be.material

        val model: BakedModel
        val state: BlockState
        val renderType: RenderType
        if (material.isAir) {
            // 还没有外观：显示理念方块本体（物品模型里的那块紫水晶纹理）。
            state = be.blockState
            model = minecraft.itemRenderer.getModel(
                ItemStack(Miehex_revolutionBlocks.IDEA_FULL.get()), level, null, 0,
            )
            renderType = RenderType.cutout()
        } else {
            state = material
            model = minecraft.blockRenderer.getBlockModel(material)
            renderType = ItemBlockRenderTypes.getRenderType(material, false)
        }

        minecraft.blockRenderer.modelRenderer.tesselateBlock(
            level, model, state, be.blockPos, poseStack,
            bufferSource.getBuffer(renderType), true, level.random,
            state.getSeed(be.blockPos), OverlayTexture.NO_OVERLAY,
        )
    }

    companion object {
        fun register() {
            BlockEntityRendererRegistry.register(
                Miehex_revolutionBlockEntities.IDEA_BE.get(), ::IdeaBlockEntityRenderer
            )
        }
    }
}
