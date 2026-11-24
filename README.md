# MieHex:Revolution

[![powered by hexdoc](https://img.shields.io/endpoint?url=https://hexxy.media/api/v0/badge/hexdoc?label=1)](https://github.com/hexdoc-dev/hexdoc)

MieHex:Revolution 是 Hex Casting 的扩展模组，为咒法体系引入全新图案与理念方块（Idea Block）等实用机制。

MieHex:Revolution is a Hex Casting addon that introduces new patterns, including Idea Blocks — a novel building mechanic.

---

## 新图案 | New Patterns

| 图案 Pattern | 说明 Description |
|-------------|-----------------|
| **灵魂投射** *Soul Projection* | 将灵魂碎片投射至实体，共享其视野 / Project a sliver of your soul into an entity to share its vision |
| **极限：极大之精思** *Maximum Reflection* | 获取此现实能表达的最大数值 / Obtain the largest number expressible in this reality |
| **极限：极小之精思** *Minimum Reflection* | 获取此现实能表达的最小数值 / Obtain the smallest number expressible in this reality |
| **学者之翻页** *Scholar's Page-Turn* | 隔空翻阅副手法术书 / Flip your offhand spellbook remotely |
| **时之锚** *Temporal Anchor* | 锚定空间，在时限内保持区块加载 / Keep a chunk loaded for a duration |
| **虚空之手** *Void Hand* | 远程右键交互方块 / Right-click a block from afar |
| **构筑理念方块** *Construct Idea Block* | 在指定位置生成理念方块 / Manifest an Idea Block at a position |
| **天变 / 清涤** *Sky Tint / Clearing* | 在半径内给天空与云染色，持续指定时间 / Dye the sky and clouds within a radius for a duration |
| **幻化 / 复元** *Counterfeit Visage / Restoration* | 让实体的主手物品（或掉落物本身）看起来像另一件物品 / Make a held or dropped item look like another |

## 理念方块 | Idea Blocks

理念方块是一种特殊的建筑方块：手持任意方块右键，它就换上那块方块的外观（直接覆盖，不需要先还原）；手持同一种方块再右键，则依次切换它的朝向、台阶上下等形态。空手潜行右键回到理念本体，潜行时手持方块右键则交还原版、正常放置。默认纹理为 `miehex_revolution:block/idea`。

它模仿的是「方块在那一格被放下时的完整状态」——朝向、台阶上下、楼梯朝向、栅栏/墙/玻璃板的连接、门的铰链全由原版放置逻辑给出，碰撞形状与受光也随外观一同改变，因此台阶、楼梯、栅栏、玻璃板、活板门乃至门都可以放心模仿；门的两半会自动补齐，拆掉一半另一半一起消失。相邻的两个理念方块模仿同一种方块时会互相连接，没有可循环属性时右键也能按当前邻居刷新连接。外观会随方块一起保存，破坏掉落、创造模式中键拾取、重新放下都会带着它。声音为紫水晶。

An Idea Block mimics any block's appearance. Right-click with a block to take on its look (no reset needed first); right-click again with the same block to cycle its facing, slab half and other variants. Shift + empty hand returns it to the plain idea form, while shift + block in hand hands the click back to vanilla so you can place normally. Default texture is `miehex_revolution:block/idea`.

It mimics the full block state that block would get when placed in that very position — facing, slab half, stair shape, fence/wall/pane connections and door hinge all come from vanilla's placement logic — and collision shapes and lighting follow the appearance. So slabs, stairs, fences, panes, trapdoors and even doors are all fair game; a door's other half is filled in automatically and both halves vanish together. Two neighbouring Idea Blocks mimicking the same block connect to each other, and a right-click with nothing to cycle refreshes connections against current neighbours. The appearance is stored with the block, surviving breakage, middle-click pick-block and re-placement. Amethyst sound.

## 联动 | Cross-Mod Compat

理念方块作为"灵魂投射"的目标实体时，支持和 Hexal 的 咒灵 联动（软依赖，Hexal 未安装时不生效）。

Idea Blocks as monitor targets have soft compat with Hexal's wisps — automatically enabled when Hexal is present.
