package cn.xm1221.MieHexRevolution.mixin;

import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import at.petrak.hexcasting.api.casting.iota.BooleanIota;
import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.common.casting.actions.rw.OpReadable;
import cn.xm1221.MieHexRevolution.pigment.PigmentColorExtractor;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.Mixin;

import java.util.Collections;
import java.util.List;

@Mixin(value = OpReadable.class, remap = false)
public abstract class MixinOpReadable {

    @WrapMethod(method = "execute", remap = false)
    private List<Iota> miehex_revolution$execute(
            List<? extends Iota> args,
            CastingEnvironment env,
            Operation<List<Iota>> original) {
        if (PigmentColorExtractor.INSTANCE.isHeldPigmentReadable(env)) {
            return Collections.singletonList(new BooleanIota(true));
        }
        return original.call(args, env);
    }
}
