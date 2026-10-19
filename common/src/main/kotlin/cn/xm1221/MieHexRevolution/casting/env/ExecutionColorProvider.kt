package cn.xm1221.MieHexRevolution.casting.env

/**
 * 自定义执行色提供者：当施法环境不是普通的 StaffCastEnv（而是
 * FakeCastingEnv / PlayersCastingEnv 等替代环境）时，提供替代默认执行蓝的颜色。
 */
interface ExecutionColorProvider {
    /** 执行色（ARGB），用于替换默认的执行蓝 */
    fun getExecutionColor(): Int

    companion object {
        /** 默认执行蓝（0 = 使用 HexMod 默认颜色） */
        const val DEFAULT: Int = 0
        /** FakeCastingEnv（代替生物施法）的执行色：暗红 */
        const val FAKE_COLOR: Int = 0xFFE53935.toInt()
        /** PlayersCastingEnv（代替玩家施法）的执行色：绿色 */
        const val PLAYERS_COLOR: Int = 0xFF43A047.toInt()
    }
}
