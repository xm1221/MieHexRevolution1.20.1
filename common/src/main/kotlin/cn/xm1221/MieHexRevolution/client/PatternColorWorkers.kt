package cn.xm1221.MieHexRevolution.client

import cn.xm1221.MieHexRevolution.Miehex_revolution
import cn.xm1221.MieHexRevolution.util.RemoteCastDataKeys
import cn.xm1221.MieLib.registry.DrawPatternHook
import cn.xm1221.MieLib.worker.DrawPatternWorker
import net.minecraft.client.Minecraft

/** 与 ImportsIota.Type.color()（15631086 = 0xEE62EE）一致。 */
private const val IMPORT_ALPHA = 0xC8 shl 24
private const val IMPORT_COLOR = 0xEE62EE
private const val IMPORT_FADE = 0xFFA7E0

/**
 * 图案描边颜色交给 MieLib 的 [DrawPatternHook]。
 * 导入色先画，执行色优先级更高，远程施法时整段盖掉导入色。
 */
fun registerPatternColorWorkers() {
    DrawPatternHook.register(
        Miehex_revolution.id("import_color"),
        object : DrawPatternWorker(0) {
            override fun ifEffect(result: DrawResult, seed: Double) = PatternStrokeMarks.isMarked(seed)

            override fun effect(drawResult: DrawResult, seed: Double) = DrawResult(
                drawResult.drawLast,
                IMPORT_ALPHA or IMPORT_COLOR,
                IMPORT_ALPHA or IMPORT_FADE,
                drawResult.flowIrregular,
            )
        },
    )
    DrawPatternHook.register(
        Miehex_revolution.id("execution_color"),
        object : DrawPatternWorker(100) {
            override fun ifEffect(result: DrawResult, seed: Double): Boolean {
                val player = Minecraft.getInstance().player ?: return false
                return RemoteCastDataKeys.getColor(player) != 0
            }

            override fun effect(drawResult: DrawResult, seed: Double): DrawResult {
                val color = RemoteCastDataKeys.getColor(Minecraft.getInstance().player!!)
                return DrawResult(drawResult.drawLast, color, color, drawResult.flowIrregular)
            }
        },
    )
}
