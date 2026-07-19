package cn.xm1221.MieHexRevolution.mixin;


import net.minecraft.network.protocol.game.ClientboundSetCameraPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {

    @Shadow
    private Entity camera;

    @Shadow
    public abstract boolean isSpectator();

    @Shadow
    public net.minecraft.server.network.ServerGamePacketListenerImpl connection;

    @Shadow
    public abstract Entity getCamera();

    @Shadow
    public abstract ServerLevel serverLevel();

    /**
     * 修改 setCamera 方法：
     * - 旁观者模式：完全保留原行为（传送 + 移动 + 发包）
     * - 非旁观者模式：只设置 camera 字段并发送相机包，不传送
     */
    @Inject(method = "setCamera", at = @At("HEAD"), cancellable = true)
    private void setCamera(Entity entity, CallbackInfo ci) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        // 旁观者模式：保持原逻辑，不取消方法
        if (self.isSpectator()) {
            return;
        }
        Entity newCamera = entity == null ? self : entity;
        this.camera = newCamera;
        self.connection.send(new ClientboundSetCameraPacket(newCamera));
        // 取消原方法的执行，避免后续的传送和移动代码
        ci.cancel();
    }
}