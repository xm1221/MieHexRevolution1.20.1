package cn.xm1221.MieHexRevolution.pigment

import at.petrak.hexcasting.api.casting.iota.EntityIota
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.ListIota
import at.petrak.hexcasting.api.casting.iota.Vec3Iota
import at.petrak.hexcasting.api.casting.mishaps.MishapBadOffhandItem
import at.petrak.hexcasting.api.pigment.FrozenPigment
import at.petrak.hexcasting.common.items.pigment.ItemAmethystAndCopperPigment
import at.petrak.hexcasting.common.items.pigment.ItemAmethystPigment
import at.petrak.hexcasting.common.items.pigment.ItemDyePigment
import at.petrak.hexcasting.common.items.pigment.ItemPridePigment
import at.petrak.hexcasting.common.items.pigment.ItemUUIDPigment
import at.petrak.hexcasting.xplat.IXplatAbstractions
import cn.xm1221.MieHexRevolution.api.casting.mishap.MishapTooManyPigmentColors
import cn.xm1221.MieHexRevolution.item.ItemCustomPigment
import cn.xm1221.MieHexRevolution.mixin.AccessorPridePigmentType
import cn.xm1221.MieHexRevolution.mixin.AccessorUUIDPigmentProvider
import cn.xm1221.MieLib.worker.CastingVMCall
import net.minecraft.Util
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.decoration.ItemFrame
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3
import java.util.UUID

/**
 * Extracts color iotas from any Hex Casting pigment (or addon pigment) and handles
 * chromatic-overload mishap detection for [ItemCustomPigment].
 */
object PigmentColorExtractor {

    private val ANCIENT_COLORS = intArrayOf(
        0xFF54398A.toInt(),
        0xFFCFA0F3.toInt(),
        0xFFFECBE6.toInt(),
        0xFFCFA0F3.toInt(),
        0xFFE77C56.toInt(),
    )

    private const val AMETHYST_COLOR = 0xFFAB65EB.toInt()
    private val SAMPLE_TIMES = floatArrayOf(0f, 100f, 200f, 300f)

    private val CASTER_UUID_STACK: ThreadLocal<ArrayDeque<Pair<CastingVMCall, UUID>>> =
        ThreadLocal.withInitial { ArrayDeque() }

    fun pushCasterUuid(call: CastingVMCall, uuid: UUID) {
        CASTER_UUID_STACK.get().addLast(call to uuid)
    }

    fun popCasterUuid(call: CastingVMCall) {
        val deque = CASTER_UUID_STACK.get()
        while (deque.isNotEmpty() && deque.last().first === call) {
            deque.removeLast()
        }
        if (deque.isEmpty()) {
            CASTER_UUID_STACK.remove()
        }
    }

    fun currentCasterUuid(): UUID =
        CASTER_UUID_STACK.get().lastOrNull()?.second ?: Util.NIL_UUID

    fun extractColors(stack: ItemStack, owner: UUID): IntArray {
        val item = stack.item
        return when (item) {
            is ItemCustomPigment -> PigmentData.read(stack).colors.copyOf()
            is ItemDyePigment -> intArrayOf(0xFF000000.toInt() or item.dyeColor.textColor)
            is ItemAmethystPigment -> intArrayOf(AMETHYST_COLOR)
            is ItemAmethystAndCopperPigment -> ANCIENT_COLORS.copyOf()
            is ItemPridePigment -> (item.type as AccessorPridePigmentType).`miehex_revolution$getColors`().copyOf()
            is ItemUUIDPigment -> {
                val provider = item.provideColor(stack, owner)
                if (provider is AccessorUUIDPigmentProvider) {
                    provider.`miehex_revolution$getColors`().copyOf()
                } else {
                    sampleFallback(stack, owner)
                }
            }
            else -> sampleFallback(stack, owner)
        }
    }

    private fun sampleFallback(stack: ItemStack, owner: UUID): IntArray {
        val provider = IXplatAbstractions.INSTANCE.getColorProvider(FrozenPigment(stack, owner))
        val samples = IntArray(SAMPLE_TIMES.size) { i -> provider.getColor(SAMPLE_TIMES[i], Vec3.ZERO) }
        return if (samples.all { it == samples[0] }) {
            intArrayOf(samples[0])
        } else {
            samples.distinct().toIntArray()
        }
    }

    fun readColorIota(stack: ItemStack, owner: UUID): Iota =
        PigmentData.colorsToIota(extractColors(stack, owner))

    private fun extractEntityItem(entity: Entity): ItemStack? = when (entity) {
        is ItemEntity -> entity.item
        is ItemFrame -> entity.item
        else -> null
    }

    fun throwIfTooManyColors(args: List<Iota>, mishap: MishapBadOffhandItem) {
        val stack = mishap.item ?: return
        throwIfStackTooManyColors(stack, args.firstOrNull())
    }

    fun throwIfEntityTooManyColors(args: List<Iota>) {
        val entity = (args.firstOrNull() as? EntityIota)?.entity ?: return
        val stack = extractEntityItem(entity) ?: return
        throwIfStackTooManyColors(stack, args.getOrNull(1))
    }

    private fun throwIfStackTooManyColors(stack: ItemStack, datum: Iota?) {
        if (stack.isEmpty || stack.item !is ItemCustomPigment) return
        if (PigmentData.hasPigmentData(stack)) return
        val listIota = datum as? ListIota ?: return
        val size = listIota.list.size()
        if (size <= PigmentData.MAX_COLORS) return
        for (elem in listIota.list) {
            if (elem !is Vec3Iota) return
            val v = elem.vec3
            if (!v.x.isFinite() || !v.y.isFinite() || !v.z.isFinite()) return
        }
        throw MishapTooManyPigmentColors(size, PigmentData.MAX_COLORS)
    }
}
