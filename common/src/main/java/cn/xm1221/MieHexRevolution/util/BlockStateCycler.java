package cn.xm1221.MieHexRevolution.util;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.ArrayList;
import java.util.List;

/**
 * 泛型桥。
 *
 * <p>{@code Property<T extends Comparable<T>>} 在 Kotlin 里没有办法对 {@code Property<*>}
 * 做 unchecked 调用：星投影会丢掉 {@code T extends Comparable<T>} 这个自引用约束，
 * 写成 {@code Property<Comparable<Any>>} 又满足不了上界。所以循环属性的活统一放这里，
 * 由 {@link #cycleRaw} 在擦除后的类型上做一次 unchecked 强转，运行时安全
 * （{@code BlockState#setValue} 只按属性对象与取值查表）。
 */
public final class BlockStateCycler {

    private BlockStateCycler() {
    }

    /**
     * 把 {@code state} 上的 {@code property} 推进 {@code step} 格，越界回绕。
     *
     * @param step 步长，{@code rotation} 这类 16 值属性给 4 就能四向循环
     */
    public static <T extends Comparable<T>> BlockState cycle(BlockState state, Property<T> property, int step) {
        List<T> values = new ArrayList<>(property.getPossibleValues());
        if (values.size() < 2) {
            return state;
        }
        int index = values.indexOf(state.getValue(property));
        if (index < 0) {
            return state;
        }
        int size = values.size();
        return state.setValue(property, values.get(Math.floorMod(index + step, size)));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static BlockState cycleRaw(BlockState state, Property<?> property, int step) {
        return cycle(state, (Property) property, step);
    }
}
