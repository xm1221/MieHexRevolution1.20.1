package cn.xm1221.MieHexRevolution.util;

/**
 * Duck interface implemented on {@link net.minecraft.world.entity.player.Player} by
 * {@code MixinPlayerSyncedData}，用于在 mixin 之外访问玩家的执行色同步数据。
 *
 * <p>EntityDataAccessor 必须注册在 Player 的静态初始化阶段（见 mixin 里的
 * {@code @Unique static final} 字段）：1.21 的 SynchedEntityData.Builder 在构造时
 * 就按 {@code ClassTreeIdRegistry.getCount(Player.class)} 固定了数组长度，
 * 任何在首个 Player 实例创建之后才 defineId 的 accessor 都会越界。</p>
 */
public interface RemoteCastColorHolder {
    /** 读取执行色（ARGB），0 表示默认执行蓝。 */
    int miehex$getExecutionColor();

    /** 写入执行色（ARGB）。 */
    void miehex$setExecutionColor(int color);
}
