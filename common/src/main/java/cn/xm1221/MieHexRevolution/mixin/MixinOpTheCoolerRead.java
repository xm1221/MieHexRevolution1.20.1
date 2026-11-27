package cn.xm1221.MieHexRevolution.mixin;

import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.casting.mishaps.MishapBadEntity;
import at.petrak.hexcasting.api.casting.mishaps.MishapBadItem;
import at.petrak.hexcasting.common.casting.actions.rw.OpTheCoolerRead;
import cn.xm1221.MieHexRevolution.pigment.PigmentColorExtractor;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;

@Mixin(value = OpTheCoolerRead.class, remap = false)
public abstract class MixinOpTheCoolerRead {

    @WrapMethod(method = "execute", remap = false)
    private List<Iota> miehex_revolution$execute(
            List<? extends Iota> args,
            CastingEnvironment env,
            Operation<List<Iota>> original) {
        try {
            return original.call(args, env);
        } catch (MishapBadItem | MishapBadEntity mishap) {
            List<Iota> result = PigmentColorExtractor.INSTANCE.tryReadEntity(args, env);
            if (result != null) {
                return result;
            }
            throw mishap;
        }
    }
}
