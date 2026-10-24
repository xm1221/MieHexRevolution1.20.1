package cn.xm1221.MieHexRevolution.client

import cn.xm1221.MieHexRevolution.duck.ImportMarkedSpellcasting
import cn.xm1221.MieHexRevolution.util.RemoteCastDataKeys
import cn.xm1221.MieLib.registry.DrawPatternHook
import cn.xm1221.MieLib.worker.DrawPatternWorker
import net.minecraft.client.Minecraft
import net.minecraft.resources.ResourceLocation

/**
 * 通过 MieLib 的 DrawPatternHook 给 GuiSpellcasting 中的图案描边换色（原 MixinGuiSpellcasting 的 WrapOperation）：
 * 1. 远程施法（执行色非 0）→ 整段替换为 RemoteCastDataKeys 的执行色（优先）；
 * 2. 被导入标记的那一笔 → ImportsIota 同色描边。
 * 仅客户端。
 */
object ImportDrawPatternWorker : DrawPatternWorker(0) {
    private const val ALPHA = 0xC8 shl 24
    /** 与 ImportsIota.Type.color()（15631086 = 0xEE62EE）一致 */
    private const val IMPORT_COLOR = 0xEE62EE
    private const val IMPORT_FADE = 0xFFA7E0

    @JvmStatic
    fun init() {
        DrawPatternHook.register(ResourceLocation("miehex_revolution", "import_and_remote_color"), this)
    }

    override fun effect(drawResult: DrawResult, seed: Double): DrawResult {
        val mc = Minecraft.getInstance()
        val color = mc.player?.let { RemoteCastDataKeys.getColor(it) } ?: 0
        if (color != 0) {
            return DrawResult(drawResult.drawLast, color, color, drawResult.flowIrregular)
        }
        val screen = mc.screen
        if (screen is ImportMarkedSpellcasting && screen.`miehex_revolution$isImportMarked`(seed)) {
            return DrawResult(drawResult.drawLast, ALPHA or IMPORT_COLOR, ALPHA or IMPORT_FADE, drawResult.flowIrregular)
        }
        return drawResult
    }
}
