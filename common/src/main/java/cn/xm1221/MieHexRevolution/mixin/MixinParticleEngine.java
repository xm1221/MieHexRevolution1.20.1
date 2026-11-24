package cn.xm1221.MieHexRevolution.mixin;

import cn.xm1221.MieHexRevolution.block.idea.IdeaBlock;
import cn.xm1221.MieHexRevolution.block.idea.IdeaBlockEntity;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

/**
 * 破坏粒子跟随伪装。
 *
 * <p>原版 {@code ParticleEngine#destroy(BlockPos, BlockState)} 决定粒子纹理与数量的，
 * 就是传进来的那个方块状态；而方块实体渲染器管不到这一步，所以敲一块伪装成石头的
 * 理念方块会掉理念碎片。这里把状态换成被模仿的那份再交回原版。
 */
@Mixin(ParticleEngine.class)
public abstract class MixinParticleEngine {

    @WrapMethod(method = "destroy")
    private void miehex_revolution$destroyWithMaterial(BlockPos pos, BlockState state, Operation<Void> original) {
        Level level = Minecraft.getInstance().level;
        if (level != null && state.getBlock() instanceof IdeaBlock) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof IdeaBlockEntity idea && !idea.getMaterial().isAir()) {
                original.call(pos, idea.getMaterial());
                return;
            }
        }
        original.call(pos, state);
    }
}
