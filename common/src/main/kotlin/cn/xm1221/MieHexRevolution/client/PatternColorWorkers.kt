package cn.xm1221.MieHexRevolution.client

import cn.xm1221.MieHexRevolution.Miehex_revolution
import cn.xm1221.MieHexRevolution.util.ClientImportKeys
import cn.xm1221.MieHexRevolution.util.RemoteCastDataKeys
import cn.xm1221.MieHexRevolution.util.importKeyString
import cn.xm1221.MieLib.client.mielib
import cn.xm1221.MieLib.registry.DrawPatternHook
import cn.xm1221.MieLib.registry.SpellcastingClientEvents
import cn.xm1221.MieLib.worker.DrawPatternWorker
import net.minecraft.client.Minecraft
import net.minecraft.resources.ResourceLocation

/** 与 ImportsIota.Type.color()（15631086 = 0xEE62EE）一致。 */
private const val IMPORT_ALPHA = 0xC8 shl 24
private const val IMPORT_COLOR = 0xEE62EE
private const val IMPORT_FADE = 0xFFA7E0

/**
 * 图案描边颜色与服务端回包标记全部交给 MieLib 的 [DrawPatternHook] 与 [SpellcastingClientEvents]。
 * 导入色先画，执行色优先级更高，远程施法时整段盖掉导入色。
 */
fun registerPatternColorWorkers() {
    SpellcastingClientEvents.registerGuiServerUpdate(
        Miehex_revolution.id("mark_import_stroke"),
        SpellcastingClientEvents.GuiServerUpdateListener { gui, _, index ->
            val list = gui.mielib().patterns
            if (index in list.indices) {
                val pattern = list[index]
                if (ClientImportKeys.contains(importKeyString(pattern.pattern))) {
                    PatternStrokeMarks.mark(pattern, list)
                }
            }
        },
    )
    DrawPatternHook.register(
        ResourceLocation(Miehex_revolution.MODID, "import_color"),
        object : DrawPatternWorker(0) {
            override fun ifEffect(result: DrawResult, ctx: DrawPatternContext) =
                PatternStrokeMarks.isMarked(ctx.pattern)

            override fun effect(drawResult: DrawResult, ctx: DrawPatternContext) =
                drawResult.withColor(IMPORT_ALPHA or IMPORT_COLOR, IMPORT_ALPHA or IMPORT_FADE)
        },
    )
    DrawPatternHook.register(
        ResourceLocation(Miehex_revolution.MODID, "execution_color"),
        object : DrawPatternWorker(100) {
            override fun ifEffect(result: DrawResult, ctx: DrawPatternContext): Boolean {
                val player = Minecraft.getInstance().player ?: return false
                return RemoteCastDataKeys.getColor(player) != 0
            }

            override fun effect(drawResult: DrawResult, ctx: DrawPatternContext): DrawResult {
                val color = RemoteCastDataKeys.getColor(Minecraft.getInstance().player!!)
                return drawResult.withColor(color)
            }
        },
    )
}
