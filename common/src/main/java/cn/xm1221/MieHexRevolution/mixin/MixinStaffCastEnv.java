package cn.xm1221.MieHexRevolution.mixin;

import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import at.petrak.hexcasting.api.casting.eval.env.PlayerBasedCastEnv;
import at.petrak.hexcasting.api.casting.eval.env.StaffCastEnv;
import at.petrak.hexcasting.api.casting.eval.vm.CastingVM;
import at.petrak.hexcasting.xplat.IXplatAbstractions;
import cn.xm1221.MieHexRevolution.casting.env.FakeCastingEnv;
import cn.xm1221.MieHexRevolution.casting.env.PlayersCastingEnv;
import cn.xm1221.MieHexRevolution.util.PlayerRemoteCastData;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 玩家实体持有施法目标实体数据时，把 StaffCastEnv 中执行改为在
 * FakeCastingEnv（目标为生物）或 PlayersCastingEnv（目标为玩家）中执行：
 * - 保留原有 CastingImage（栈延续），仅替换环境
 * - CastingImage 的同步仍由 handleNewPatternOnServer 的原流程完成（setStaffcastImage / MsgNewSpellPatternS2C）
 */
@Mixin(StaffCastEnv.class)
public abstract class MixinStaffCastEnv {

    @WrapOperation(
        method = "handleNewPatternOnServer",
        at = @At(
            value = "INVOKE",
            target = "Lat/petrak/hexcasting/xplat/IXplatAbstractions;getStaffcastVM(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/InteractionHand;)Lat/petrak/hexcasting/api/casting/eval/vm/CastingVM;",
            // 必须 remap，让 mapping 服务把 target 里 MC 类的 descriptor（ServerPlayer 等）
            // 转换到运行时名（fabric intermediary: class_3222），否则 INVOKE 点找不到
            remap = true
        ),
        remap = false
    )
    private static CastingVM miehex_revolution$wrapVM(
        IXplatAbstractions inst, ServerPlayer player, InteractionHand hand,
        Operation<CastingVM> original
    ) {
        CastingVM vm = original.call(inst, player, hand);
        LivingEntity target = PlayerRemoteCastData.get(player);
        if (target != null) {
            CastingEnvironment env = vm.getEnv();
            if (target instanceof ServerPlayer friend && env instanceof PlayerBasedCastEnv pEnv) {
                env = new PlayersCastingEnv(pEnv, friend);
            } else {
                env = new FakeCastingEnv(env, target);
            }
            return new CastingVM(vm.getImage(), env);
        }
        return vm;
    }
}
