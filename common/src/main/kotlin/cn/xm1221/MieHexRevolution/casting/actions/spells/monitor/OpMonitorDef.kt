package cn.xm1221.MieHexRevolution.casting.actions.spells.monitor

import at.petrak.hexcasting.api.casting.castables.ConstMediaAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.getEntity
import at.petrak.hexcasting.api.casting.iota.EntityIota
import at.petrak.hexcasting.api.casting.iota.GarbageIota
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.mishaps.MishapInvalidIota
import cn.xm1221.MieHexRevolution.Miehex_revolution

class OpMonitorDef : ConstMediaAction {
      override val argc: Int
          get() = 1

       override fun execute(
           args: List<Iota>,
           env: CastingEnvironment
       ): List<Iota> {
           val target = args.getEntity(env.world, 0, argc)
           if (Miehex_revolution.isMonitorEntity(target.type)) {
               val res = mutableListOf<Iota>()
               if (env.world.players().size > 1023) {
                   res.add(GarbageIota())
                   return res
               }
               for (p in env.world.players()) {
                   if (p.camera == target) res.add(EntityIota(p))
               }
               return res
           }
           throw MishapInvalidIota.Companion.of(EntityIota(target), 0, "monitor")

       }
  }