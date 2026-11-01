package cn.xm1221.MieHexRevolution.mixin;

import cn.xm1221.MieHexRevolution.client.SkyTintHooks;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 天空/云朵染色。client mixin，必须登记在 mixins.json 的 "client" 数组里。 */
@Mixin(ClientLevel.class)
public abstract class MixinClientLevelSkyColor {

    @Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true)
    private void miehex_revolution$tintSkyColor(
            Vec3 cameraPos, float partialTick,
            CallbackInfoReturnable<Vec3> cir) {
        ClientLevel self = (ClientLevel) (Object) this;
        cir.setReturnValue(SkyTintHooks.tintSkyColor(self, cir.getReturnValue(), cameraPos));
    }

    @Inject(method = "getCloudColor", at = @At("RETURN"), cancellable = true)
    private void miehex_revolution$tintCloudColor(
            float partialTick,
            CallbackInfoReturnable<Vec3> cir) {
        ClientLevel self = (ClientLevel) (Object) this;
        cir.setReturnValue(SkyTintHooks.tintCloudColor(self, cir.getReturnValue()));
    }
}
