package cn.xm1221.MieHexRevolution.casting.actions.useful

import at.petrak.hexcasting.api.casting.castables.ConstMediaAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.getInt
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.mishaps.MishapBadCaster
import at.petrak.hexcasting.api.casting.mishaps.MishapBadOffhandItem
import at.petrak.hexcasting.common.items.storage.ItemSpellbook

class OpPages: ConstMediaAction {
    override val argc: Int
        get() = 1

    override fun execute(
        args: List<Iota>,
        env: CastingEnvironment
    ): List<Iota> {
        val caster =env.castingEntity
        if(caster!=null){
            val idx=args.getInt(0, argc)
            var book = caster.getItemInHand(env.otherHand)
            var item = book.item
            if(item is ItemSpellbook){
                for( i in 0 until idx){
                    ItemSpellbook.rotatePageIdx(book,true)
                }
                return listOf()
            }
            book = caster.getItemInHand(env.otherHand)
            item = book.item
            if(item is ItemSpellbook){
                for( i in 0 until idx){
                    ItemSpellbook.rotatePageIdx(book,true)
                }
                return listOf()
            }
            throw MishapBadOffhandItem.Companion.of(book,"spellbook")
        }
        throw MishapBadCaster()
    }
}