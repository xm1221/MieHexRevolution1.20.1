package cn.xm1221.MieHexRevolution.mixin;

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
import org.spongepowered.asm.mixin.Mixin;

/**
 * 物品外观伪装：画的是 {@link RendererHelper#getFaker(ItemStack)} 给出的栈。
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
}
