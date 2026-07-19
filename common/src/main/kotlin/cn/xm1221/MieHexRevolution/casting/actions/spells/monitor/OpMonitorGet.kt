package cn.xm1221.MieHexRevolution.casting.actions.spells.monitor

import at.petrak.hexcasting.api.casting.castables.ConstMediaAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.getEntity
import at.petrak.hexcasting.api.casting.iota.EntityIota
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.mishaps.MishapInvalidIota
import at.petrak.hexcasting.api.casting.mishaps.MishapOthersName
import cn.xm1221.MieHexRevolution.util.HexMonitorRecords.CAMERAS

import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity

class OpMonitorGet: ConstMediaAction {
       override fun execute(
           args: List<Iota>,
           env: CastingEnvironment
       ): List<Iota> {
           val target = args.getEntity(0, argc)

           if (target is ServerPlayer && CAMERAS[target] != target) {
               return listOf(EntityIota(CAMERAS[target] as Entity))

           } else if (target is ServerPlayer && CAMERAS[target] == target) {
               throw MishapOthersName(target)
           }
           throw MishapInvalidIota.Companion.of(EntityIota(target), 0, "player")
       }

       override val argc: Int
           get() = 1
   }