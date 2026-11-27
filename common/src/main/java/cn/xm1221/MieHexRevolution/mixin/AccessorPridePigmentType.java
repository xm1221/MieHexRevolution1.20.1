package cn.xm1221.MieHexRevolution.mixin;

import at.petrak.hexcasting.common.items.pigment.ItemPridePigment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = ItemPridePigment.Type.class, remap = false)
public interface AccessorPridePigmentType {
    @Accessor("colors")
    int[] miehex_revolution$getColors();
}
