package cn.xm1221.MieHexRevolution.client

import at.petrak.hexcasting.api.casting.eval.ResolvedPattern
import java.util.Collections
import java.util.IdentityHashMap

/**
 * 当前施法界面里「这一笔命中了导入键」的图案。
 * 用实例身份而不是形状：同形状的其它笔、已经画过的老图案都不变色。
 * [seed] 是 `drawPatternFromPoints` 的下标，正在绘制的图案会越界。
 */
object PatternStrokeMarks {
    private val marked: MutableSet<ResolvedPattern> =
        Collections.newSetFromMap(IdentityHashMap())
    private var patterns: List<ResolvedPattern>? = null

    @JvmStatic
    fun bind(current: List<ResolvedPattern>) {
        patterns = current
        if (marked.isEmpty()) return
        val live: MutableSet<ResolvedPattern> = Collections.newSetFromMap(IdentityHashMap())
        live.addAll(current)
        marked.retainAll(live)
    }

    @JvmStatic
    fun mark(pattern: ResolvedPattern) {
        marked.add(pattern)
    }

    fun isMarked(seed: Double): Boolean {
        val list = patterns ?: return false
        val index = seed.toInt()
        if (index < 0 || index >= list.size) {
            return false
        }
        return list[index] in marked
    }
}
