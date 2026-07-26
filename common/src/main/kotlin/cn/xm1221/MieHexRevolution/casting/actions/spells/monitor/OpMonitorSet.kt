package cn.xm1221.MieHexRevolution.casting.actions.spells.monitor

import at.petrak.hexcasting.api.casting.RenderedSpell
import at.petrak.hexcasting.api.casting.castables.SpellAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.getEntity
import at.petrak.hexcasting.api.casting.iota.EntityIota
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.mishaps.MishapBadCaster
import at.petrak.hexcasting.api.casting.mishaps.MishapInvalidIota
import at.petrak.hexcasting.api.misc.MediaConstants
import cn.xm1221.MieHexRevolution.Miehex_revolution
import cn.xm1221.MieHexRevolution.util.HexMonitorRecords
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundSetCameraPacket
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity


class OpMonitorSet : SpellAction {
       override val argc: Int
           get() = 1

       override fun execute(
           args: List<Iota>,
           env: CastingEnvironment
       ): SpellAction.Result {
           val monitor = args.getEntity(0, argc)
           if (env.castingEntity !is ServerPlayer) {
               throw MishapBadCaster()
           }
           if(!Miehex_revolution.isMonitorEntity(monitor.type)) {
               throw MishapInvalidIota.of(EntityIota(monitor),0,"monitor")
           }

               val caster = env.castingEntity
               val res = SpellAction.Result(
                   effect = object : RenderedSpell {
                       override fun cast(env: CastingEnvironment) {
                           if (caster is ServerPlayer) {
                               caster.connection.send(ClientboundSetCameraPacket(monitor))
                               HexMonitorRecords.CAMERAS[caster] = monitor as Entity?
                               HexMonitorRecords.CASTERS[monitor as Entity?]=caster
                               //caster.camera=monitor
                               if(monitor is ServerPlayer && monitor.uuid != caster.uuid) {
                                   monitor.sendSystemMessage(Component.translatable("hex_monitor.chat.looking").withStyle(
                                       ChatFormatting.DARK_RED))
                               }
                           }
                       }

                   },
                   cost = 5 * MediaConstants.DUST_UNIT,
                   particles = listOf()
               )
               return res



       }
   }