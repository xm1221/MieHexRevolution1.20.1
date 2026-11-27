package cn.xm1221.MieHexRevolution.mixin;

import at.petrak.hexcasting.api.casting.castables.SpellAction;
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.casting.mishaps.MishapBadOffhandItem;
import at.petrak.hexcasting.common.casting.actions.rw.OpWrite;
import cn.xm1221.MieHexRevolution.pigment.PigmentColorExtractor;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;

@Mixin(value = OpWrite.class, remap = false)
public abstract class MixinOpWrite {

    @WrapMethod(method = "execute", remap = false)
    private SpellAction.Result miehex_revolution$execute(
            List<? extends Iota> args,
            CastingEnvironment env,
            Operation<SpellAction.Result> original) {
        try {
            return original.call(args, env);
        } catch (MishapBadOffhandItem mishap) {
            PigmentColorExtractor.INSTANCE.throwIfTooManyColors(args, mishap);
            throw mishap;
        }
    }
}
