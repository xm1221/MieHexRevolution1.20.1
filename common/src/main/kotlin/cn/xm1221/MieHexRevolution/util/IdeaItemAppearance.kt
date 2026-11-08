package cn.xm1221.MieHexRevolution.util

import cn.xm1221.MieHexRevolution.block.idea.IdeaBlock
import cn.xm1221.MieHexRevolution.registry.Miehex_revolutionBlocks
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.block.model.BakedQuad
import net.minecraft.client.renderer.block.model.FaceBakery
import net.minecraft.client.renderer.block.model.ItemTransforms
import java.util.IdentityHashMap
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.client.resources.model.BakedModel
import net.minecraft.core.Direction
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.NbtUtils
import net.minecraft.util.RandomSource
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.inventory.InventoryMenu
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

/**
 * 物品形态的理念方块：物品栏/地上/展示框里也画成“已经变形”的样子。
 *
 * <p>做法是在 [net.minecraft.client.renderer.entity.ItemRenderer] 里把物品栈换掉：本体画被模仿方块的
 * **方块模型**（门、活板门这些方块的物品模型只是一张平面贴图，不像变形后的方块），再叠一层“负块”描边。
 *
 * <p>负块 = 同一个模型、每个面的顶点逆序、绕**模型自身的包围盒中心**放大一点：
 * 逆序让外表面变成背面被剔除，只剩壳的内表面能画；它在被模仿的方块之后面，
 * 正对镜头处被本体挡掉，只有露在轮廓外的一圈留下来。颜色取理念方块本体的结晶（[ideaSprite]）。
 *
 * <p>只画在不需要跟本体比深度的上下文，见 [shellScale]。
 */
object IdeaItemAppearance {

    /**
     * 外壳沿自身外法线往外让开这么远（模型空间，1.0 = 一整格）。
     *
     * <p>只影响深度、不改外形：把壳的可画面严格推到本体之后，共面处就永远由本体赢，轮廓外那一圈照旧。
     * 需要它的原因是：一块面的平面若穿过模型包围盒中心（放大是绕中心做的），放大只在面内变宽、
     * 平面不动，壳就与本体的那一面严格共面（`cross` 花草、单张平面贴图都是），
     * 两个共面三角形逐像素插值出的深度只差几个 ULP。
     */
    private const val SHELL_BIAS: Float = 0.02f

    /**
     * 外壳相对本体放大多少；**返回 null = 这个显示上下文不画负块**。
     *
     * <p>画物品栏、掉落物、展示框；**手持（第一/第三人称）不画** —— 那是本体和壳在同一透视里、
     * 深度测试开着画的，壳只能靠“比本体更远”多出轮廓外那百分之几宽的一圈，
     * 边缘像素随手的晃动逐帧翻盘，看上去就是描边在闪（咩咩 2026-11-08 拍板，别再加回去）。
     *
     * <p>放大量给到 30%：GUI 里 `GuiGraphics` 会先 `pose.scale(16,16,16)` 再画，图标只有十来像素宽，
     * 放得少了每边还不到一个像素。
     */
    private fun shellScale(displayContext: ItemDisplayContext): Float? = when (displayContext) {
        ItemDisplayContext.GUI, ItemDisplayContext.GROUND, ItemDisplayContext.FIXED -> 1.30f
        else -> null
    }

    private const val QUAD_SEED: Long = 42L

    /** `DefaultVertexFormat.BLOCK` 的步长：x, y, z, color, u, v, light, normal。 */
    private const val VERTEX_STRIDE = 8

    /**
     * 描边取结晶贴图上的哪一点（贴图内像素坐标 0..16，不是 0..1 比例）。
     *
     * <p>`(1, 0)` 是结晶左上那块最亮的青 `#9FDDDB`，也就是理念方块最认得出的颜色；
     * 贴着不要换成中间那些橄榄色杂晶（`(8, 8)` 是 `#AFB692`），那一圈会像脏了。
     * 整圈就取这一个点，tint 全白 —— 于是描边颜色永远等于贴图那一像素，
     * 以后改 `idea.png` 的配色，描边自动跟着走。
     */
    private const val CRYSTAL_U: Double = 1.0
    private const val CRYSTAL_V: Double = 0.0

    private const val TINT_RED: Float = 1f
    private const val TINT_GREEN: Float = 1f
    private const val TINT_BLUE: Float = 1f

    /**
     * 物品形态整体再缩一点。
     *
     * <p>内部模型统一改用方块模型（门、活板门这些方块自己的物品模型只是一张平面贴图，
     * 看着不像“变形后的方块”），而方块模型占满整格，比原版物品图标显大，
     * 因此统一乘这个系数收回来。
     */
    const val ITEM_SCALE: Float = 0.85f

    /**
     * 物品栈里存的材质 → 那个方块的方块状态。
     *
     * @return 没有材质、方块注册表里已经没有它时返回 null（调用方照常画本体）
     */
    fun materialState(stack: ItemStack): BlockState? {
        if (stack.isEmpty) return null
        val item = stack.item as? BlockItem ?: return null
        if (item.block !is IdeaBlock) return null
        val tag = BlockItem.getBlockEntityData(stack) ?: return null
        if (!tag.contains("Material")) return null
        // 方块注册表里已经没有那个方块时，原版 readBlockState 会退回 AIR，不会崩。
        val state = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag.getCompound("Material"))
        if (state.isAir) return null
        return state
    }

    /**
     * 物品栈里存的材质 → 那个方块的物品栈。
     *
     * @return 没有材质、方块没有对应物品、或不是理念方块时返回 null（调用方照常画本体）
     */
    fun materialStack(stack: ItemStack): ItemStack? {
        val state = materialState(stack) ?: return null
        val materialItem = state.block.asItem()
        if (materialItem === Items.AIR) return null
        return ItemStack(materialItem)
    }

    /**
     * 物品形态真正拿去画的模型。
     *
     * <p>不能直接把方块模型塞给 [net.minecraft.client.renderer.entity.ItemRenderer]，有两处必须兜：
     *
     * <p>① **multipart 模型在无状态时会返回空表**。本方法先按材质真实状态
     * （[materialState]，也就是理念方块身上存的那个状态）把每个方向的面上算一次缓存下来。
     * 栅栏、墙、玻璃板、栅栏门、铁栏杆这些都是 multipart，不兜的话整个物品形态一个面都画不出来。
     *
     * <p>② **方块模型不一定带 `display` 段**，缺了就退回“无变换”，在手里会画成占满屏幕的巨块。
     * 所以统一套标准方块显示变换（石头物品模型那一套，即 `block/block` 的
     * 30°/225°/0.625）—— 与“模型统一用方块模型做变换”一致，所有材质看起来是同一套摆法。
     *
     * @param state 理念方块存着的被模仿方块状态
     */
    fun itemModel(state: BlockState): BakedModel =
        BlockDisplayModel(Minecraft.getInstance().getBlockRenderer().getBlockModel(state), state)

    /** 状态已解算、显示变换统一的方块模型。 */
    private class BlockDisplayModel(
        delegate: BakedModel,
        state: BlockState,
    ) : BakedModel by delegate {

        private val culled: Map<Direction, List<BakedQuad>>
        private val unculled: List<BakedQuad>

        init {
            val random = RandomSource.create(QUAD_SEED)
            culled = Direction.values().associateWith { direction ->
                random.setSeed(QUAD_SEED)
                delegate.getQuads(state, direction, random)
            }
            random.setSeed(QUAD_SEED)
            unculled = delegate.getQuads(state, null, random)
        }

        override fun getQuads(state: BlockState?, direction: Direction?, random: RandomSource?): List<BakedQuad> =
            if (direction == null) unculled else culled[direction] ?: emptyList()

        override fun getTransforms(): ItemTransforms = blockTransforms

        override fun isGui3d(): Boolean = true

        override fun usesBlockLight(): Boolean = true
    }

    /** `block/block` 那套方块类显示变换，取自石头的物品模型。 */
    private val blockTransforms: ItemTransforms by lazy {
        Minecraft.getInstance().itemRenderer
            .getModel(ItemStack(Blocks.STONE), null, null, 0).transforms
    }

    /**
     * 在被模仿模型的轮廓外画一圈负块描边。
     *
     * <p>调用前本体已经画完了，这里要自己把同样的物品变换重新套一遍
     * （[net.minecraft.client.renderer.entity.ItemRenderer] 内部是 push/pop 的，回来时位姿已复原）。
     *
     * <p>不在 [shellScale] 白名单里的显示上下文第一句就返回，连取面、算包围盒都不做。
     */
    fun renderShell(
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        model: BakedModel,
        displayContext: ItemDisplayContext,
        leftHand: Boolean,
        light: Int,
        overlay: Int,
    ) {
        val scale = shellScale(displayContext) ?: return
        val quads = collectQuads(model)
        if (quads.isEmpty()) return
        val bounds = Bounds(quads)
        val crystal = ideaSprite()

        poseStack.pushPose()
        model.transforms.getTransform(displayContext).apply(leftHand, poseStack)
        // 绕“模型自己占的那块空间”的中心放大：半砖、火把、活板门这些不占满格的模型，
        // 围着格子中心放大会让描边歪向一边。
        poseStack.translate(-0.5, -0.5, -0.5)
        poseStack.translate(bounds.centreX.toDouble(), bounds.centreY.toDouble(), bounds.centreZ.toDouble())
        poseStack.scale(scale, scale, scale)
        poseStack.translate(-bounds.centreX.toDouble(), -bounds.centreY.toDouble(), -bounds.centreZ.toDouble())
        val pose = poseStack.last()

        val consumer = buffer.getBuffer(shellRenderType)
        putQuads(consumer, pose, quads, crystal, light, overlay)

        poseStack.popPose()
    }

    /**
     * 外壳用的渲染类型。
     *
     * <p>整圈是纯色、不要纹理，只需要一张图集加 cutout 的 alpha 测试；cull 开着，
     * 靠顶点逆序让壳的外表面被剔掉，只剩下“壳的内表面”能画出来。缓存成同一个实例，
     * 别每个物品每帧都 `RenderType.entityCutout(...)` 新建一个：那等于让顶点批的身份每帧换一次，
     * 而且白丢对象。
     */
    private val shellRenderType: RenderType by lazy {
        RenderType.entityCutout(InventoryMenu.BLOCK_ATLAS)
    }

    /**
     * 取模型全部的面。
     *
     * <p>迭代顺序照 [net.minecraft.client.renderer.entity.ItemRenderer#renderModelLists]：
     * 先六个方向各取“有剔除面”的那批，再取无剔除面的一批，同一个种子。
     */
    private fun collectQuads(model: BakedModel): List<BakedQuad> {
        val random = RandomSource.create(QUAD_SEED)
        val quads = ArrayList<BakedQuad>()
        for (direction in Direction.values()) {
            random.setSeed(QUAD_SEED)
            quads.addAll(model.getQuads(null, direction, random))
        }
        random.setSeed(QUAD_SEED)
        quads.addAll(model.getQuads(null, null, random))
        return quads
    }

    /**
     * 理念方块本体那块结晶的贴图。
     *
     * <p>从“还没材质时显示的模型”（models/item/idea_full.json，一块 cube_all）的第一个面上拿，
     * 不去图集里按名字查：这条路用到的 [BakedQuad.getSprite] 本来就在画面里跑着，一定拿得到。
     *
     * @return 拿不到时返回 null，描边退回“材质贴图 + 水青色覆盖”
     */
    private fun ideaSprite(): TextureAtlasSprite? {
        val stack = ItemStack(Miehex_revolutionBlocks.IDEA_FULL.get())
        val model = Minecraft.getInstance().itemRenderer.getModel(stack, null, null, 0)
        val random = RandomSource.create(QUAD_SEED)
        for (direction in Direction.values()) {
            random.setSeed(QUAD_SEED)
            model.getQuads(null, direction, random).firstOrNull()?.let { return it.sprite }
        }
        random.setSeed(QUAD_SEED)
        return model.getQuads(null, null, random).firstOrNull()?.sprite
    }

    /**
     * 把整圈壳写进顶点缓冲。
     *
     * <p>顶点不逐个自己写，而是照原版本体的写法走 [VertexConsumer.putBulkData]：同一台写入器、
     * 同一套顶点格式、同一份光照/overlay。唯一改动是把每个面的四个顶点顺序倒过来 —— 绕序一翻，
     * 外表面就成了背面被剔除，只剩壳的内表面能画，而它落在被模仿方块的外面，于是只剩一圈描边。
     */
    private fun putQuads(
        consumer: VertexConsumer,
        pose: PoseStack.Pose,
        quads: List<BakedQuad>,
        crystal: TextureAtlasSprite?,
        light: Int,
        overlay: Int,
    ) {
        // 整圈只取结晶贴图上的一个点：描边是一整块颜色，不需要纹理。
        // 注意 [TextureAtlasSprite.getU]/[TextureAtlasSprite.getV] 的参数是贴图内像素（0..16），
        // 不是 0..1 的比例 —— 传 0.5 会贴到最左那条边上，图集 mipmap 在那里混进透明像素，
        // 描边会被 cutout 的 alpha 测试整片丢掉。
        val crystalU = crystal?.getU(CRYSTAL_U)?.toFloat()
        val crystalV = crystal?.getV(CRYSTAL_V)?.toFloat()
        for (quad in quads) {
            consumer.putBulkData(
                pose, shellQuad(quad, crystalU, crystalV),
                TINT_RED, TINT_GREEN, TINT_BLUE, light, overlay,
            )
        }
    }

    /** 复制一份顶点逆序、沿面朝外方向让开 [SHELL_BIAS]、贴图坐标压在结晶贴图同一点上的面。 */
    private fun shellQuad(quad: BakedQuad, u: Float?, v: Float?): BakedQuad {
        // 烘焙出来的面是常量对象，整圈壳每帧都一样，缓存住别每帧丢一堆临时数组。
        if (u != cachedU || v != cachedV) {
            shellCache.clear()
            cachedU = u
            cachedV = v
        }
        val key = quad
        shellCache[key]?.let { return it }
        val source = quad.vertices
        val flipped = IntArray(source.size)
        // 法线只能取 BakedQuad.direction：烘焙出的顶点数组只有 pos/color/uv，法线那一格恒为 0。
        // 它由 FaceBakery 从绕序算出，没有 cullface 的面（cross 等）也有值。
        val facing = quad.direction ?: FaceBakery.calculateFacing(source)
        val biasX = facing.stepX * SHELL_BIAS
        val biasY = facing.stepY * SHELL_BIAS
        val biasZ = facing.stepZ * SHELL_BIAS
        for (index in 0 until 4) {
            val from = (3 - index) * VERTEX_STRIDE
            val to = index * VERTEX_STRIDE
            System.arraycopy(source, from, flipped, to, VERTEX_STRIDE)
            // 逆序之后能画出来的是“背着镜头”那一面，沿它的外法线推就是往更远处推。
            flipped[to] = (Float.fromBits(source[from]) + biasX).toRawBits()
            flipped[to + 1] = (Float.fromBits(source[from + 1]) + biasY).toRawBits()
            flipped[to + 2] = (Float.fromBits(source[from + 2]) + biasZ).toRawBits()
            if (u != null && v != null) {
                // 拿不到结晶贴图时保留材质自己那张。
                flipped[to + 4] = u.toRawBits()
                flipped[to + 5] = v.toRawBits()
            }
        }
        val shell = BakedQuad(flipped, quad.tintIndex, facing, quad.sprite, quad.isShade)
        shellCache[key] = shell
        return shell
    }

    private val shellCache = IdentityHashMap<BakedQuad, BakedQuad>()
    private var cachedU: Float? = null
    private var cachedV: Float? = null

    /** 模型自身的包围盒（只用到中心，放大要绕着它做）。 */
    private class Bounds(quads: List<BakedQuad>) {
        val centreX: Float
        val centreY: Float
        val centreZ: Float

        init {
            var minX = Float.MAX_VALUE
            var minY = Float.MAX_VALUE
            var minZ = Float.MAX_VALUE
            var maxX = -Float.MAX_VALUE
            var maxY = -Float.MAX_VALUE
            var maxZ = -Float.MAX_VALUE
            for (quad in quads) {
                val data = quad.vertices
                var index = 0
                while (index + 2 < data.size) {
                    val x = Float.fromBits(data[index])
                    val y = Float.fromBits(data[index + 1])
                    val z = Float.fromBits(data[index + 2])
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                    if (z < minZ) minZ = z
                    if (z > maxZ) maxZ = z
                    index += VERTEX_STRIDE
                }
            }
            centreX = (minX + maxX) * 0.5f
            centreY = (minY + maxY) * 0.5f
            centreZ = (minZ + maxZ) * 0.5f
        }
    }
}
