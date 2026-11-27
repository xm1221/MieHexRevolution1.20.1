package cn.xm1221.MieHexRevolution.mixin;

import at.petrak.hexcasting.api.casting.castables.SpellAction;
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.casting.mishaps.MishapBadEntity;
import at.petrak.hexcasting.api.casting.mishaps.MishapBadItem;
import at.petrak.hexcasting.common.casting.actions.rw.OpTheCoolerWrite;
import cn.xm1221.MieHexRevolution.pigment.PigmentColorExtractor;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;

@Mixin(value = OpTheCoolerWrite.class, remap = false)
public abstract class MixinOpTheCoolerWrite {

    @WrapMethod(method = "execute", remap = false)
    private SpellAction.Result miehex_revolution$execute(
            List<? extends Iota> args,
            CastingEnvironment env,
            Operation<SpellAction.Result> original) {
        try {
            return original.call(args, env);
        } catch (MishapBadItem | MishapBadEntity mishap) {
            PigmentColorExtractor.INSTANCE.throwIfEntityTooManyColors(args);
            throw mishap;
        }
    }
}
