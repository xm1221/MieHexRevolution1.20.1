package cn.xm1221.MieHexRevolution.casting.actions.spells.item

import at.petrak.hexcasting.api.casting.ParticleSpray
import at.petrak.hexcasting.api.casting.RenderedSpell
import at.petrak.hexcasting.api.casting.castables.SpellAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.getEntity
import at.petrak.hexcasting.api.casting.getItemEntity
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.mishaps.MishapBadCaster
import at.petrak.hexcasting.api.casting.mishaps.MishapBadEntity
import at.petrak.hexcasting.api.casting.mishaps.MishapBadOffhandItem
import at.petrak.hexcasting.api.misc.MediaConstants
import cn.xm1221.MieHexRevolution.util.RendererHelper
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.ItemStack

/**
 * 物品外观伪装：只改看起来是什么。
 *
 * - [Target]：实体，改它主手（掉落物则改它本身），外观来源＝施法者副手
 * - [Self]：物品实体，改施法者副手物品，外观来源＝那个物品实体
 * - [Clear]：无参数，去掉副手物品的伪装
 *
 * Hex 没有物品类型的 iota，唯一能指代具体物品栈的是掉落物实体，所以 [Self] 要丢地上指。
 */
object OpItemDisguise {

    private val WANTED_ITEM_HOLDER: Component =
        Component.translatable("miehex_revolution.mishap.wanted.item_holder")
    private val WANTED_DISGUISE_SOURCE: Component =
        Component.translatable("miehex_revolution.mishap.wanted.disguise_source")
    private val WANTED_HELD_ITEM: Component =
        Component.translatable("miehex_revolution.mishap.wanted.held_item")

    private val COST: Long = MediaConstants.DUST_UNIT

    private fun casterOf(env: CastingEnvironment): ServerPlayer =
        env.castingEntity as? ServerPlayer ?: throw MishapBadCaster()

    /** 返回的是实体身上同一个对象引用，原地写 NBT 就等于改到了它。 */
    private fun resolveTargetStack(entity: Entity): ItemStack? = when (entity) {
        is ItemEntity -> entity.item
        is LivingEntity -> entity.getMainHandItem().takeUnless { it.isEmpty }
        else -> null
    }

    /** 原地改完 NBT 后把变化推给客户端。 */
    private fun syncHeldChange(entity: Entity, stack: ItemStack, slot: EquipmentSlot) {
        when (entity) {
            is ItemEntity -> entity.setItem(stack)
            is ServerPlayer -> entity.inventoryMenu.broadcastChanges()
            is LivingEntity -> entity.setItemSlot(slot, stack)
        }
    }

    object Target : SpellAction {
        override val argc: Int get() = 1

        override fun execute(args: List<Iota>, env: CastingEnvironment): SpellAction.Result {
            val entity = args.getEntity(0, argc)
            env.assertEntityInRange(entity)

            val caster = casterOf(env)
            val source = caster.getItemInHand(env.getOtherHand())
            val held = resolveTargetStack(entity) ?: throw MishapBadEntity(entity, WANTED_ITEM_HOLDER)
            if ( source.isEmpty) throw MishapBadOffhandItem(source, WANTED_DISGUISE_SOURCE)

            return SpellAction.Result(
                effect = object : RenderedSpell {
                    override fun cast(env: CastingEnvironment) {
                            RendererHelper.setDisguise(held, source)
                        syncHeldChange(entity, held, EquipmentSlot.MAINHAND)
                    }
                },
                cost = COST,
                particles = listOf(ParticleSpray.burst(entity.position(), 1.0, 12)),
            )
        }
    }

    object Self : SpellAction {
        override val argc: Int get() = 1

        override fun execute(args: List<Iota>, env: CastingEnvironment): SpellAction.Result {
            val sourceEntity = args.getItemEntity(0, argc)
            env.assertEntityInRange(sourceEntity)

            val caster = casterOf(env)
            val source = sourceEntity.item
            if (source.isEmpty) throw MishapBadEntity(sourceEntity, WANTED_DISGUISE_SOURCE)
            val target = caster.getItemInHand(env.getOtherHand())
            if (target.isEmpty) throw MishapBadOffhandItem(target, WANTED_HELD_ITEM)

            return SpellAction.Result(
                effect = object : RenderedSpell {
                    override fun cast(env: CastingEnvironment) {
                        if (RendererHelper.setDisguise(target, source)) {
                            caster.inventoryMenu.broadcastChanges()
                        }
                    }
                },
                cost = COST,
                particles = listOf(ParticleSpray.burst(caster.position(), 1.0, 12)),
            )
        }
    }

    object Clear : SpellAction {
        override val argc: Int get() = 0

        override fun execute(args: List<Iota>, env: CastingEnvironment): SpellAction.Result {
            val caster = casterOf(env)
            val target = caster.getItemInHand(env.getOtherHand())
            if (target.isEmpty) throw MishapBadOffhandItem(target, WANTED_HELD_ITEM)

            return SpellAction.Result(
                effect = object : RenderedSpell {
                    override fun cast(env: CastingEnvironment) {
                        if (RendererHelper.clearDisguise(target)) {
                            caster.inventoryMenu.broadcastChanges()
                        }
                    }
                },
                cost = MediaConstants.DUST_UNIT / 10,
                particles = listOf(ParticleSpray.burst(caster.position(), 1.0, 8)),
            )
        }
    }
}
