package cn.xm1221.MieHexRevolution.mixin;

import at.petrak.hexcasting.api.casting.eval.ExecutionClientView;
import at.petrak.hexcasting.api.casting.eval.ResolvedPattern;
import at.petrak.hexcasting.client.gui.GuiSpellcasting;
import cn.xm1221.MieHexRevolution.util.ClientImportKeys;
import cn.xm1221.MieHexRevolution.util.MieHexRevolutionHelpersKt;
import cn.xm1221.MieHexRevolution.util.RemoteCastDataKeys;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec2;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * GuiSpellcasting 渲染时给图案描边换色的两个钩子：
 * 1. 远程施法（执行色非 0）→ 整段替换为 RemoteCastDataKeys 的执行色（既有行为）；
 * 2. 导入视觉区分——**逐笔标记**：在 hexmod 每笔回报 recvServerUpdate 时，若这一笔的图案
 *    形状命中当前导入键集合（客户端由 MsgSyncImportKeysS2C 每图案同步），就把该
 *    ResolvedPattern 实例记入 identity map；渲染时只给"被标记的那一笔"描边换
 *    ImportsIota 同色（0xEE62EE）。已画的老图案、同形状的其它笔都不会变色。
 */
@Mixin(GuiSpellcasting.class)
public abstract class MixinGuiSpellcasting {

    private static final int ALPHA = 0xC8 << 24;
    /** 与 ImportsIota.Type.color()（15631086 = 0xEE62EE）一致 */
    private static final int IMPORT_COLOR = 0xEE62EE;
    private static final int IMPORT_FADE = 0xFFA7E0;

    // GuiSpellcasting 是模组类，不参与混淆：字面字段名即可（remap=false 同时消掉
    // "unable to locate obfuscation mapping" 编译警告）。
    @Shadow(remap = false)
    private List<ResolvedPattern> patterns;

    /** 逐笔标记：被标记的 ResolvedPattern 实例在渲染时用导入色描边 */
    @Unique
    private final Map<ResolvedPattern, Boolean> miehex_revolution$importMarked = new IdentityHashMap<>();

    // recvServerUpdate 是模组类方法，不参与混淆：remap=false 让编译期 AP 跳过映射查找
    @Inject(method = "recvServerUpdate", remap = false, at = @At("TAIL"))
    private void miehex_revolution$markImportFired(ExecutionClientView info, int index, CallbackInfo ci) {
        if (index >= 0 && index < this.patterns.size()) {
            ResolvedPattern rp = this.patterns.get(index);
            if (ClientImportKeys.contains(MieHexRevolutionHelpersKt.importKeyString(rp.getPattern()))) {
                this.miehex_revolution$importMarked.put(rp, Boolean.TRUE);
            }
        }
    }

    @WrapOperation(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lat/petrak/hexcasting/client/render/RenderLib;drawPatternFromPoints(Lorg/joml/Matrix4f;Ljava/util/List;Ljava/util/Set;ZIIFFFD)V",
            remap = false
        )
    )
    private void miehex_revolution$wrapExecutionColor(
        Matrix4f mat, List<Vec2> points, Set<Integer> dupIndices,
        boolean drawLast, int tail, int head, float flowIrregular,
        float readabilityOffset, float lastSegmentLen, double seed,
        Operation<Void> original
    ) {
        // 远程施法整段覆盖优先
        int color = RemoteCastDataKeys.getColor(Minecraft.getInstance().player);
        if (color != 0) {
            tail = color;
            head = color;
        } else if (isImportMarked(seed)) {
            tail = ALPHA | IMPORT_COLOR;
            head = ALPHA | IMPORT_FADE;
        }
        original.call(mat, points, dupIndices, drawLast, tail, head, flowIrregular,
            readabilityOffset, lastSegmentLen, seed);
    }

    /** seed 即 drawPatternFromPoints 的最后一个参数：轮盘已有图案的下标（WIP 图案是越界值，跳过） */
    private boolean isImportMarked(double seed) {
        int idx = (int) seed;
        if (idx < 0 || idx >= this.patterns.size()) {
            return false;
        }
        return Boolean.TRUE.equals(this.miehex_revolution$importMarked.get(this.patterns.get(idx)));
    }
}