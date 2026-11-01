package cn.xm1221.MieHexRevolution.util

import at.petrak.hexcasting.api.utils.getCompound
import at.petrak.hexcasting.api.utils.putCompound
import cn.xm1221.MieHexRevolution.Miehex_revolution
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack

/**
 * 物品外观伪装的读写中枢。数据挂在物品 NBT 上：
 * `miehex_revolution:faker: { miehex_revolution:item: <伪装物品> }`
 *
 * 刻意保留的副作用：伪装数据参与 `ItemStack.isSameItemSameTags`，所以伪装过的物品
 * 与同类堆不叠、进合成台也不匹配。别当 bug 修。
 */
object RendererHelper {

    const val FAKER_TAG: String = Miehex_revolution.MODID + ":faker"
    const val ITEM_TAG: String = Miehex_revolution.MODID + ":item"

    const val MAX_DEPTH: Int = 4

    fun hasDisguise(stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        val faker = stack.getCompound(FAKER_TAG) ?: return false
        return faker.getCompound(ITEM_TAG) != null
    }

    /** 没有伪装时原样返回同一个对象，调用方可以靠 `===` 判断。 */
    fun getFaker(stack: ItemStack): ItemStack {
        if (stack.isEmpty) return stack
        var current = stack
        var depth = 0
        while (depth++ < MAX_DEPTH) {
            val faker = current.getCompound(FAKER_TAG) ?: break
            val itemTag = faker.getCompound(ITEM_TAG) ?: break
            val next = ItemStack.of(itemTag)
            if (next.isEmpty) break
            current = next
        }
        return current
    }

    /** 源物品本身带伪装时取它最终的外观，所以永远只有一层。 */
    fun setDisguise(target: ItemStack, source: ItemStack): Boolean {
        if (target.isEmpty || source.isEmpty) return false
        val appearance = getFaker(source)
        if (appearance.isEmpty) return false
        val faker = CompoundTag()
        faker.putCompound(ITEM_TAG, appearance.save(CompoundTag()))
        target.putCompound(FAKER_TAG, faker)
        return true
    }

    fun clearDisguise(target: ItemStack): Boolean {
        if (target.isEmpty) return false
        if (!hasDisguise(target)) return false
        target.removeTagKey(FAKER_TAG)
        return true
    }
}
