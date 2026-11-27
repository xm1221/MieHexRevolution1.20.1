package cn.xm1221.MieHexRevolution.pigment

import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.util.FastColor
import net.minecraft.util.Mth
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Generates dynamic, multi-condition mind/consciousness + Pride-inspired item names for
 * [cn.xm1221.MieHexRevolution.item.ItemCustomPigment] via [net.minecraft.world.item.Item.getName].
 */
object PigmentNaming {

    private data class Hsv(val h: Float, val s: Float, val v: Float)

    private data class ScoredKey(val key: String, val score: Float)

    private data class Metrics(
        val k: Int,
        val avgS: Float,
        val avgV: Float,
        val valSpan: Float,
        val hasWhite: Boolean,
        val hasBlack: Boolean,
        val hasChromatic: Boolean,
        val chromaticCount: Int,
        val hueSpan: Float,
        val isSymmetric: Boolean,
        val dominantHue: Float,
    )

    private const val KEY_PREFIX = "pigment_name.miehex_revolution."

    fun buildName(data: PigmentData): Component {
        val colors = data.colors
        if (colors.isEmpty()) {
            return Component.translatable("item.miehex_revolution.custom_pigment")
        }

        val m = analyze(colors)

        val prefixCandidates = ArrayList<ScoredKey>(4)
        if (m.avgS < 0.22f) {
            prefixCandidates.add(ScoredKey("prefix.detached", 0.92f - m.avgS))
        }
        if (m.hasWhite && m.hasChromatic && m.k >= 2) {
            prefixCandidates.add(ScoredKey("prefix.partial", 0.85f))
        }
        if (m.avgV < 0.34f) {
            prefixCandidates.add(ScoredKey("prefix.subliminal", 0.84f - m.avgV))
        }
        if (m.avgS > 0.78f && m.avgV > 0.78f) {
            prefixCandidates.add(ScoredKey("prefix.radiant", (m.avgS + m.avgV) * 0.44f))
        }
        val slot1 = prefixCandidates.maxByOrNull { it.score }?.key

        val traitCandidates = ArrayList<ScoredKey>(8)
        if (m.isSymmetric) {
            traitCandidates.add(ScoredKey("trait.mirrored", 0.95f))
        }
        if (m.k >= 6) {
            traitCandidates.add(ScoredKey("trait.overflowing", 0.92f))
        }
        if (m.k >= 3 && m.hueSpan > 200f) {
            traitCandidates.add(ScoredKey("trait.spectrum", 0.88f))
        }
        if (m.k == 2 && m.chromaticCount == 2 && m.hueSpan > 140f) {
            traitCandidates.add(ScoredKey("trait.paradox", 0.86f))
        }
        if (m.valSpan > 0.65f) {
            traitCandidates.add(ScoredKey("trait.binary", 0.80f))
        }
        if (m.k >= 4 && (m.valSpan > 0.35f || m.hueSpan > 90f)) {
            traitCandidates.add(ScoredKey("trait.plural", 0.78f))
        }
        if (m.k >= 3 && m.hueSpan in 70f..210f) {
            traitCandidates.add(ScoredKey("trait.fluid", 0.76f))
        }
        if (m.k >= 2 && m.chromaticCount >= 2 && m.hueSpan in 8f..55f) {
            traitCandidates.add(ScoredKey("trait.flowing", 0.74f))
        }
        val maxTraits = if (slot1 != null) 1 else 2
        val slot2 = traitCandidates
            .sortedByDescending { it.score }
            .take(maxTraits)
            .map { it.key }

        val slot3 = chooseCoreKey(m, slot1)

        val keys = ArrayList<String>(5)
        if (slot1 != null) keys.add(slot1)
        keys.addAll(slot2)
        keys.add(slot3)
        keys.add("suffix")

        val result = Component.empty()
        val lastIdx = (keys.size - 1).coerceAtLeast(1)
        for (i in keys.indices) {
            if (i > 0) {
                result.append(Component.translatable("${KEY_PREFIX}sep"))
            }
            val t = i.toFloat() / lastIdx.toFloat()
            val rgb = readableRgb(sampleColor(colors, t))
            result.append(
                Component.translatable(KEY_PREFIX + keys[i])
                    .withStyle(Style.EMPTY.withColor(rgb))
            )
        }
        return result
    }

    private fun chooseCoreKey(m: Metrics, slot1: String?): String {
        if (!m.hasChromatic) {
            return when {
                m.hasBlack && m.hasWhite -> "core.self"
                m.avgV < 0.25f -> "core.void"
                else -> "core.stillness"
            }
        }
        if (m.k >= 3 && m.hueSpan > 200f) {
            return if (m.k >= 6) "core.psyche" else "core.myriad"
        }
        val h = m.dominantHue
        return when {
            h < 22f || h >= 340f -> if (slot1 == "prefix.radiant") "core.obsession" else "core.ardor"
            h < 50f -> "core.impulse"
            h < 80f -> "core.insight"
            h < 155f -> "core.empathy"
            h < 205f -> "core.clarity"
            h < 255f -> "core.thought"
            h < 300f -> "core.mind"
            else -> "core.reverie"
        }
    }

    private fun analyze(colors: IntArray): Metrics {
        val k = colors.size
        val hsvs = Array(k) { i -> rgbToHsv(colors[i]) }

        var sumS = 0f
        var sumV = 0f
        var minV = Float.MAX_VALUE
        var maxV = -Float.MAX_VALUE
        var hasWhite = false
        var hasBlack = false
        val chromatics = ArrayList<Hsv>(k)

        for (hsv in hsvs) {
            sumS += hsv.s
            sumV += hsv.v
            if (hsv.v < minV) minV = hsv.v
            if (hsv.v > maxV) maxV = hsv.v
            if (hsv.s < 0.14f && hsv.v > 0.88f) hasWhite = true
            if (hsv.v < 0.18f) hasBlack = true
            if (hsv.s >= 0.16f && hsv.v >= 0.12f) chromatics.add(hsv)
        }

        val hueSpan = computeHueSpan(chromatics)
        val isSymmetric = k >= 3 && colors.distinct().size >= 2 && (0 until k / 2).all { i ->
            rgbClose(colors[i], colors[k - 1 - i], maxDelta = 24)
        }
        val dominantHue = computeDominantHue(chromatics)

        return Metrics(
            k = k,
            avgS = sumS / k,
            avgV = sumV / k,
            valSpan = (maxV - minV).coerceAtLeast(0f),
            hasWhite = hasWhite,
            hasBlack = hasBlack,
            hasChromatic = chromatics.isNotEmpty(),
            chromaticCount = chromatics.size,
            hueSpan = hueSpan,
            isSymmetric = isSymmetric,
            dominantHue = dominantHue,
        )
    }

    private fun computeHueSpan(chromatics: List<Hsv>): Float {
        if (chromatics.size < 2) return 0f
        val sorted = chromatics.map { it.h }.sorted()
        var maxGap = 0f
        for (i in 0 until sorted.size - 1) {
            val gap = sorted[i + 1] - sorted[i]
            if (gap > maxGap) maxGap = gap
        }
        val wrapGap = 360f - sorted.last() + sorted.first()
        if (wrapGap > maxGap) maxGap = wrapGap
        return (360f - maxGap).coerceIn(0f, 360f)
    }

    private fun computeDominantHue(chromatics: List<Hsv>): Float {
        if (chromatics.isEmpty()) return -1f
        var sumSin = 0.0
        var sumCos = 0.0
        for (hsv in chromatics) {
            val rad = Math.toRadians(hsv.h.toDouble())
            val weight = (hsv.s * hsv.v).toDouble().coerceAtLeast(0.01)
            sumSin += sin(rad) * weight
            sumCos += cos(rad) * weight
        }
        if (sumSin == 0.0 && sumCos == 0.0) return chromatics[0].h
        var deg = Math.toDegrees(atan2(sumSin, sumCos)).toFloat()
        if (deg < 0f) deg += 360f
        return deg
    }

    private fun rgbClose(a: Int, b: Int, maxDelta: Int): Boolean =
        abs(FastColor.ARGB32.red(a) - FastColor.ARGB32.red(b)) <= maxDelta &&
            abs(FastColor.ARGB32.green(a) - FastColor.ARGB32.green(b)) <= maxDelta &&
            abs(FastColor.ARGB32.blue(a) - FastColor.ARGB32.blue(b)) <= maxDelta

    private fun rgbToHsv(argb: Int): Hsv {
        val r = FastColor.ARGB32.red(argb) / 255f
        val g = FastColor.ARGB32.green(argb) / 255f
        val b = FastColor.ARGB32.blue(argb) / 255f
        val maxC = max(r, max(g, b))
        val minC = min(r, min(g, b))
        val delta = maxC - minC

        val h = when {
            delta <= 1e-5f -> 0f
            maxC == r -> 60f * (((g - b) / delta).mod(6f))
            maxC == g -> 60f * (((b - r) / delta) + 2f)
            else -> 60f * (((r - g) / delta) + 4f)
        }.let { if (it < 0f) it + 360f else it }

        val s = if (maxC <= 1e-5f) 0f else delta / maxC
        return Hsv(h, s, maxC)
    }

    private fun sampleColor(colors: IntArray, t: Float): Int {
        if (colors.size == 1) return colors[0]
        val clamped = t.coerceIn(0f, 1f)
        val scaled = clamped * (colors.size - 1)
        val idx = scaled.toInt().coerceIn(0, colors.size - 2)
        val frac = scaled - idx
        val c0 = colors[idx]
        val c1 = colors[idx + 1]
        val r = Mth.lerp(frac, FastColor.ARGB32.red(c0).toFloat(), FastColor.ARGB32.red(c1).toFloat()).roundToInt()
        val g = Mth.lerp(frac, FastColor.ARGB32.green(c0).toFloat(), FastColor.ARGB32.green(c1).toFloat()).roundToInt()
        val b = Mth.lerp(frac, FastColor.ARGB32.blue(c0).toFloat(), FastColor.ARGB32.blue(c1).toFloat()).roundToInt()
        return FastColor.ARGB32.color(255, r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
    }

    private fun readableRgb(argb: Int): Int {
        var r = FastColor.ARGB32.red(argb)
        var g = FastColor.ARGB32.green(argb)
        var b = FastColor.ARGB32.blue(argb)
        val lum = (0.2126 * r + 0.7152 * g + 0.0722 * b) / 255.0
        if (lum < 0.25) {
            val lift = ((0.25 - lum) / 0.25).coerceIn(0.0, 1.0)
            r = (r + (96 - r) * lift).roundToInt().coerceIn(0, 255)
            g = (g + (96 - g) * lift).roundToInt().coerceIn(0, 255)
            b = (b + (96 - b) * lift).roundToInt().coerceIn(0, 255)
        }
        return (r shl 16) or (g shl 8) or b
    }
}
