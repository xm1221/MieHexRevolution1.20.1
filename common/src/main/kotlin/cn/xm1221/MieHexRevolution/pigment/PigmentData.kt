package cn.xm1221.MieHexRevolution.pigment

import at.petrak.hexcasting.api.addldata.ADPigment
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.ListIota
import at.petrak.hexcasting.api.casting.iota.Vec3Iota
import at.petrak.hexcasting.api.pigment.ColorProvider
import cn.xm1221.MieHexRevolution.Miehex_revolution
import cn.xm1221.MieHexRevolution.casting.sky.SkyTintZone
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.DoubleTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.util.FastColor
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.phys.Vec3
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * CustomData-backed pigment definition for [cn.xm1221.MieHexRevolution.item.ItemCustomPigment].
 */
data class PigmentData(
    val colors: IntArray,
    val mode: Mode = Mode.MORPH,
    val speed: Float = DEFAULT_SPEED,
    val dir: Vec3 = DEFAULT_DIR,
    val mask: String = DEFAULT_MASK,
) {

    enum class Mode(val id: String) {
        SOLID("solid"),
        MORPH("morph"),
        STRIPE("stripe");

        companion object {
            fun fromId(id: String?): Mode? = values().firstOrNull { it.id == id }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PigmentData) return false
        return colors.contentEquals(other.colors) &&
            mode == other.mode &&
            speed == other.speed &&
            dir == other.dir &&
            mask == other.mask
    }

    override fun hashCode(): Int {
        var result = colors.contentHashCode()
        result = 31 * result + mode.hashCode()
        result = 31 * result + speed.hashCode()
        result = 31 * result + dir.hashCode()
        result = 31 * result + mask.hashCode()
        return result
    }

    companion object {
        const val TAG_ROOT = "${Miehex_revolution.MODID}:pigment"
        const val KEY_VERSION = "V"
        const val CURRENT_VERSION = 1
        const val KEY_COLORS = "C"
        const val KEY_MODE = "M"
        const val KEY_SPEED = "S"
        const val KEY_DIR = "D"
        const val KEY_MASK = "K"

        const val MAX_COLORS = 32
        const val MIN_SPEED = 0.05f
        const val MAX_SPEED = 10.0f
        const val DEFAULT_SPEED = 1.0f
        val DEFAULT_DIR = Vec3(0.1, 0.1, 0.1)
        const val DEFAULT_MASK = "auto"

        private val POOL_1 = listOf("dye_red", "default")
        private val POOL_2 = listOf("uuid", "pride_intersex")
        private val POOL_3 = listOf("pride_pansexual", "pride_transgender", "pride_bisexual", "pride_genderqueer", "pride_demiboy")
        private val POOL_4 = listOf("pride_agender", "pride_asexual", "pride_nonbinary", "pride_plural")
        private val POOL_5 = listOf("pride_gay", "pride_lesbian", "pride_aroace", "pride_aromantic", "pride_genderfluid")
        private val POOL_MULTI = listOf("pride_gay", "pride_aroace", "pride_nonbinary", "pride_pansexual", "uuid", "dye_red")

        val ALL_MASK_IDS: Set<String> = (POOL_1 + POOL_2 + POOL_3 + POOL_4 + POOL_5 + POOL_MULTI).toSet()

        fun poolForColorCount(count: Int): List<String> = when {
            count <= 1 -> POOL_1
            count == 2 -> POOL_2
            count == 3 -> POOL_3
            count == 4 -> POOL_4
            count == 5 -> POOL_5
            else -> POOL_MULTI
        }

        fun pickRandomMask(colorCount: Int, rng: Random = Random.Default): String {
            val pool = poolForColorCount(colorCount)
            return pool[rng.nextInt(pool.size)]
        }

        fun resolveMask(data: PigmentData): String {
            if (data.mask in ALL_MASK_IDS) return data.mask
            val pool = poolForColorCount(data.colors.size)
            val idx = Math.floorMod(data.colors.contentHashCode(), pool.size)
            return pool[idx]
        }

        const val DEFAULT_COLOR: Int = 0xFF9E9E9E.toInt()
        val DEFAULT = PigmentData(intArrayOf(DEFAULT_COLOR), Mode.SOLID)

        fun hasPigmentData(stack: ItemStack): Boolean {
            if (stack.isEmpty) return false
            val tag = stack.get(DataComponents.CUSTOM_DATA)?.copyTag() ?: return false
            if (!tag.contains(TAG_ROOT, Tag.TAG_COMPOUND.toInt())) return false
            val root = tag.getCompound(TAG_ROOT)
            return root.getIntArray(KEY_COLORS).isNotEmpty()
        }

        fun readOrNull(stack: ItemStack): PigmentData? {
            if (stack.isEmpty) return null
            val tag = stack.get(DataComponents.CUSTOM_DATA)?.copyTag() ?: return null
            if (!tag.contains(TAG_ROOT, Tag.TAG_COMPOUND.toInt())) return null
            val root = tag.getCompound(TAG_ROOT)
            val rawColors = root.getIntArray(KEY_COLORS)
            if (rawColors.isEmpty()) return null

            val count = rawColors.size.coerceAtMost(MAX_COLORS)
            val colors = IntArray(count) { i -> 0xFF000000.toInt() or (rawColors[i] and 0x00FFFFFF) }

            val defaultMode = if (colors.size == 1) Mode.SOLID else Mode.MORPH
            val mode = Mode.fromId(if (root.contains(KEY_MODE, Tag.TAG_STRING.toInt())) root.getString(KEY_MODE) else null)
                ?: defaultMode

            val rawSpeed = if (root.contains(KEY_SPEED, Tag.TAG_ANY_NUMERIC.toInt())) root.getFloat(KEY_SPEED) else DEFAULT_SPEED
            val speed = if (rawSpeed.isFinite()) rawSpeed.coerceIn(MIN_SPEED, MAX_SPEED) else DEFAULT_SPEED

            val dir = readDir(root) ?: DEFAULT_DIR

            val rawMask = if (root.contains(KEY_MASK, Tag.TAG_STRING.toInt())) root.getString(KEY_MASK) else ""
            val mask = if (rawMask.isNotBlank()) rawMask else DEFAULT_MASK

            return PigmentData(colors = colors, mode = mode, speed = speed, dir = dir, mask = mask)
        }

        fun read(stack: ItemStack): PigmentData = readOrNull(stack) ?: DEFAULT

        fun write(stack: ItemStack, data: PigmentData) {
            if (stack.isEmpty || data.colors.isEmpty()) return
            val count = data.colors.size.coerceAtMost(MAX_COLORS)
            val packedColors = IntArray(count) { i -> data.colors[i] and 0x00FFFFFF }

            val root = CompoundTag()
            root.putInt(KEY_VERSION, CURRENT_VERSION)
            root.putIntArray(KEY_COLORS, packedColors)
            root.putString(KEY_MODE, data.mode.id)

            val clampedSpeed = if (data.speed.isFinite()) data.speed.coerceIn(MIN_SPEED, MAX_SPEED) else DEFAULT_SPEED
            if (clampedSpeed != DEFAULT_SPEED) {
                root.putFloat(KEY_SPEED, clampedSpeed)
            }
            if (data.dir != DEFAULT_DIR && data.dir.x.isFinite() && data.dir.y.isFinite() && data.dir.z.isFinite()) {
                val dirTag = ListTag()
                dirTag.add(DoubleTag.valueOf(data.dir.x))
                dirTag.add(DoubleTag.valueOf(data.dir.y))
                dirTag.add(DoubleTag.valueOf(data.dir.z))
                root.put(KEY_DIR, dirTag)
            }
            if (data.mask.isNotBlank() && data.mask != DEFAULT_MASK) {
                root.putString(KEY_MASK, data.mask)
            }

            CustomData.update(DataComponents.CUSTOM_DATA, stack) { it.put(TAG_ROOT, root) }
        }

        private fun readDir(root: CompoundTag): Vec3? {
            if (!root.contains(KEY_DIR, Tag.TAG_LIST.toInt())) return null
            val list = root.getList(KEY_DIR, Tag.TAG_DOUBLE.toInt())
            if (list.size != 3) return null
            val x = list.getDouble(0)
            val y = list.getDouble(1)
            val z = list.getDouble(2)
            if (!x.isFinite() || !y.isFinite() || !z.isFinite()) return null
            return Vec3(x, y, z)
        }

        fun vecToArgb(raw: Vec3): Int {
            val n = SkyTintZone.normalizeColor(raw)
            val r = (n.x * 255.0).roundToInt().coerceIn(0, 255)
            val g = (n.y * 255.0).roundToInt().coerceIn(0, 255)
            val b = (n.z * 255.0).roundToInt().coerceIn(0, 255)
            return FastColor.ARGB32.color(255, r, g, b)
        }

        fun argbToVec(argb: Int): Vec3 = Vec3(
            FastColor.ARGB32.red(argb) / 255.0,
            FastColor.ARGB32.green(argb) / 255.0,
            FastColor.ARGB32.blue(argb) / 255.0,
        )

        fun colorsToIota(colors: IntArray): Iota {
            if (colors.isEmpty()) return Vec3Iota(argbToVec(DEFAULT_COLOR))
            if (colors.size == 1) return Vec3Iota(argbToVec(colors[0]))
            return ListIota(colors.map { Vec3Iota(argbToVec(it)) })
        }
    }
}

/**
 * LRU cache of immutable [ColorProvider] instances keyed by [PigmentData] so high-frequency
 * vertex rendering never parses CustomData or allocates arrays per call.
 */
object PigmentProviderCache {
    private const val MAX_ENTRIES = 32

    private val cache = object : LinkedHashMap<PigmentData, ColorProvider>(MAX_ENTRIES, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<PigmentData, ColorProvider>): Boolean =
            size > MAX_ENTRIES
    }

    @Synchronized
    fun getOrBuild(data: PigmentData): ColorProvider =
        cache.getOrPut(data) { NbtColorProvider(data.colors.copyOf(), data.mode, data.speed, data.dir) }

    private class NbtColorProvider(
        private val colors: IntArray,
        private val mode: PigmentData.Mode,
        private val speed: Float,
        private val dir: Vec3,
    ) : ColorProvider() {
        override fun getRawColor(time: Float, position: Vec3): Int {
            if (colors.isEmpty()) return PigmentData.DEFAULT_COLOR
            if (colors.size == 1 || mode == PigmentData.Mode.SOLID) return colors[0]
            return ADPigment.morphBetweenColors(colors, dir, time / (400f / speed), position)
        }
    }
}
