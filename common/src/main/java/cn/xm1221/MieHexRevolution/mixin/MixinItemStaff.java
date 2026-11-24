package cn.xm1221.MieHexRevolution.mixin;

import at.petrak.hexcasting.common.items.ItemStaff;
import at.petrak.hexcasting.common.lib.HexAttributes;
import cn.xm1221.MieHexRevolution.util.PlayerRemoteCastData;
import cn.xm1221.MieHexRevolution.util.RemoteCastDataKeys;
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
 * 注：
 * - method 用 mojmap 名 "use"，靠 refmap 映射（fabric: method_7836, forge: m_7203_），不要写 remap=false。
 * - 用 @At("HEAD") + isShiftKeyDown 判断，避免 @At INVOKE target 里 MC 类 descriptor
 *   （ServerPlayer 等）在 fabric intermediary 环境无法匹配的问题。
 * - 怯灵（FEEBLE_MIND）时 Hex 自己就不认潜行右键，这里也不该清数据，否则状态与它不一致。
 * - 客户端只清执行色（那边没有 PlayerRemoteCastData 表，清不到也无所谓），服务端清真正的目标。
 */
@Mixin(ItemStaff.class)
public abstract class MixinItemStaff {

    @Inject(method = "use", at = @At("HEAD"))
    private void miehex_revolution$clearRemoteCastData(
        Level world, Player player, InteractionHand hand,
        CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir
    ) {
        if (!player.isShiftKeyDown() || player.getAttributeValue(HexAttributes.FEEBLE_MIND) > 0) {
            return;
        }
        if (world.isClientSide) {
            RemoteCastDataKeys.reset(player);
        } else if (player instanceof ServerPlayer serverPlayer) {
            PlayerRemoteCastData.clear(serverPlayer);
        }
    }
}
