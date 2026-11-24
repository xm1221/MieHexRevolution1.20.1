package cn.xm1221.MieHexRevolution.casting.actions.spells.sky

import at.petrak.hexcasting.api.casting.ParticleSpray
import at.petrak.hexcasting.api.casting.RenderedSpell
import at.petrak.hexcasting.api.casting.castables.SpellAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.getDoubleBetween
import at.petrak.hexcasting.api.casting.getNumOrVec
import at.petrak.hexcasting.api.casting.getPositiveDoubleUnderInclusive
import at.petrak.hexcasting.api.casting.getPositiveIntUnderInclusive
import at.petrak.hexcasting.api.casting.getVec3
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.misc.MediaConstants
import cn.xm1221.MieHexRevolution.casting.sky.SkyTintZone
import cn.xm1221.MieHexRevolution.networking.msg.MsgClearSkyTintS2C
import cn.xm1221.MieHexRevolution.networking.msg.MsgSkyTintS2C
import net.minecraft.world.phys.Vec3

/**
 * 天空染色，作用于一定范围内的天空与云朵。
 *
 * - [Cast]：颜色, 半径, 持续刻数, 强度，球心＝施法者
 * - [CastAt]：颜色, 半径, 持续刻数, 强度, 球心
 * - [Clear]：无参数，清空该维度全部
 *
 * 颜色接受向量或单值（灰度）；分量都 ≤ 1 按 0..1 解释，否则按 0..255。
 */
object OpSkyTint {

    private fun colorAt(args: List<Iota>, idx: Int, argc: Int): Vec3 {
        val either = args.getNumOrVec(idx, argc)
        val vec = either.right().orElse(null)
        val grey = either.left().orElse(0.0)
        return SkyTintZone.normalizeColor(vec ?: Vec3(grey, grey, grey))
    }

    /** 单位：紫水晶粉（1 粉 = DUST_UNIT）。 */
    private fun costOf(radius: Double, durationTicks: Int, strength: Double): Long {
        val base = 0.5 + radius * 0.1 + durationTicks / 20.0 * 0.2
        val dust = base * (0.5 + strength.coerceIn(0.0, 1.0) * 0.5)
        return (dust * MediaConstants.DUST_UNIT).toLong().coerceAtLeast(1L)
    }

    private fun spellFor(
        center: Vec3,
        radius: Double,
        color: Vec3,
        durationTicks: Int,
        strength: Double,
    ): SpellAction.Result =
        SpellAction.Result(
            effect = object : RenderedSpell {
                override fun cast(env: CastingEnvironment) {
                    MsgSkyTintS2C(
                        dimension = env.world.dimension().location(),
                        center = center,
                        radius = radius,
                        color = color,
                        durationTicks = durationTicks,
                        strength = strength,
                    ).sendToPlayers(env.world.players())
                }
            },
            cost = costOf(radius, durationTicks, strength),
            particles = listOf(ParticleSpray.burst(center, radius.coerceAtMost(4.0), 24)),
        )

    object Cast : SpellAction {
        override val argc: Int get() = 4

        override fun execute(args: List<Iota>, env: CastingEnvironment): SpellAction.Result {
            val color = colorAt(args, 0, argc)
            val radius = args.getPositiveDoubleUnderInclusive(1, SkyTintZone.MAX_RADIUS, argc)
            val duration = args.getPositiveIntUnderInclusive(2, SkyTintZone.MAX_DURATION_TICKS, argc)
            val strength = args.getDoubleBetween(3, 0.0, 1.0, argc)
            val center = env.castingEntity?.position() ?: Vec3.ZERO
            return spellFor(center, radius, color, duration, strength)
        }
    }

    object CastAt : SpellAction {
        override val argc: Int get() = 5

        override fun execute(args: List<Iota>, env: CastingEnvironment): SpellAction.Result {
            val color = colorAt(args, 0, argc)
            val radius = args.getPositiveDoubleUnderInclusive(1, SkyTintZone.MAX_RADIUS, argc)
            val duration = args.getPositiveIntUnderInclusive(2, SkyTintZone.MAX_DURATION_TICKS, argc)
            val strength = args.getDoubleBetween(3, 0.0, 1.0, argc)
            val center = args.getVec3(4, argc)
            env.assertVecInRange(center)
            return spellFor(center, radius, color, duration, strength)
        }
    }

    object Clear : SpellAction {
        override val argc: Int get() = 0

        override fun execute(args: List<Iota>, env: CastingEnvironment): SpellAction.Result {
            val center = env.castingEntity?.position() ?: Vec3.ZERO
            return SpellAction.Result(
                effect = object : RenderedSpell {
                    override fun cast(env: CastingEnvironment) {
                        MsgClearSkyTintS2C(
                            dimension = env.world.dimension().location(),
                            center = center,
                            radius = 0.0,
                        ).sendToPlayers(env.world.players())
                    }
                },
                cost = MediaConstants.DUST_UNIT / 10,
                particles = listOf(ParticleSpray.burst(center, 1.5, 12)),
            )
        }
    }
}
