package cn.xm1221.MieHexRevolution.client.pigment

import cn.xm1221.MieHexRevolution.Miehex_revolution
import cn.xm1221.MieHexRevolution.pigment.PigmentData
import com.google.gson.JsonParser
import com.mojang.blaze3d.platform.NativeImage
import net.minecraft.Util
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManagerReloadListener
import net.minecraft.util.FastColor
import net.minecraft.util.Mth
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Client-side runtime animated texture baker and LRU cache for
 * [cn.xm1221.MieHexRevolution.item.ItemCustomPigment].
 *
 * Reuses Hex Casting's own colorizer sprite strips and `.mcmeta` frame schedules
 * directly from the active [net.minecraft.server.packs.resources.ResourceManager].
 */
object CustomPigmentTextures {

    private const val FALLBACK_SIZE = 16
    private const val MAX_CACHED_TEXTURES = 64

    private val TEMPLATE_REF_COLORS: Map<String, IntArray> = mapOf(
        "dye_red" to intArrayOf(0xD82F3A),
        "default" to intArrayOf(0xAB65EB),
        "uuid" to intArrayOf(0xB38EF3, 0xFAD64A),
        "pride_intersex" to intArrayOf(0xEBF367, 0x7210BC),
        "pride_pansexual" to intArrayOf(0xE278EF, 0xEBF367, 0x6AC2E4),
        "pride_transgender" to intArrayOf(0xEB92EA, 0xFFFFFF, 0x6AC2E4),
        "pride_bisexual" to intArrayOf(0xDB45FF, 0x9C2BD0, 0x2F4DD8),
        "pride_genderqueer" to intArrayOf(0xCA78EF, 0xFFFFFF, 0x2DB418),
        "pride_demiboy" to intArrayOf(0x7A8081, 0x6AC2E4, 0xFFFFFF),
        "pride_agender" to intArrayOf(0x16A10C, 0xFFFFFF, 0x7A8081, 0x302F30),
        "pride_asexual" to intArrayOf(0x333233, 0x9A9FA1, 0xFFFFFF, 0x7210BC),
        "pride_nonbinary" to intArrayOf(0xEBF367, 0xFFFFFF, 0x7210BC, 0x333233),
        "pride_plural" to intArrayOf(0x30C69F, 0x347DDF, 0x6B3FBE, 0x110F11),
        "pride_gay" to intArrayOf(0xD82F3A, 0xE0883F, 0xEBF367, 0x2DB418, 0x2F4DD8),
        "pride_lesbian" to intArrayOf(0xD82F3A, 0xE0883F, 0xFFFFFF, 0xE278EF, 0xA30262),
        "pride_aroace" to intArrayOf(0x353DBA, 0x82DCEB, 0xFFFFFF, 0xEBF367, 0xDD6D26),
        "pride_aromantic" to intArrayOf(0x16A10C, 0x82EB8B, 0xFFFFFF, 0x7A8081, 0x302F30),
        "pride_genderfluid" to intArrayOf(0xFBACF9, 0xFFFFFF, 0x9C2BD0, 0x333233, 0x2F4DD8),
    )

    private val FALLBACK_SHADE_ROWS = arrayOf(
        "................",
        "................",
        "................",
        "......1111......",
        "....11333200....",
        "....14412220....",
        "...1344311220...",
        "...1313311120...",
        "...1321131130...",
        "...1221113430...",
        "....02211430....",
        "....00223300....",
        "......0000......",
        "................",
        "................",
        "................",
    )

    private class MaskTemplate(
        val frameSize: Int,
        val frameCount: Int,
        val bandCount: Int,
        val frames: Array<IntArray>,
        val timeline: IntArray,
    )

    data class CachedTexture(
        val location: ResourceLocation,
        val texture: DynamicTexture,
        val renderType: RenderType,
        var lastFrame: Int = -1,
    )

    private val templateCache = HashMap<String, MaskTemplate>()
    private var generation = 0
    private var sequence = 0

    private val cache = object : LinkedHashMap<PigmentData, CachedTexture>(MAX_CACHED_TEXTURES, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<PigmentData, CachedTexture>): Boolean {
            if (size > MAX_CACHED_TEXTURES) {
                Minecraft.getInstance().textureManager.release(eldest.value.location)
                return true
            }
            return false
        }
    }

    val RELOAD_LISTENER: ResourceManagerReloadListener = ResourceManagerReloadListener {
        clear()
    }

    fun getOrCreate(data: PigmentData): CachedTexture {
        val maskId = PigmentData.resolveMask(data)
        val template = getOrLoadTemplate(maskId)
        val tick = floor((Util.getMillis() / 50.0) * data.speed).toLong()
        val frameIdx = template.timeline[tick.mod(template.timeline.size)]

        val entry = cache[data] ?: run {
            val texture = DynamicTexture(template.frameSize, template.frameSize, true)
            val rl = Miehex_revolution.id("dynamic/pigment/${generation}_${sequence++}")
            Minecraft.getInstance().textureManager.register(rl, texture)
            val created = CachedTexture(rl, texture, RenderType.entityCutout(rl), lastFrame = -1)
            cache[data] = created
            created
        }

        if (entry.lastFrame != frameIdx) {
            val pixels = entry.texture.pixels
            if (pixels != null) {
                writeFrame(pixels, data, template, frameIdx)
                entry.texture.upload()
                entry.lastFrame = frameIdx
            }
        }
        return entry
    }

    fun clear() {
        val tm = Minecraft.getInstance().textureManager
        for (entry in cache.values) {
            tm.release(entry.location)
        }
        cache.clear()
        templateCache.clear()
        generation++
        sequence = 0
        CustomPigmentRenderer.clearModelCache()
    }

    private fun writeFrame(
        image: NativeImage,
        data: PigmentData,
        template: MaskTemplate,
        frameIdx: Int,
    ) {
        val size = template.frameSize
        val frame = template.frames[frameIdx.coerceIn(0, template.frameCount - 1)]
        val colors = data.colors
        val phase = frameIdx.toFloat() / template.frameCount.coerceAtLeast(1).toFloat()
        val diagDenom = (2f * (size - 1).coerceAtLeast(1))

        for (y in 0 until size) {
            for (x in 0 until size) {
                val packed = frame[y * size + x]
                val alpha = (packed ushr 16) and 0xFF
                if (alpha == 0) {
                    image.setPixelRGBA(x, y, 0)
                    continue
                }
                val bandIdx = packed and 0xFF
                val level = ((packed ushr 8) and 0xFF).coerceIn(0, 4)

                val baseArgb = when {
                    colors.isEmpty() -> PigmentData.DEFAULT_COLOR
                    colors.size == 1 || data.mode == PigmentData.Mode.SOLID -> colors[0]
                    colors.size <= template.bandCount -> colors[bandIdx % colors.size]
                    else -> {
                        val u = ((size - 1 - x) + y).toFloat() / diagDenom
                        resolveMultiColor(colors, data.mode, u, phase)
                    }
                }

                val (r, g, b) = shadeColor(baseArgb, level)
                // NativeImage expects ABGR byte order.
                image.setPixelRGBA(x, y, FastColor.ABGR32.color(255, b, g, r))
            }
        }
    }

    private fun resolveMultiColor(
        colors: IntArray,
        mode: PigmentData.Mode,
        u: Float,
        phase: Float,
    ): Int {
        if (mode == PigmentData.Mode.STRIPE) {
            val offset = (sin(phase * 2.0 * Math.PI) * 0.08).toFloat()
            val pos = (u + offset).mod(1f)
            val idx = (pos * colors.size).toInt().coerceIn(0, colors.size - 1)
            return colors[idx]
        }

        val fIdx = ((u + phase).mod(1f)) * colors.size
        val base = floor(fIdx).toInt()
        val tRaw = fIdx - base
        val t = if (tRaw < 0.5f) {
            4f * tRaw * tRaw * tRaw
        } else {
            1f - (-2f * tRaw + 2f).pow(3) / 2f
        }
        val c0 = colors[base % colors.size]
        val c1 = colors[(base + 1) % colors.size]
        val r = Mth.lerp(t, FastColor.ARGB32.red(c0).toFloat(), FastColor.ARGB32.red(c1).toFloat()).roundToInt()
        val g = Mth.lerp(t, FastColor.ARGB32.green(c0).toFloat(), FastColor.ARGB32.green(c1).toFloat()).roundToInt()
        val b = Mth.lerp(t, FastColor.ARGB32.blue(c0).toFloat(), FastColor.ARGB32.blue(c1).toFloat()).roundToInt()
        return FastColor.ARGB32.color(255, r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
    }

    private fun shadeColor(argb: Int, level: Int): Triple<Int, Int, Int> {
        val r = FastColor.ARGB32.red(argb)
        val g = FastColor.ARGB32.green(argb)
        val b = FastColor.ARGB32.blue(argb)
        return when (level) {
            0 -> Triple((r * 0.45).toInt().coerceIn(0, 255), (g * 0.45).toInt().coerceIn(0, 255), (b * 0.45).toInt().coerceIn(0, 255))
            1 -> Triple((r * 0.68).toInt().coerceIn(0, 255), (g * 0.68).toInt().coerceIn(0, 255), (b * 0.68).toInt().coerceIn(0, 255))
            2 -> Triple((r * 0.90).toInt().coerceIn(0, 255), (g * 0.90).toInt().coerceIn(0, 255), (b * 0.90).toInt().coerceIn(0, 255))
            3 -> Triple(r, g, b)
            else -> Triple(
                (r + (255 - r) * 0.40).toInt().coerceIn(0, 255),
                (g + (255 - g) * 0.40).toInt().coerceIn(0, 255),
                (b + (255 - b) * 0.40).toInt().coerceIn(0, 255),
            )
        }
    }

    private fun getOrLoadTemplate(maskId: String): MaskTemplate =
        templateCache.getOrPut(maskId) { loadHexTemplate(maskId) ?: buildFallbackTemplate() }

    private fun loadHexTemplate(maskId: String): MaskTemplate? {
        val refColors = TEMPLATE_REF_COLORS[maskId] ?: return null
        val pngLoc = ResourceLocation.fromNamespaceAndPath("hexcasting", "textures/item/colorizer/$maskId.png")
        val mcmetaLoc = ResourceLocation.fromNamespaceAndPath("hexcasting", "textures/item/colorizer/$maskId.png.mcmeta")
        val rm = Minecraft.getInstance().resourceManager

        return try {
            val res = rm.getResource(pngLoc).orElse(null) ?: return null
            res.open().use { stream ->
                NativeImage.read(stream).use { img ->
                    val frameSize = img.width.coerceAtLeast(1)
                    val frameCount = (img.height / frameSize).coerceAtLeast(1)
                    val timeline = loadTimeline(rm, mcmetaLoc, frameCount)

                    val uniqueRgbs = LinkedHashSet<Int>()
                    for (f in 0 until frameCount) {
                        val yOff = f * frameSize
                        for (y in 0 until frameSize) {
                            for (x in 0 until frameSize) {
                                val abgr = img.getPixelRGBA(x, yOff + y)
                                if (FastColor.ABGR32.alpha(abgr) > 0) {
                                    val rgb = (FastColor.ABGR32.red(abgr) shl 16) or
                                        (FastColor.ABGR32.green(abgr) shl 8) or
                                        FastColor.ABGR32.blue(abgr)
                                    uniqueRgbs.add(rgb)
                                }
                            }
                        }
                    }

                    val bandCount = refColors.size.coerceAtLeast(1)
                    val byBand = Array(bandCount) { ArrayList<Int>() }
                    val rgbToBand = HashMap<Int, Int>(uniqueRgbs.size * 2)
                    for (rgb in uniqueRgbs) {
                        val bIdx = if (bandCount == 1) 0 else nearestBand(rgb, refColors)
                        rgbToBand[rgb] = bIdx
                        byBand[bIdx].add(rgb)
                    }

                    val rgbToLevel = HashMap<Int, Int>(uniqueRgbs.size * 2)
                    for (bandList in byBand) {
                        bandList.sortBy { rgb ->
                            val r = (rgb ushr 16) and 0xFF
                            val g = (rgb ushr 8) and 0xFF
                            val b = rgb and 0xFF
                            0.2126 * r + 0.7152 * g + 0.0722 * b
                        }
                        val n = bandList.size
                        for (rank in 0 until n) {
                            val level = if (n <= 1) 3 else ((rank * 4.0) / (n - 1)).roundToInt().coerceIn(0, 4)
                            rgbToLevel[bandList[rank]] = level
                        }
                    }

                    val frames = Array(frameCount) { f ->
                        val yOff = f * frameSize
                        IntArray(frameSize * frameSize) { idx ->
                            val x = idx % frameSize
                            val y = idx / frameSize
                            val abgr = img.getPixelRGBA(x, yOff + y)
                            if (FastColor.ABGR32.alpha(abgr) == 0) {
                                0
                            } else {
                                val rgb = (FastColor.ABGR32.red(abgr) shl 16) or
                                    (FastColor.ABGR32.green(abgr) shl 8) or
                                    FastColor.ABGR32.blue(abgr)
                                val band = rgbToBand[rgb] ?: 0
                                val level = rgbToLevel[rgb] ?: 3
                                (255 shl 16) or (level shl 8) or (band and 0xFF)
                            }
                        }
                    }

                    MaskTemplate(frameSize, frameCount, bandCount, frames, timeline)
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun loadTimeline(
        rm: net.minecraft.server.packs.resources.ResourceManager,
        mcmetaLoc: ResourceLocation,
        frameCount: Int,
    ): IntArray {
        val defaultTimeline = IntArray(frameCount) { it }
        val res = rm.getResource(mcmetaLoc).orElse(null) ?: return defaultTimeline
        return try {
            res.open().use { stream ->
                InputStreamReader(stream, StandardCharsets.UTF_8).use { reader ->
                    val root = JsonParser.parseReader(reader).asJsonObject
                    val anim = root.getAsJsonObject("animation") ?: return defaultTimeline
                    val defaultTime = if (anim.has("frametime")) anim.get("frametime").asInt.coerceAtLeast(1) else 1
                    val framesEl = anim.get("frames") ?: return buildUniformTimeline(frameCount, defaultTime)
                    if (!framesEl.isJsonArray) return buildUniformTimeline(frameCount, defaultTime)

                    val out = ArrayList<Int>()
                    for (el in framesEl.asJsonArray) {
                        if (el.isJsonPrimitive && el.asJsonPrimitive.isNumber) {
                            val idx = el.asInt.coerceIn(0, frameCount - 1)
                            repeat(defaultTime) { out.add(idx) }
                        } else if (el.isJsonObject) {
                            val obj = el.asJsonObject
                            val idx = (if (obj.has("index")) obj.get("index").asInt else 0).coerceIn(0, frameCount - 1)
                            val time = (if (obj.has("time")) obj.get("time").asInt else defaultTime).coerceAtLeast(1)
                            repeat(time) { out.add(idx) }
                        }
                    }
                    if (out.isEmpty()) defaultTimeline else out.toIntArray()
                }
            }
        } catch (_: Exception) {
            defaultTimeline
        }
    }

    private fun buildUniformTimeline(frameCount: Int, frameTime: Int): IntArray {
        val out = IntArray(frameCount * frameTime)
        var cursor = 0
        for (f in 0 until frameCount) {
            repeat(frameTime) { out[cursor++] = f }
        }
        return out
    }

    private fun nearestBand(rgb: Int, refColors: IntArray): Int {
        val r = (rgb ushr 16) and 0xFF
        val g = (rgb ushr 8) and 0xFF
        val b = rgb and 0xFF
        var bestIdx = 0
        var bestDist = Double.MAX_VALUE
        for (i in refColors.indices) {
            val ref = refColors[i]
            val rr = (ref ushr 16) and 0xFF
            val rg = (ref ushr 8) and 0xFF
            val rb = ref and 0xFF
            val rm = (r + rr) * 0.5
            val dr = (r - rr).toDouble()
            val dg = (g - rg).toDouble()
            val db = (b - rb).toDouble()
            val dist = (2.0 + rm / 256.0) * dr * dr + 4.0 * dg * dg + (2.0 + (255.0 - rm) / 256.0) * db * db
            if (dist < bestDist) {
                bestDist = dist
                bestIdx = i
            }
        }
        return bestIdx
    }

    private fun buildFallbackTemplate(): MaskTemplate {
        val frame0 = IntArray(FALLBACK_SIZE * FALLBACK_SIZE) { idx ->
            val x = idx % FALLBACK_SIZE
            val y = idx / FALLBACK_SIZE
            val ch = FALLBACK_SHADE_ROWS[y][x]
            if (ch == '.') 0 else (255 shl 16) or ((ch - '0') shl 8)
        }
        return MaskTemplate(FALLBACK_SIZE, 1, 1, arrayOf(frame0), intArrayOf(0))
    }
}
