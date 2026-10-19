package cn.xm1221.MieHexRevolution.mixin;

import at.petrak.hexcasting.api.casting.eval.CastResult;
import at.petrak.hexcasting.api.casting.eval.vm.CastingVM;
import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation;
import at.petrak.hexcasting.api.casting.iota.PatternIota;
import cn.xm1221.MieHexRevolution.util.MieHexRevolutionHelpersKt;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(PatternIota.class)
public abstract class PatternIotaMixin {


    @WrapMethod(method = "execute")

    public @NotNull CastResult execute(CastingVM vm, ServerLevel world, SpellContinuation continuation, Operation<CastResult> original){
        @Nullable CastResult res;
        res= MieHexRevolutionHelpersKt.executeWithImports(((PatternIota)(Object)this),vm,world,continuation);
        if(res == null){
            return original.call(vm,world,continuation);
        }
        return res;
    }
}
