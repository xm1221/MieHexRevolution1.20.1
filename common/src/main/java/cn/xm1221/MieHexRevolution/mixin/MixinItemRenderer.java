package cn.xm1221.MieHexRevolution.mixin;

import cn.xm1221.MieHexRevolution.util.IdeaItemAppearance;
import cn.xm1221.MieHexRevolution.util.RendererHelper;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

/**
 * 物品外观伪装：画的是 {@link RendererHelper#getFaker(ItemStack)} 给出的栈。
 *
 * <p>理念方块走另一条路（见 {@link IdeaItemAppearance}）：本体换成被模仿方块的物品模型，
 * 再补一圈负块描边。
 *
 * <p>三个坑：目标是实例方法（处理方法不能写 static）；{@code render} 的 {@code model}
 * 是按原物品解出来的，换了栈必须用 {@code getModel} 重解；带
 * {@code BlockEntityWithoutLevelRenderer} 的物品（箱子/潜影盒/床/旗帜）在
 * {@code renderStatic} 内部就 return 了，不走 {@code render}，所以两处都要包。
 */
@Mixin(ItemRenderer.class)
public abstract class MixinItemRenderer {

    @WrapMethod(method = "renderStatic(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/level/Level;III)V")
    private void miehex_revolution$renderStaticWithEntity(
            LivingEntity entity,
            ItemStack itemStack,
            ItemDisplayContext displayContext,
            boolean leftHand,
            PoseStack poseStack,
            MultiBufferSource buffer,
            Level level,
            int seed,
            int combinedLight,
            int combinedOverlay,
            Operation<Void> original) {
        original.call(entity, RendererHelper.INSTANCE.getFaker(itemStack), displayContext, leftHand,
                poseStack, buffer, level, seed, combinedLight, combinedOverlay);
    }

    @WrapMethod(method = "renderStatic(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;IILcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/level/Level;I)V")
    private void miehex_revolution$renderStatic(
            ItemStack itemStack,
            ItemDisplayContext displayContext,
            int combinedLight,
            int combinedOverlay,
            PoseStack poseStack,
            MultiBufferSource buffer,
            Level level,
            int seed,
            Operation<Void> original) {
        original.call(RendererHelper.INSTANCE.getFaker(itemStack), displayContext, combinedLight,
                combinedOverlay, poseStack, buffer, level, seed);
    }

    @WrapMethod(method = "render")
    private void miehex_revolution$render(
            ItemStack itemStack,
            ItemDisplayContext displayContext,
            boolean leftHand,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int combinedLight,
            int combinedOverlay,
            BakedModel model,
            Operation<Void> original) {
        // 理念方块：存了材质就把本体换成被模仿方块的方块模型，再补一圈负块描边。
        ItemStack material = IdeaItemAppearance.INSTANCE.materialStack(itemStack);
        if (material != null) {
            BakedModel materialModel = miehex_revolution$materialModel(itemStack, material);
            poseStack.pushPose();
            poseStack.scale(IdeaItemAppearance.ITEM_SCALE, IdeaItemAppearance.ITEM_SCALE,
                    IdeaItemAppearance.ITEM_SCALE);
            original.call(material, displayContext, leftHand, poseStack, buffer,
                    combinedLight, combinedOverlay, materialModel);
            IdeaItemAppearance.INSTANCE.renderShell(poseStack, buffer, materialModel, displayContext,
                    leftHand, combinedLight, combinedOverlay);
            poseStack.popPose();
            return;
        }
        ItemStack faker = RendererHelper.INSTANCE.getFaker(itemStack);
        if (faker == itemStack) {
            original.call(itemStack, displayContext, leftHand, poseStack, buffer,
                    combinedLight, combinedOverlay, model);
            return;
        }
        BakedModel fakerModel = ((ItemRenderer) (Object) this)
                .getModel(faker, Minecraft.getInstance().level, null, 0);
        original.call(faker, displayContext, leftHand, poseStack, buffer,
                combinedLight, combinedOverlay, fakerModel);
    }

    /**
     * 被模仿方块用哪个模型画。
     *
     * <p>一律用<b>方块模型</b>，不用“那个物品自己的模型”：门、活板门这类方块的物品模型
     * 只是一张平面贴图，画出来是张画，不像“已经被理念方块变形过”的方块；
     * 方块模型才是它摆在世界里的真正形状。取到模型后还要交给
     * {@link IdeaItemAppearance#itemModel} 兜一层：multipart 模型（栅栏/墙/玻璃板/栅栏门）
     * 遇到 {@code state = null} 会返回空表，而 ItemRenderer 取面时永远传 null。
     * 拿不到方块状态时（理论上不会）退回物品模型。
     */
    private BakedModel miehex_revolution$materialModel(ItemStack ideaStack, ItemStack material) {
        BlockState state = IdeaItemAppearance.INSTANCE.materialState(ideaStack);
        if (state != null) {
            return IdeaItemAppearance.INSTANCE.itemModel(state);
        }
        return ((ItemRenderer) (Object) this).getModel(material, Minecraft.getInstance().level, null, 0);
    }
}
