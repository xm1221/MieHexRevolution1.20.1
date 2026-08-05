package cn.xm1221.MieHexRevolution.mixin;

import at.petrak.hexcasting.common.items.ItemStaff;
import at.petrak.hexcasting.xplat.IXplatAbstractions;
import cn.xm1221.MieHexRevolution.util.PlayerRemoteCastData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 法杖潜行右键（清除施法数据并重新打开 GUI）时，同时清除玩家实体上的远程施法目标数据。
 * 注：method 用 mojmap 名 "use"，靠 refmap 自动映射到 Forge SRG（m_7203_），不要写 remap=false。
 */
@Mixin(ItemStaff.class)
public abstract class MixinItemStaff {

    @Inject(
        method = "use",
        at = @At(
            value = "INVOKE",
            target = "Lat/petrak/hexcasting/xplat/IXplatAbstractions;clearCastingData(Lnet/minecraft/server/level/ServerPlayer;)V",
            remap = false
        )
    )
    private void miehex_revolution$clearRemoteCastData(
        Level world, Player player, InteractionHand hand,
        CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir
    ) {
        if (!world.isClientSide && player instanceof ServerPlayer serverPlayer) {
            PlayerRemoteCastData.clear(serverPlayer);
        }
    }
}
