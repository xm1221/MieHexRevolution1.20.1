# 染色剂（Pigment）实现原理与「NBT 数据驱动染色剂」设计

> 状态：**设计稿，尚未落地任何代码**。
> 依据：Hex Casting `0.11.4`（MC 1.20.1）的 sources jar 与构建产物 jar（只读缓存，未解压到工作区）+ 逐像素实测。
> 目标读者：本项目（`miehex_revolution`）后续接手实现的人。
> 相关版本：`main` = 1.20.1 / `1.21` 分支 = 1.21.1 + Hex Casting `0.12.0-devel-pre-64`。

---

## 0. 一句话结论

HexMod 的染色剂是**「一个物品 = 一种颜色」**：颜色写在 Java 常量里，纹理是与之逐一一对应的手绘帧条 PNG（35 张、约 1067 帧），二者靠「多注册 35 个物品」硬绑定。

我们要做的形态是**「一个物品 = 任意颜色」**：颜色与纹理都由 NBT（1.21：数据组件）驱动，纹理在运行期由「共享掩码 + 调色板」合成。

关键前提：**HexMod 的能力是扫注册表自动挂的，`provideColor` 签名里带 `ItemStack`，所以颜色侧不需要改 HexMod 一行代码。**

---

# 第一部分 · HexMod 染色剂实现原理

## 1.1 三层结构

```
ItemStack（物品 + NBT）
   │  implements PigmentItem.provideColor(ItemStack stack, UUID owner)
   ▼
ColorProvider（纯计算：给 time + 空间坐标，算出一个 ARGB）
   │  env.setPigment(new FrozenPigment(stack.copy(), uuid))
   ▼
FrozenPigment（record: 整份 ItemStack + owner UUID）
   → 存入玩家 / 召唤方块 / 法术环 / 咒术物品 / 施法粒子包
   → 渲染时再反向取出 requireColorProvider → provideColor
```

三层里只有第一层是「物品」，第二层是「算颜色的策略」，第三层是「把物品整个复制一份到处带着走」。

## 1.2 颜色定义

```java
// api/item/PigmentItem.java
public interface PigmentItem {
    ColorProvider provideColor(ItemStack stack, UUID owner);   // ← stack 可见，这是 NBT 的入口
}
```

`ColorProvider` 抽象类只要实现一个方法：

```java
protected abstract int getRawColor(float time, Vec3 position);   // AARRGGBB
```

对外的 `getColor(time, position)` 是 **`final`**，会做一次「最低亮度兜底」：

- 相对亮度 = `(0.2126R + 0.7152G + 0.0722B) / 0xFF`
- 若 `< 0.05`，叠加 6 色暗轮 `{FF200000, FF202000, FF002000, FF002020, FF000020, FF200020}`，速度 `time/400`
- 返回 `0xff000000 | r<<16 | g<<8 | b`；缺省色 `0xFF_ff00dc`

**副作用（重要）**：纯黑染色剂画出来不黑，是缓慢流动的暗红/暗黄/暗绿…。自定义染色剂若允许玩家选纯黑，必须提前告知，或把最暗档压在亮度 ≥ 0.05（约 `#1A1A1A` 以上）。

### 各色族的常量

| 物品 | 类 | 颜色来源 | 渐变速度 |
|---|---|---|---|
| `dye_colorizer_*`（16） | `ItemDyePigment` | `dyeColor.getTextColor()` → **16 种聊天格式色**（红 `0xFF0000`、蓝 `0x0000FF`、白 `0xFFFFFF`） | 单色，不渐变 |
| `pride_colorizer_*`（16） | `ItemPridePigment` | 枚举里每型一个 2~5 色数组 | `time/400` |
| `default_colorizer` 空无 | `ItemAmethystPigment` | 常量 `0xFFAB65EB` | 单色 |
| `uuid_colorizer` 灵魂闪光 | `ItemUUIDPigment` | Paucal 贡献者 JSON 的 `hexcasting:colorizer`；否则用 `uuid.low ^ uuid.high` 作种子随机两组 HSV | `time/400` |
| `ancient_colorizer` 远古 | `ItemAmethystAndCopperPigment` | `{54398A, CFA0F3, FECBE6, CFA0F3, E77C56}` | `time/600` |

> 注意：染料染色剂的**世界色是聊天色**，与它贴图上的柔和色调不是一套（见 2.6）。

### 渐变算法 `ADPigment.morphBetweenColors(colors, dir, time, position)`

```
fIdx  = fract(time + dir·position) * colors.length
t     = 三次缓动(tRaw)      // tRaw < 0.5 ? 4t³ : 1 − (−2t+2)³ / 2
color = lerp(colors[i], colors[i+1], t)      // 含 alpha 通道
```

`dir` 固定 `(0.1, 0.1, 0.1)`，**所以同一个颜色提供者在空间不同位置取到的颜色不同**——这是悬浮二十面体（哨卫/法杖）会沿对角线呈彩虹、而不是整条一个色的原因：渲染时逐顶点用自己的坐标去问一次颜色。

## 1.3 施加与承载

**施加**：`OpColorize`（图案「内化染色剂」，argc = 0，花费 `DUST_UNIT` = 1 紫水晶粉）。

```
找主手/副手里 isPigment 的栈 → stack.copy() → env.setPigment(FrozenPigment(copy, uuid)) → 物品被消耗
```

扣物品用的是 `withdrawItem({ ItemStack.isSameItemSameTags(copy, it) }, 1, true)`。

**承载**：`FrozenPigment(ItemStack item, UUID owner)`，序列化为 `{stack: <整份物品 NBT>, owner: <uuid>}`。它被塞进：

1. Fabric：玩家 Cardinal Component `hexcasting:favored_pigment`（AutoSynced）
2. Forge：`player.getPersistentData()` 的 `hexcasting:pigment`，靠 `MsgPigmentUpdateAck` 同步
3. 每个**召唤方块**（`BlockEntityConjured` 的 `tag_colorizer`）
4. 每个**法术环 impetus**、每次环执行的 `casterPigment`
5. 每个**已装填咒术物品**（cypher / trinket / artifact 的 `pigment` 键）
6. **每一次施法粒子包** `MsgCastParticleS2C`

→ 这意味着染色剂的 NBT 会**被复制到大量对象里并反复上网络**（见 3.5 坑位 #1）。

## 1.4 纹理来源

模型（datagen 生成，`HexItemModels` 里三个 for 循环逐色生成）：

```json
{ "parent": "minecraft:item/generated",
  "textures": { "layer0": "hexcasting:item/colorizer/<变体>" } }
```

贴图：`assets/hexcasting/textures/item/colorizer/<变体>.png` + 同名 `.mcmeta`

- **竖向帧条**：宽 16，高 = 16 × 帧数
- `.mcmeta` 是手排帧表，`"interpolate": false`，每帧时长只有 1 / 4 / 10 三种
- 整条动画 42~66 tick（约 2.1~3.3 秒）一循环

---

# 第二部分 · 实测：这些东西到底是怎么画出来的

> 测量方式：直接从构建产物 jar 里按条目读 PNG 像素（PowerShell + `System.Drawing`，未解压落盘）。
> 复现思路见 2.8。

## 2.1 只有 5 套轮廓动画，35 张图共用

- **所有变体的第 0 帧完全相同**：同一颗「像素圆」——中段 10 px 宽、上下各 8 px 宽、12 行高，约 90 个不透明像素。
  ```
  ......####......
  ....########....
  ....########....
  ...##########...
  ...##########...      ← 16×16，居中
  ...##########...
  ...##########...
  ....########....
  ....########....
  ......####......
  ```
- **同一帧数组内部，逐帧 alpha 掩码完全一致**。逐帧比对结果：

| 帧数 | 组内成员 | 逐帧掩码一致 |
|---|---|---|
| 36 | bisexual, demiboy, demigirl, genderqueer, pansexual, transgender | 36/36 全同 |
| 32 | agender, asexual, nonbinary, plural | 32/32 全同 |
| 31 | aroace, aromantic, gay, genderfluid, lesbian | 31/31 全同 |
| 30 | 16 张 `dye_*` + intersex | 30/30 全同 |
| 8 | default（空无） | 独立一套 |

→ 全模组约 **1067 帧**贴图，实际只有 **5 套轮廓动画**。

## 2.2 染料那 16 张：连「明暗档位图」都一样

把每帧内的颜色按亮度排序成 0..4 档（0 最暗、4 最亮），得到「档位图」。`dye_red` / `dye_blue` / `dye_green` / `dye_white` 的档位图**逐帧完全一致**：

```
....11333200....
....14412220....
...1344311220...
...1313311120...
...1321131130...
...1221113430...
....02211430....
....00223300....
......0000......
```

也就是说：

> **HexMod 的染料贴图，本质就是「一张共享的 5 档明暗掩码 × 16 套色阶」，只不过在美术阶段就被烘焙成了 16 张 PNG。**

每张染料恰好用 5 个颜色，16 张合计正好 80 色。
（中心亮、下缘最暗 —— 观感是一颗中心高光的圆润宝石。）

## 2.3 帧数与调色板色数严格挂钩

16 个 pride 变体，零例外：

| 调色板色数 k | 帧数 | 变体 |
|---|---|---|
| 2 | **30** | intersex |
| 3 | **36** | bisexual, demiboy, demigirl, genderqueer, pansexual, transgender |
| 4 | **32** | agender, asexual, nonbinary, plural |
| 5 | **31** | aroace, aromantic, gay, genderfluid, lesbian |

四组色数 → 四个不同的帧数，且组内完全一致。这不是美术随手定的，是有工具按 k 算出来的（具体公式未还原，实测值如上）。

## 2.4 pride 的差异只在「色带怎么排」

色带条数 = k，但方向和位置**每张按各自旗帜**画，并不统一：

- `gay`：色带沿右上→左下斜排，**蓝(k−1) 在上、红(0) 在下**
- `transgender`：蓝上 / 白中 / 粉下
- `aromantic`、`genderfluid`：顺序是**反的**（0 在上、k−1 在下）
- `aroace`：不是水平条带，是斜向/环绕的
- `bisexual` 与 `pansexual` 同属 3 色组，但带的像素占用量不同（22 vs 12 一类差异）

**唯一异类是 `intersex`**：没有色带，画的是「黄底 + 一个紫色圆环」（双性人旗帜本身就是紫圈），紫环只占 8 个像素。原因写在源码注释里：

```java
// how to do an intersex gradient escapes me
INTERSEX(new int[]{0xebf367, 0x7210bc}),
```

### 三张示意图（帧 0，数字 = 最近调色板索引，`.` = 透明）

```
pride_gay (5 色)          pride_transgender (3 色)   pride_intersex (2 色)
......4433......          ......2222......           ......0000......
....44443311....          ....22122222....           ....00000000....
....42333321....          ....21122222....           ....00000000....
...4433331220...          ...2111102222...          ...0000110000...
...4333211110...          ...2121102222...          ...0001001000...
...3321121120...          ...0002200222...          ...0001001000...
...3221112200...          ...0002200122...          ...0000110000...
....12111200....          ....00000122....           ....00000000....
....11110000....          ....00000022....           ....00000000....
......0000......          ......0002......           ......0000......
```

## 2.5 动画是「呼吸」，不是「自转」

追踪 `gay` 那 31 帧里红色带质心相对宝石质心的角度：**全程稳定在 45°~53°**（帧 0 = 52.7°，帧 15~29 = 45°）。

变的是：

- 带的**面积**（红色带 22 → 55 像素，2.5 倍起伏）
- 轮廓边缘 1 px 抖动（28/29 帧的 alpha 掩码都在变）

→ 不是「宝石在转」，是「色带在呼吸 + 边缘在抖」。做动效复刻时，**不需要旋转逻辑**。

## 2.6 贴图色 ≠ 世界色

`gay` 一帧用到 18 个颜色，其中**不含它自己的世界色 `0x2f4dd8` 本体**，蓝带用的是 `172bc9`（暗）与 `6894d4` / `99c7e4`（亮）——后两个恰好是 `dye_blue` 那张的档位。

结论：每条带的色阶是**手配**的近似色阶，部分从染料色阶表摘用，**不是由色常量机械推导**。

- 16 张染料：合计 80 色（每张恰好 5 档）
- 16 张 pride：合计 130 色，其中 **67 个不在染料色阶表里**
- `default` / `ancient` / `uuid`：另有 33 色不在上述集合内

→ 我们自动烘焙时只能做到「观感接近」，**不要承诺像素级复刻**。

## 2.7 全量统计

| 分类 | 张数 | 帧数/张 | 帧数小计 |
|---|---|---|---|
| `dye_colorizer_*` | 16 | 30 | 480 |
| `pride_colorizer_*` | 16 | 30/31/32/36 | 529 |
| `ancient_colorizer` | 1 | 30 | 30 |
| `uuid_colorizer` | 1 | 20 | 20 |
| `default_colorizer` | 1 | 8 | 8 |
| **合计** | **35** | | **≈1067** |

每帧只用 6~22 个不同颜色，色彩预算很省。

## 2.8 复现方法

```python
# 需 Pillow；直接从构建产物 jar 按条目读，不要解压
import zipfile
from PIL import Image
from io import BytesIO

jar = r'...\hexcasting-fabric-1.20.1-0.11.4.jar'
path = 'assets/hexcasting/textures/item/colorizer/pride_gay.png'
with zipfile.ZipFile(jar) as z:
    img = Image.open(BytesIO(z.read(path))).convert('RGBA')

frames = img.height // 16
for f in range(frames):
    cell = img.crop((0, f * 16, 16, f * 16 + 16))
    # cell.load(); 逐像素取 (r,g,b,a) 即可复现上文所有统计
```

## 2.9 手册里的现状（写作时注意）

`thehexbook` 的 `items/pigments` 条目只写了：

- 16 个染料染色剂（「色彩染色剂」页）
- 2 个特殊染色剂（灵魂闪光 / 空无）
- 结尾斜体：*「以及许多还没被研究出来的内在颜色。」*

**16 个 pride 染色剂在手册里一个字都没有**（官方刻意留白，配方靠象征物、名字不解释）。

→ 若将来给我们自己的染色剂写 Patchouli 条目，**不可自行编造原书没有的机制说明**；pride 族的"未记载"状态也应保持，不要主动补全。

---

# 第三部分 · 我们的设计：NBT（1.20.1）/ 数据组件（1.21）驱动

## 3.1 为什么可行：能力是自动挂的

HexMod 扫注册表，任何 `implements PigmentItem` 的物品都会自动获得颜色能力，并且**拿到的是 ItemStack 本身**：

- Fabric：`registry.register(i -> i instanceof PigmentItem, PIGMENT, CCPigment.ItemBased::new)`
- Forge：`if (stack.getItem() instanceof PigmentItem pigment) evt.addCapability(PIGMENT_CAP, provide(stack, HexCapabilities.COLOR, () -> new CapItemPigment(pigment, stack)))`

→ **颜色侧零侵入**，不需要改 HexMod，也不需要 mixin。

## 3.2 数据契约

### 1.20.1（NBT）

```
miehex_revolution:pigment: {
  V: 1,                          // schema 版本，未知字段忽略
  C: [I; 0x8B4BFF, 0xFFC0CB],    // 1..8 个裸 RGB，alpha 统一补 0xFF
  M: "morph",                    // solid | morph | stripe
  S: 1.0f,                       // 速度倍率：time / (400 / S)
  D: [0.1, 0.1, 0.1],            // 渐变方向
  K: "gem",                      // 掩码/形状 id
  P: <8 字节 hash>               // 可选：像素图哈希，不直接塞像素
}
```

三条硬约束：

1. **最多 8 色**（HexMod 最多用到 5~6 色就够）
2. **NBT 里绝不塞原始像素**（理由见 3.5 坑位 #1）
3. **值必须确定性**：不能有 `LastTick` / seed / 时间戳

### 1.21.1（数据组件）

```kotlin
data class CustomPigmentData(
    val colors: List<Int>, val mode: Mode, val speed: Float, val pixels: ByteArray?,
)

val CUSTOM_PIGMENT: DataComponentType<CustomPigmentData> =
    DataComponentType.builder<CustomPigmentData>()
        .persistent(CustomPigmentData.CODEC)                  // RecordCodecBuilder + ByteBufCodecs
        .networkSynchronized(CustomPigmentData.STREAM_CODEC)   // 必须！漏了客户端看不到组件
        .build()
```

- `Item.Properties().component(CUSTOM_PIGMENT, DEFAULT)`，读取 `stack.getOrDefault(CUSTOM_PIGMENT, DEFAULT)`
- 已核对签名：`DataComponentType$Builder#persistent/networkSynchronized/build`、`Item$Properties#component`、`ItemStack#set/get/remove`
- **纹理侧完全不变**：1.21.1 的 `TextureManager.register(String, DynamicTexture)` 与 1.20.1 一致

## 3.3 颜色侧

```kotlin
class ItemCustomPigment(p: Properties) : Item(p), PigmentItem {
    override fun provideColor(stack: ItemStack, owner: UUID): ColorProvider =
        PigmentProviderCache.getOrBuild(PigmentData.read(stack), owner)
}

private class NbtColorProvider(
    private val colors: IntArray, private val speed: Float, private val dir: Vec3,
) : ColorProvider() {
    override fun getRawColor(time: Float, position: Vec3): Int =
        if (colors.size == 1) colors[0]                            // 单色短路
        else ADPigment.morphBetweenColors(colors, dir, time / (400f / speed), position)
}
```

- 直接复用 `ADPigment.morphBetweenColors` → 「同色不同位置不同色」的观感自然继承
- `PigmentData` 必须是**纯 common 数据类**（服务端也要算：法术环 impetus 在服务端存 FrozenPigment）
- **必须缓存**：`getColorProvider()` 在渲染循环里每顶点都可能被调，不能在里面解析 NBT / 建数组。用 `(tagHash, owner) → Provider` 的 8~16 条 LRU，provider 内部只放不可变数组

## 3.4 纹理侧：三条路线

| 方案 | 做法 | 优点 | 缺点 |
|---|---|---|---|
| **A 单色 tint** | 一张灰度白模贴图 + `ItemColor`（`getColor(stack, tint)` 拿得到 stack） | 零成本、零风险、光影/OptiFine 全兼容 | 整张只能一个色，做不出多色条纹 |
| **B 预烘焙变体** | `ItemProperties` / `custom_model_data` 按 NBT 切模型 | 纯 JSON，简单 | 只能选**已有**颜色，不叫「自定义」 |
| **C 运行期合成（推荐）** | 一张「明暗/色带索引掩码」PNG + 调色板 → `NativeImage` → `DynamicTexture` → `TextureManager.register` | 任意颜色/任意条纹，观感与 HexMod 一致，1 张源图替代 ~1067 帧 | 要做纹理缓存/回收，要自己写顶点 |

### 方案 C 的具体形态

**1. 掩码（两个通道）**

`textures/item/pigment_mask.png`，16 宽 × 16×N 高（N 取 8~30，可直接借用 HexMod 那种呼吸节奏）。

| 通道 | 含义 |
|---|---|
| **R** | 色带编号 `0..k-1` |
| **G** | 带内档位 `0..m-1`（明暗） |
| **A** | 覆盖 |
| B | 保留 |

**为什么必须双通道**：HexMod 的染料是「单色 5 档」（一维），但 pride 是「带编号 × 带内 2~4 档明暗」（二维）。只用一个通道只能复刻染料那类，复刻不了 pride 的色带。

**2. 烘焙**

```
color(x,y) = shadeOf( palette[R] , G )      // shade 建议：明度 ±，或向白/黑 lerp
```

- 单色（`solid`）：所有 R 映射同一色
- 多色（`morph` / `stripe`）：R 就是调色板索引
- 玩家自画（v2）：玩家画的就是索引图

用 `NativeImage` + `setPixelRGBA` 生成 16×16。

**3. 注册纹理**

```kotlin
val rl = Minecraft.getInstance().textureManager
    .register("miehex_revolution:pigment/${hash}", DynamicTexture(image))
// 回收：textureManager.release(rl)
```

**4. 画出来**

沿用项目里已验证的路子：

- `common/src/main/java/cn/xm1221/MieHexRevolution/mixin/MixinItemRenderer.java` 已有 `@WrapMethod("render")` 钩子（含替换 `BakedModel`）
- `common/src/main/kotlin/cn/xm1221/MieHexRevolution/util/IdeaItemAppearance.kt` 里 `renderShell` 已示范「自己取面 + 自己写顶点 + `buffer.getBuffer(RenderType.entityCutout(自己的纹理))`」（`VERTEX_STRIDE = 8`）

照抄一个分支：套 `model.transforms.getTransform(displayContext)`，把烘焙出的 4 个顶点写进去。

> **为什么必须自己写顶点**：`ItemRenderer` 的 RenderType 来自 `ItemBlockRenderTypes.getRenderType(stack)`，我们的动态纹理不在图集里，用它画会按图集 UV 贴成一片糊。

**5. 动画**

- v1：只画帧 0（静止）
- v1.5：只对「当前可见的、至多 4 个」每 2 tick 重烘一帧 + `upload()`

世界侧本来就在动（手持宝石光、施法粒子），图标不必强行动画。

## 3.5 坑位清单（都来自 HexMod 代码或实测）

1. **`FrozenPigment` 复制整份物品栈的 NBT**，并写进：玩家组件 / 每个召唤方块 BE / 每个 impetus / 每个已装填咒术物品 / **每一次施法粒子包**。
   → 染色剂 NBT 必须小。塞 16×16 像素（256B~1KB）意味着每放一次「召唤方块」就复制一份、每施法一次就发一次。
   → **对策**：NBT 只放调色板 + 参数；像素图用 hash 引用 + 自建小包（只发给看得见的人），或纯客户端本地缓存。
2. **`OpColorize` 用 `isSameItemSameTags` 扣物品** → NBT 必须稳定。混进会变的字段，玩家会看到「内化失败但东西没少」。
3. `ColorProvider.getColor` 是 `final`，最低亮度兜底关不掉 → **纯黑不可用**。
4. `provideColor` 在高频渲染路径上被调用 → 必须缓存。
5. `MishapBadOffhandItem.of(EMPTY, "colorizer")` 的提示键是公用的 → 自定义染色剂的失败提示自动一致，不用改。
6. **F3+T 资源重载会清空 `TextureManager`** → 必须挂 `RegisterClientReloadListeners` / 纹理重载钩子重建动态纹理缓存，否则按完 F3+T 图标全变紫黑格。
7. `gradle.properties` 里 `minecraft_version=1.20.1` 走的是 `common/fabric/forge`；仓库根下的 `neoforge/` 是构建残留（未在 `settings.gradle.kts` 中），真正的 1.21 目标在 `1.21` 分支。

## 3.6 复刻 16 个 pride 预设

有了 `M` + `C` + `K`，"pride 族"就等于 16 条预设数据（不用注册 16 个物品）：

| 预设 | 色数 k | 掩码（色带排布） | 调色板 |
|---|---|---|---|
| agender | 4 | 32 帧组 | `16a10c ffffff 7a8081 302f30` |
| aroace | 5 | 31 帧组（斜向） | `7210bc ebf367 ffffff 82dceb 2f4dd8` |
| aromantic | 5 | 31 帧组（0 在上） | `16a10c 82eb8b ffffff 7a8081 302f30` |
| asexual | 4 | 32 帧组 | `333233 9a9fa1 ffffff 7210bc` |
| bisexual | 3 | 36 帧组 | `db45ff 9c2bd0 6894d4` |
| demiboy | 3 | 36 帧组 | `9a9fa1 a9ffff ffffff` |
| demigirl | 3 | 36 帧组 | `9a9fa1 fcb1ff ffffff` |
| gay | 5 | 31 帧组（斜排，k−1 在上） | `d82f3a e0883f ebf367 2db418 2f4dd8` |
| genderfluid | 5 | 31 帧组（0 在上） | `fbacf9 ffffff 9c2bd0 333233 2f4dd8` |
| genderqueer | 3 | 36 帧组 | `ca78ef ffffff 2db418` |
| intersex | 2 | 30 帧组（**圆环，非色带**） | `ebf367 7210bc` |
| lesbian | 5 | 31 帧组 | `d82f3a efb87d ffffff fbacf9 a30262` |
| nonbinary | 4 | 32 帧组 | `ebf367 ffffff 7210bc 333233` |
| pansexual | 3 | 36 帧组 | `e278ef ebf367 6ac2e4` |
| plural | 4 | 32 帧组 | `30c69f 347ddf 6b3fbe 000000` |
| transgender | 3 | 36 帧组 | `eb92ea ffffff 6ac2e4` |

（源码里还有一行被注释掉的 `ACHILLEAN`，且色值 `0xe49c7` 只有 5 位十六进制，是笔误，未启用。）

注意 `plural` 含 `000000`（纯黑）——因为最低亮度兜底，它实际渲染出来不会是纯黑。

## 3.7 落地顺序

1. **数据层**：`PigmentData`（纯 common、带版本号与校验）、`ItemCustomPigment`、注册进 `registry/Miehex_revolutionBlocks.kt` 里那个 `ITEMS` DeferredRegister（或新建 Items 注册类）、lang + 一张临时单色贴图
2. **颜色层**：Provider + 缓存 → 此时已能被「内化染色剂」用上，手持宝石光、施法粒子、召唤方块立刻跟着 NBT 变色
3. **纹理层**：掩码 PNG + 动态纹理缓存 + `MixinItemRenderer` 分支
4. **编辑层**：调色盘 GUI / 从手持染料取色 / 用已有 op 直接从 iota 读写颜色
5. **1.21 分支**：NBT 换组件，纹理侧不动

---

# 附录 A · pride 配方（4 紫水晶粉 + 1 件象征物）

配方结构（`HexplatRecipes.java:588` 的 `gayRecipe`）：

```java
ShapedRecipeBuilder.shaped(RecipeCategory.MISC, colorizer)
    .define('D', HexItems.AMETHYST_DUST)
    .define('C', material)
    .pattern(" D ").pattern("DCD").pattern(" D ")
```

| 变体 | 中心物 | 变体 | 中心物 |
|---|---|---|---|
| AGENDER | 玻璃 | LESBIAN | 蜜脾 |
| AROACE | 小麦种子 | NONBINARY | 苔藓块 |
| AROMANTIC | 箭 | PANSEXUAL | 胡萝卜（装 farmersdelight 时改用 `farmersdelight:skillet`） |
| ASEXUAL | 面包 | PLURAL | 中继器 |
| BISEXUAL | 小麦 | TRANSGENDER | 鸡蛋 |
| DEMIBOY | 粗铁 | GENDERFLUID | 水桶 |
| DEMIGIRL | 粗铜 | GENDERQUEER | 玻璃瓶 |
| GAY | 石砖墙 | INTERSEX | 杜鹃 |

染料染色剂同结构，中心物为对应染料；`uuid_colorizer` 用 `ring(...)`（紫水晶碎片居中）、`default_colorizer` 用 `ringCornerless(...)`、`ancient_colorizer` 用 `ringCornerless(...)` + 铜锭。

# 附录 B · pride 中文名（官方译名，`zh_cn.json`）

| 枚举 | 中文 |
|---|---|
| AGENDER | 无性别染色剂 |
| AROACE | 无浪漫倾向无性恋染色剂 |
| AROMANTIC | 无浪漫倾向染色剂 |
| ASEXUAL | 无性恋染色剂 |
| BISEXUAL | 双性恋染色剂 |
| DEMIBOY | 部分男性染色剂 |
| DEMIGIRL | 部分女性染色剂 |
| GAY | 男同性恋染色剂 |
| GENDERFLUID | 性别流体染色剂 |
| GENDERQUEER | 性别酷儿染色剂 |
| INTERSEX | 双性人染色剂 |
| LESBIAN | 女同性恋染色剂 |
| NONBINARY | 非二元性别染色剂 |
| PANSEXUAL | 泛性恋染色剂 |
| PLURAL | 多重人格染色剂 |
| TRANSGENDER | 跨性别染色剂 |

其余：`uuid_colorizer` = 灵魂闪光染色剂、`default_colorizer` = 空无染色剂、`ancient_colorizer` = 远古染色剂。

# 附录 C · 待验证项

1. Hex Casting `0.12.0-devel-pre-64`（1.21.1）的 `PigmentItem.provideColor` 签名是否仍为 `(ItemStack, UUID)` —— 本机缓存里没有 0.12 的 sources，落 1.21 前必须先对。
2. 帧数 ↔ 色数的生成公式（实测 2→30 / 3→36 / 4→32 / 5→31，具体规则未还原；不影响我们的实现）。
3. 方案 C 的顶点绘制在 Forge 端（`net.minecraftforge.client` 的 `ItemRenderer`）与 Fabric 是否存在分支差异 —— 需参照现有 `MixinItemRenderer` 的双平台写法确认。
4. 动态纹理缓存对光影包（Iris/OptiFine）的影响未测。

# 附录 D · 参考位置

- Hex Casting sources jar：`~/.gradle/caches/modules-2/files-2.1/at.petra-k.hexcasting/hexcasting-common-1.20.1/0.11.4/<hash>/hexcasting-common-1.20.1-0.11.4-sources.jar`
- 贴图在构建产物 jar：`hexcasting-fabric-1.20.1-0.11.4.jar` → `assets/hexcasting/textures/item/colorizer/*.png`
- 关键类：`api/item/PigmentItem`、`api/pigment/{ColorProvider, FrozenPigment}`、`api/addldata/ADPigment`、`common/items/pigment/*`、`common/casting/actions/spells/OpColorize`、`datagen/recipe/HexplatRecipes`、`datagen/xplat/HexItemModels`
- 本项目已有可复用件：`mixin/MixinItemRenderer.java`、`util/IdeaItemAppearance.kt`、`util/RendererHelper.kt`、`casting/env/ExecutionColorProvider.kt`
