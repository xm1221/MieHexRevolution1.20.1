package cn.xm1221.MieHexRevolution.block.idea

import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.state.BlockBehaviour

class IdeaFullBlock : IdeaBlock(
    BlockBehaviour.Properties.of()
        .strength(2f, 6f)
        .sound(SoundType.AMETHYST)
        .noOcclusion()
        .isRedstoneConductor { _, _, _ -> false }
        .isSuffocating { _, _, _ -> false }
        .isViewBlocking { _, _, _ -> false }
)
