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
        .isViewBlocking { _, _, _ -> false }
)
