package cn.xm1221.MieHexRevolution.mixin;

import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import at.petrak.hexcasting.api.casting.iota.BooleanIota;
import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.common.casting.actions.rw.OpTheCoolerReadable;
import cn.xm1221.MieHexRevolution.pigment.PigmentColorExtractor;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.Mixin;

import java.util.Collections;
import java.util.List;

@Mixin(value = OpTheCoolerReadable.class, remap = false)
public abstract class MixinOpTheCoolerReadable {

    @WrapMethod(method = "execute", remap = false)
    private List<Iota> miehex_revolution$execute(
            List<? extends Iota> args,
            CastingEnvironment env,
            Operation<List<Iota>> original) {
        List<Iota> result = original.call(args, env);
        if (result.size() == 1
                && result.get(0) instanceof BooleanIota b
                && !b.getBool()
                && PigmentColorExtractor.INSTANCE.isEntityPigmentReadable(args)) {
            return Collections.singletonList(new BooleanIota(true));
        }
        return result;
    }
}
