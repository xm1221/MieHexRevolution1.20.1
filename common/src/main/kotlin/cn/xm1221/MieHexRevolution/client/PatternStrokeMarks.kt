package cn.xm1221.MieHexRevolution.client

import at.petrak.hexcasting.api.casting.eval.ResolvedPattern
import java.util.Collections
import java.util.IdentityHashMap

/**
 * 当前施法界面里「这一笔命中了导入键」的图案。
 * 用实例身份而不是形状：同形状的其它笔、已经画过的老图案都不变色。
 */
object PatternStrokeMarks {
    private val marked: MutableSet<ResolvedPattern> =
        Collections.newSetFromMap(IdentityHashMap())

    @JvmStatic
    fun mark(pattern: ResolvedPattern, currentPatterns: List<ResolvedPattern>) {
        if (marked.isNotEmpty()) {
            val live: MutableSet<ResolvedPattern> = Collections.newSetFromMap(IdentityHashMap())
            live.addAll(currentPatterns)
            marked.retainAll(live)
        }
        marked.add(pattern)
    }

    fun isMarked(pattern: ResolvedPattern?): Boolean = pattern != null && pattern in marked
}
