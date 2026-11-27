package cn.xm1221.MieHexRevolution.mixin;

import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.common.casting.actions.rw.OpRead;
import cn.xm1221.MieHexRevolution.pigment.PigmentColorExtractor;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;

@Mixin(value = OpRead.class, remap = false)
public abstract class MixinOpRead {

    @WrapMethod(method = "execute", remap = false)
    private List<Iota> miehex_revolution$execute(
            List<? extends Iota> args,
            CastingEnvironment env,
            Operation<List<Iota>> original) {
        List<Iota> pigmentResult = PigmentColorExtractor.INSTANCE.tryReadHeld(env);
        if (pigmentResult != null) {
            return pigmentResult;
        }
        return original.call(args, env);
    }
}
