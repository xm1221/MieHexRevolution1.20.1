package cn.xm1221.MieHexRevolution.casting.actions.spells.monitor

import at.petrak.hexcasting.api.casting.castables.ConstMediaAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.getEntity
import at.petrak.hexcasting.api.casting.iota.EntityIota
import at.petrak.hexcasting.api.casting.iota.GarbageIota
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.mishaps.MishapInvalidIota
import cn.xm1221.MieHexRevolution.Miehex_revolution
import kotlin.collections.plus

class OpMonitorDef : ConstMediaAction {
      override val argc: Int
          get() = 1

       override fun execute(
           args: List<Iota>,
           env: CastingEnvironment
       ): List<Iota> {
           val target = args.getEntity(0, argc)
           if (Miehex_revolution.isMonitorEntity(target.type)) {
               val res = listOf<Iota>()
               if (env.world.players().size > 1023) {
                   res.plus(GarbageIota())
                   return res
               }
               env.world.players().forEach(
                   action = {
                       if (it.camera == target) {
                           res.plus(EntityIota(it))
                       }
                   }
               )
               return res
           }
           throw MishapInvalidIota.Companion.of(EntityIota(target), 0, "monitor")

       }
  }