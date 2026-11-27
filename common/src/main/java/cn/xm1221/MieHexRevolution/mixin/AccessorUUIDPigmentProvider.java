package cn.xm1221.MieHexRevolution.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "at.petrak.hexcasting.common.items.pigment.ItemUUIDPigment$MyColorProvider", remap = false)
public interface AccessorUUIDPigmentProvider {
    @Accessor("colors")
    int[] miehex_revolution$getColors();
}
