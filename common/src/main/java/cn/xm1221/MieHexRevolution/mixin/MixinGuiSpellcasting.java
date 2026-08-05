package cn.xm1221.MieHexRevolution.mixin;

import at.petrak.hexcasting.client.gui.GuiSpellcasting;
import cn.xm1221.MieHexRevolution.util.RemoteCastDataKeys;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec2;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;
import java.util.Set;

/**
 * 玩家持有远程施法数据（执行色非 0）时，把 GUI 中图案的默认执行蓝替换为
 * RemoteCastDataKeys 同步过来的执行色（Fake 红 / Players 绿）。
 * 手法同 HexGuide：WrapOperation 包住 RenderLib.drawPatternFromPoints，改写 tail/head 颜色。
 */
@Mixin(GuiSpellcasting.class)
public abstract class MixinGuiSpellcasting {

    @WrapOperation(
        // render 是 override Screen.render：Mojmap 环境为 render，Connector/SRG（Fabric HexMod 转换）为 m_88315_
        method = {"render", "m_88315_"},
        at = @At(
            value = "INVOKE",
            target = "Lat/petrak/hexcasting/client/render/RenderLib;drawPatternFromPoints(Lorg/joml/Matrix4f;Ljava/util/List;Ljava/util/Set;ZIIFFFD)V",
            remap = false
        ),
        remap = false
    )
    private void miehex_revolution$wrapExecutionColor(
        Matrix4f mat, List<Vec2> points, Set<Integer> dupIndices,
        boolean drawLast, int tail, int head, float flowIrregular,
        float readabilityOffset, float lastSegmentLen, double seed,
        Operation<Void> original
    ) {
        int color = RemoteCastDataKeys.getColor(Minecraft.getInstance().player);
        if (color != 0) {
            tail = color;
            head = color;
        }
        original.call(mat, points, dupIndices, drawLast, tail, head, flowIrregular,
            readabilityOffset, lastSegmentLen, seed);
    }
}
