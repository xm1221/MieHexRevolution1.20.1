package cn.xm1221.MieHexRevolution.block.idea

import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.state.BlockBehaviour

/**
 * 理念方块本体。
 *
 * <p>[BlockBehaviour.Properties.dynamicShape] 是必须的：形状、碰撞、受光都取决于方块实体里
 * 存的材质，而原版会为「静态」方块缓存 BlockStateBase.Cache（碰撞形状、光照遮挡、是否实心
 * 全在这一份缓存里），缓存一旦建立就不会再跟着材质变。标成动态形状后缓存被跳过，
 * 每次查询都会走回下面那些 override。
 */
class IdeaFullBlock : IdeaBlock(
    BlockBehaviour.Properties.of()
        .strength(2f, 6f)
        .sound(SoundType.AMETHYST)
        .noOcclusion()
        .dynamicShape()
        .isRedstoneConductor { _, _, _ -> false }
        .isSuffocating { _, _, _ -> false }
        // 环境光遮蔽读的是 isViewBlocking + getLightBlock 这两个判据。原版里两者都由
        // isSolidRender 一个来源决定（默认 isViewBlocking 就是 isSolidRender，
        // 而默认 getLightBlock 也是在实心时给满 15），所以“照进 15”正是“像实心方块一样”的
        // 等价说法。只让挡光跟随材质、这两个判据各自为政的话，AO 会把这个光为 0 的格子
        // 当成“看得见的暗格”平均进邻居顶点的亮度里，方块四周就糊出一圈多余的暗影。
        // 两个判据都取自方块状态，见 [IdeaBlock.LIGHT]。
        .isViewBlocking { state, _, _ -> state.getValue(IdeaBlock.LIGHT) >= 15 }
)
