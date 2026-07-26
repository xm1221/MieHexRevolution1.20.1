package cn.xm1221.MieHexRevolution.casting.actions.spells

import at.petrak.hexcasting.api.casting.RenderedSpell
import at.petrak.hexcasting.api.casting.ParticleSpray
import at.petrak.hexcasting.api.casting.castables.SpellAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.getBlockPos
import at.petrak.hexcasting.api.casting.getInt
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.misc.MediaConstants
import net.minecraft.core.BlockPos
import net.minecraft.core.Vec3i
import net.minecraft.server.level.TicketType
import net.minecraft.world.level.ChunkPos

class OpWorldLoader {


    object Load: SpellAction {
        override fun execute(
            args: List<Iota>,
            env: CastingEnvironment
        ): SpellAction.Result {
            val pos = args.getBlockPos(0,argc)
            val time =args.getInt(1)
            val chunk = ChunkPos(pos)
            return SpellAction.Result(
         object :RenderedSpell{
             fun ticket(i:Int): TicketType<BlockPos> {
                 val type= TicketType.create<BlockPos>("casting", Vec3i::compareTo,i)
                 return  type
             }
             override fun cast(env: CastingEnvironment) {
                 env.world.chunkSource.addRegionTicket(
                     ticket(time),
                     chunk,
                     3,
                     pos
                 )
             }

         },
                cost = time.toLong(),
                emptyList<ParticleSpray>(), 0,
            )
        }

        override val argc: Int
            get() = 2
    }

   /* object ForceLoad:SpellAction{
        override val argc: Int
            get() = 1

        override fun execute(
            args: List<Iota>,
            env: CastingEnvironment
        ): SpellAction.Result {
            val pos = args.getBlockPos(0,argc)
            val chunk = ChunkPos(pos)
            return SpellAction.Result(
                object :RenderedSpell{
                    override fun cast(env: CastingEnvironment) {
                        env.world.chunkSource.addRegionTicket(
                            TicketType<ChunkPos>.FORCED,
                            chunk,
                            3,
                            chunk
                        )
                    }
        },
                cost = 20* MediaConstants.DUST_UNIT,
                emptyList<ParticleSpray>(), 0,
            )
    }

}*/

}