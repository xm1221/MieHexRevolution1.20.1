package cn.xm1221.MieHexRevolution.util

import at.petrak.hexcasting.api.casting.eval.CastResult
import at.petrak.hexcasting.api.casting.eval.ResolvedPatternType
import at.petrak.hexcasting.api.casting.eval.sideeffects.OperatorSideEffect
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage
import at.petrak.hexcasting.api.casting.eval.vm.CastingVM
import at.petrak.hexcasting.api.casting.eval.vm.ContinuationFrame
import at.petrak.hexcasting.api.casting.eval.vm.FrameEvaluate
import at.petrak.hexcasting.api.casting.eval.vm.FrameFinishEval
import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.IotaType
import at.petrak.hexcasting.api.casting.iota.ListIota
import at.petrak.hexcasting.api.casting.iota.PatternIota
import at.petrak.hexcasting.api.casting.math.HexPattern
import at.petrak.hexcasting.api.casting.mishaps.Mishap
import at.petrak.hexcasting.api.casting.mishaps.MishapEvalTooMuch
import at.petrak.hexcasting.common.blocks.akashic.AkashicFloodfiller
import at.petrak.hexcasting.common.blocks.akashic.BlockAkashicRecord
import at.petrak.hexcasting.common.blocks.akashic.BlockEntityAkashicBookshelf
import at.petrak.hexcasting.common.lib.hex.HexEvalSounds
import cn.xm1221.MieHexRevolution.api.casting.iota.ImportsIota
import cn.xm1221.MieHexRevolution.networking.Miehex_revolutionNetworking
import cn.xm1221.MieHexRevolution.networking.msg.MsgSyncImportKeysS2C
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer

/**
 * If the current cast has an [ImportsIota] bound in its user data and [this] pattern is one of its
 * keys, evaluate to the bound value instead of the pattern's usual action. Returns `null` to signal
 * that the normal pattern execution should proceed.
 *
 * Firing an import consumes one op, and we apply the same op limit a normal cast has
 * ([at.petrak.hexcasting.api.casting.eval.CastingEnvironment.maxOpCount], MishapEvalTooMuch).
 * In 0.11.3 that check only runs inside `PatternIota.lookupAndOperate` for the normal action path,
 * which this import path never reaches -- so without the check here, a bound function that
 * (transitively) calls its own key would grow the continuation forever and exhaust the heap.
 */
fun PatternIota.executeWithImports(vm: CastingVM?, world: ServerLevel?, continuation: SpellContinuation?): CastResult? {
    if (vm == null || world == null || continuation == null) return null
    val img = vm.image
    val userdata = img.userData
    // Fast path: this cast never defined any imports. `CompoundTag.getCompound` returns an empty tag
    // (not null) for a missing key, so we must check containment before deserializing.
    if (!userdata.contains("imports")) {
        syncImportKeys(vm, emptyList())
        return null
    }
    if (userdata.contains("run_raw") && userdata.getBoolean("run_raw")) {
        // Raw mode: imports are bypassed and patterns run their native action, so there is
        // nothing to mark -- clear the client's import-key set (per-stroke sync picks this
        // up immediately, and the keys come back once raw is toggled off).
        syncImportKeys(vm, emptyList())
        return null
    }
    val deserialized = IotaType.deserialize(userdata.getCompound("imports"), world)
    if (deserialized !is ImportsIota) return null
    syncImportKeys(vm, deserialized.imports.keys.map { importKeyString(it) })
    val funs = deserialized.imports[this.pattern] ?: return null

    val frameEvaluate: ContinuationFrame
    val newimg: CastingImage
    when {
        funs is ListIota -> {
            // evaluate the imported function body as a sub-spell
            frameEvaluate = FrameEvaluate(funs.list, true)
            newimg = img
        }

        !funs.executable() -> {
            // plain datum: push it onto the stack (empty sub-eval keeps the resolution shape)
            frameEvaluate = FrameEvaluate(ListIota(listOf()).list, true)
            newimg = img.copy(stack = img.stack.plus(funs))
        }

        else -> {
            // single executable iota (e.g. another pattern): evaluate it
            frameEvaluate = FrameEvaluate(ListIota(listOf(funs)).list, true)
            newimg = img
        }
    }
    var newcont = continuation.pushFrame(FrameFinishEval)
    newcont = newcont.pushFrame(frameEvaluate)
    // Consume one op per fired import (same accounting as a normal ConstMediaAction), then enforce
    // the env op limit ourselves -- see the KDoc above for why hexmod's own check cannot reach here.
    val newimg2 = newimg.copy(opsConsumed = newimg.opsConsumed + 1)
    if (newimg2.opsConsumed > vm.env.maxOpCount()) {
        return CastResult(
            cast = this,
            continuation = continuation,
            newData = null,
            sideEffects = listOf(
                OperatorSideEffect.DoMishap(
                    MishapEvalTooMuch(),
                    Mishap.Context(this.pattern, null)
                )
            ),
            resolutionType = ResolvedPatternType.ERRORED,
            sound = HexEvalSounds.MISHAP,
        )
    }
    return CastResult(
        cast = this,
        continuation = newcont,
        newData = newimg2,
        sideEffects = listOf(),
        resolutionType = ResolvedPatternType.EVALUATED,
        sound = HexEvalSounds.NORMAL_EXECUTE
    )
}

/**
 * Stable string key for one import binding key (start dir + angle signature), used both for
 * the S2C sync and, with the same encoding, by `MixinGuiSpellcasting` on the client.
 */
fun importKeyString(pattern: HexPattern): String =
    "${pattern.startDir.ordinal}:${pattern.anglesSignature()}"

/** Tell the casting player which import keys are currently active (empty = none). */
private fun syncImportKeys(vm: CastingVM, keys: List<String>) {
    val player = vm.env.castingEntity as? ServerPlayer ?: return
    Miehex_revolutionNetworking.CHANNEL.sendToPlayer(player, MsgSyncImportKeysS2C(keys))
}

/** Flood-fill search radius used by BlockAkashicRecord itself (mirrors its calls to AkashicFloodfiller). */
private const val AKASHIC_SEARCH_RANGE = 128

/**
 * All patterns currently stored in this Akashic Record's connected bookshelves.
 * Requires [pos] to be the record's own block position; walks the flood-fill region
 * (blocks implementing [AkashicFloodfiller]) and collects every occupied shelf.
 */
fun BlockAkashicRecord.getAllpatterns(world: ServerLevel, pos: BlockPos): List<PatternIota> =
    getMaps(world, pos).keys.map { PatternIota(it) }

/**
 * Every pattern -> iota mapping stored in this Akashic Record's connected bookshelves,
 * as a plain [Map] ready to be handed to e.g. [ImportsIota]. Empty shelves are skipped;
 * the search radius matches the record block's own flood-fill (128 blocks).
 */
fun BlockAkashicRecord.getMaps(world: ServerLevel, pos: BlockPos): Map<HexPattern, Iota> {
    val result = HashMap<HexPattern, Iota>()
    val seen = HashSet<BlockPos>()
    val todo = ArrayDeque<BlockPos>()
    todo.add(pos)
    seen.add(pos)
    while (todo.isNotEmpty()) {
        val here = todo.removeFirst()
        for (dir in Direction.values()) {
            val neighbor = here.relative(dir)
            if (neighbor.distSqr(pos) > (AKASHIC_SEARCH_RANGE * AKASHIC_SEARCH_RANGE).toDouble()) continue
            if (!seen.add(neighbor)) continue
            val tile = world.getBlockEntity(neighbor)
            if (tile is BlockEntityAkashicBookshelf) {
                val pattern = tile.pattern
                val tag = tile.iotaTag
                if (pattern != null && tag != null) {
                    result[pattern] = IotaType.deserialize(tag, world)
                }
            }
            if (world.getBlockState(neighbor).block is AkashicFloodfiller) {
                todo.add(neighbor)
            }
        }
    }
    return result
}
