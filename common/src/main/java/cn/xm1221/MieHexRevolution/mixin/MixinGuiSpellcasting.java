package cn.xm1221.MieHexRevolution.mixin;

import at.petrak.hexcasting.api.casting.eval.ExecutionClientView;
import at.petrak.hexcasting.api.casting.eval.ResolvedPattern;
import at.petrak.hexcasting.client.gui.GuiSpellcasting;
import cn.xm1221.MieHexRevolution.duck.ImportMarkedSpellcasting;
import cn.xm1221.MieHexRevolution.util.ClientImportKeys;
import cn.xm1221.MieHexRevolution.util.MieHexRevolutionHelpersKt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * 导入视觉区分——**逐笔标记**：在 hexmod 每笔回报 recvServerUpdate 时，若这一笔的图案
 * 形状命中当前导入键集合（客户端由 MsgSyncImportKeysS2C 每图案同步），就把该
 * ResolvedPattern 实例记入 identity map。
 *
 * 图案描边换色现由 MieLib 的 DrawPatternHook（见 ImportDrawPatternWorker）处理；
 * 本 mixin 仅负责标记哪些笔应视为导入笔。
 */
@Mixin(GuiSpellcasting.class)
public abstract class MixinGuiSpellcasting implements ImportMarkedSpellcasting {

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

    /** seed 即 drawPatternFromPoints 的最后一个参数：轮盘已有图案的下标（WIP 图案是越界值，跳过） */
    @Override
    public boolean miehex_revolution$isImportMarked(double seed) {
        int idx = (int) seed;
        if (idx < 0 || idx >= this.patterns.size()) {
            return false;
        }
        return Boolean.TRUE.equals(this.miehex_revolution$importMarked.get(this.patterns.get(idx)));
    }
}
