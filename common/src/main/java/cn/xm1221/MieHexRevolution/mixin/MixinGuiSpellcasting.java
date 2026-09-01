package cn.xm1221.MieHexRevolution.mixin;

import at.petrak.hexcasting.api.casting.eval.ResolvedPattern;
import at.petrak.hexcasting.api.casting.math.HexPattern;
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
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;
import java.util.Set;

/**
 * GuiSpellcasting 渲染时给图案描边换色的两个钩子：
 * 1. 远程施法（执行色非 0）→ 整段替换为 RemoteCastDataKeys 的执行色（既有行为）；
 * 2. 当前施法有导入且正在画的图案是导入键 → 用 ImportsIota 的颜色（0xEE62EE）描边，
 *    与轮盘上导入触发的图案在视觉上区分（导入键集合由 MsgSyncImportKeysS2C 同步到客户端）。
 */
@Mixin(GuiSpellcasting.class)
public abstract class MixinGuiSpellcasting {

    private static final int ALPHA = 0xC8 << 24;
    /** 与 ImportsIota.Type.color()（15631086 = 0xEE62EE）一致 */
    private static final int IMPORT_COLOR = 0xEE62EE;
    private static final int IMPORT_FADE = 0xFFA7E0;

    // GuiSpellcasting is a mod class: never remapped, so keep the literal field name
    // (remap = false also silences the "unable to locate obfuscation mapping" warning).
    @Shadow(remap = false)
    private List<ResolvedPattern> patterns;

    @WrapOperation(
        // render 是 override Screen.render，走 refmap 映射（fabric: method_25394, forge: m_88315_）
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
        } else if (isImportKey(seed)) {
            tail = ALPHA | IMPORT_COLOR;
            head = ALPHA | IMPORT_FADE;
        }
        original.call(mat, points, dupIndices, drawLast, tail, head, flowIrregular,
            readabilityOffset, lastSegmentLen, seed);
    }

    /** seed 即 drawPatternFromPoints 的最后一个参数：轮盘已有图案的下标（WIP 图案是越界值，跳过） */
    private boolean isImportKey(double seed) {
        int idx = (int) seed;
        if (idx < 0 || idx >= this.patterns.size()) {
            return false;
        }
        HexPattern pat = this.patterns.get(idx).getPattern();
        return ClientImportKeys.contains(MieHexRevolutionHelpersKt.importKeyString(pat));
    }
}