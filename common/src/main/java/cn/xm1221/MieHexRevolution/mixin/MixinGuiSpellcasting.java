package cn.xm1221.MieHexRevolution.mixin;

import at.petrak.hexcasting.api.casting.eval.ExecutionClientView;
import at.petrak.hexcasting.api.casting.eval.ResolvedPattern;
import at.petrak.hexcasting.client.gui.GuiSpellcasting;
import cn.xm1221.MieHexRevolution.client.PatternStrokeMarks;
import cn.xm1221.MieHexRevolution.util.ClientImportKeys;
import cn.xm1221.MieHexRevolution.util.MieHexRevolutionHelpersKt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * 只负责记下「这一笔是导入」。描边换色在 MieLib 的 DrawPatternHook 里完成，
 * 这里不再包 drawPatternFromPoints。
 */
@Mixin(GuiSpellcasting.class)
public abstract class MixinGuiSpellcasting {

    @Shadow(remap = false)
    private List<ResolvedPattern> patterns;

    @Inject(method = "render", at = @At("HEAD"))
    private void miehex_revolution$bindPatterns(CallbackInfo ci) {
        PatternStrokeMarks.bind(this.patterns);
    }

    @Inject(method = "recvServerUpdate", remap = false, at = @At("TAIL"))
    private void miehex_revolution$markImportFired(ExecutionClientView info, int index, CallbackInfo ci) {
        if (index >= 0 && index < this.patterns.size()) {
            ResolvedPattern pattern = this.patterns.get(index);
            if (ClientImportKeys.contains(MieHexRevolutionHelpersKt.importKeyString(pattern.getPattern()))) {
                PatternStrokeMarks.mark(pattern);
            }
        }
    }
}
