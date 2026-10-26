package cn.xm1221.MieHexRevolution.mixin;

import cn.xm1221.MieHexRevolution.casting.env.ExecutionColorProvider;
import cn.xm1221.MieHexRevolution.util.RemoteCastColorHolder;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 把远程施法执行色注册进玩家的 SynchedEntityData，
 * 使服务端写入后能同步到客户端（GUI mixin 读取用）。
 * 注：1.21 的 defineSynchedData(Builder) 带 Builder 参数，必须通过 Builder.define。
 * method 用 mojmap 名，靠 refmap 自动映射到 Forge SRG，不要写 remap=false。
 *
 * <p>accessor 必须在 Player 的静态初始化阶段注册：Builder 在 Entity 构造器里
 * 就按 {@code getCount(Player.class)} 固定了数组长度，若在首个实例之后才
 * defineId，就会拿到越界的 id（Index 21 out of bounds for length 21）。
 * 因此这里用 {@code @Unique static final} 字段，其初始化会被合并进 Player 的
 * {@code <clinit>}；外部通过 {@link RemoteCastColorHolder} 访问，不直接引用本 mixin 类。</p>
 */
@Mixin(Player.class)
public abstract class MixinPlayerSyncedData implements RemoteCastColorHolder {

    @Unique
    private static final EntityDataAccessor<Integer> MIEHEX$EXECUTION_COLOR =
            SynchedEntityData.defineId(Player.class, EntityDataSerializers.INT);

    @Inject(method = "defineSynchedData", at = @At("RETURN"))
    private void miehex_revolution$defineRemoteCastColor(SynchedEntityData.Builder builder, CallbackInfo ci) {
        builder.define(MIEHEX$EXECUTION_COLOR, ExecutionColorProvider.DEFAULT);
    }

    @Override
    public int miehex$getExecutionColor() {
        return ((Player) (Object) this).getEntityData().get(MIEHEX$EXECUTION_COLOR);
    }

    @Override
    public void miehex$setExecutionColor(int color) {
        ((Player) (Object) this).getEntityData().set(MIEHEX$EXECUTION_COLOR, color);
    }
}
