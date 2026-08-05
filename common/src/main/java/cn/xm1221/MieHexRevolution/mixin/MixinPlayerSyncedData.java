package cn.xm1221.MieHexRevolution.mixin;

import cn.xm1221.MieHexRevolution.casting.env.ExecutionColorProvider;
import cn.xm1221.MieHexRevolution.util.RemoteCastDataKeys;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 把远程施法执行色注册进玩家的 SynchedEntityData，
 * 使服务端写入后能同步到客户端（GUI mixin 读取用）。
 * 注：1.20.1 的 defineSynchedData() 无参数，直接 entityData.define。
 * method 用 mojmap 名，靠 refmap 自动映射到 Forge SRG，不要写 remap=false。
 */
@Mixin(Player.class)
public abstract class MixinPlayerSyncedData {

    @Inject(method = "defineSynchedData", at = @At("RETURN"))
    private void miehex_revolution$defineRemoteCastColor(CallbackInfo ci) {
        ((Player) (Object) this).getEntityData()
            .define(RemoteCastDataKeys.EXECUTION_COLOR, ExecutionColorProvider.DEFAULT);
    }
}
