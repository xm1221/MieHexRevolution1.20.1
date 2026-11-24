package cn.xm1221.MieHexRevolution.util

import cn.xm1221.MieHexRevolution.Miehex_revolution
import net.minecraft.core.HolderLookup
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData

/**
 * 物品外观伪装的读写中枢。数据挂在物品的自定义数据上：
 * `miehex_revolution:faker: { miehex_revolution:item: <伪装物品> }`
 *
 * 刻意保留的副作用：伪装数据参与 `ItemStack.isSameItemSameComponents`，所以伪装过的物品
 * 与同类堆不叠、进合成台也不匹配。别当 bug 修。
 *
 * 1.21 起物品的自定义 NBT 存在 [DataComponents.CUSTOM_DATA] 组件里，而物品本身的读写
 * （[ItemStack.save] / [ItemStack.parse]）需要注册表来解析数据驱动组件（附魔、药水等），
 * 所以这两个入口都要传 [HolderLookup.Provider]。服务端用 `level.registryAccess()`，
 * 客户端用 `Minecraft.getInstance().level?.registryAccess()`；拿不到时按“没有伪装”处理，
 * 宁可少画一次也不抛异常。
 */
object RendererHelper {

    const val FAKER_TAG: String = Miehex_revolution.MODID + ":faker"
    const val ITEM_TAG: String = Miehex_revolution.MODID + ":item"

    const val MAX_DEPTH: Int = 4

    fun hasDisguise(stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        val faker = customTag(stack)?.getCompound(FAKER_TAG) ?: return false
        return faker.contains(ITEM_TAG)
    }

    /** 没有伪装时原样返回同一个对象，调用方可以靠 `===` 判断。 */
    fun getFaker(stack: ItemStack, registries: HolderLookup.Provider?): ItemStack {
        if (stack.isEmpty) return stack
        if (registries == null) return stack
        var current = stack
        var depth = 0
        while (depth++ < MAX_DEPTH) {
            val faker = customTag(current)?.getCompound(FAKER_TAG) ?: break
            if (!faker.contains(ITEM_TAG)) break
            val next = ItemStack.parse(registries, faker.getCompound(ITEM_TAG)).orElse(ItemStack.EMPTY)
            if (next.isEmpty) break
            current = next
        }
        return current
    }

    /** 源物品本身带伪装时取它最终的外观，所以永远只有一层。 */
    fun setDisguise(target: ItemStack, source: ItemStack, registries: HolderLookup.Provider?): Boolean {
        if (target.isEmpty || source.isEmpty) return false
        if (registries == null) return false
        val appearance = getFaker(source, registries)
        if (appearance.isEmpty) return false
        val saved = appearance.save(registries) as? CompoundTag ?: return false
        val faker = CompoundTag()
        faker.put(ITEM_TAG, saved)
        val tag = customTag(target) ?: CompoundTag()
        tag.put(FAKER_TAG, faker)
        target.set(DataComponents.CUSTOM_DATA, CustomData.of(tag))
        return true
    }

    fun clearDisguise(target: ItemStack): Boolean {
        if (target.isEmpty) return false
        val tag = customTag(target) ?: return false
        if (!tag.contains(FAKER_TAG)) return false
        tag.remove(FAKER_TAG)
        if (tag.isEmpty) {
            target.remove(DataComponents.CUSTOM_DATA)
        } else {
            target.set(DataComponents.CUSTOM_DATA, CustomData.of(tag))
        }
        return true
    }

    /** 物品自定义数据的**副本**；没有则 null。（就地改副本，调用方自己写回。） */
    private fun customTag(stack: ItemStack): CompoundTag? {
        val custom = stack.get(DataComponents.CUSTOM_DATA) ?: return null
        return custom.copyTag()
    }
}
