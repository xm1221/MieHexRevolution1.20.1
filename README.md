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

## 理念方块 | Idea Blocks

理念方块是一种特殊的建筑方块——手持任意方块右键即可改变其外观，空手潜行右键还原。默认纹理为 `miehex_revolution:block/idea`。

An Idea Block is a chameleon-like building block — right-click it with any other block to mimic that block's appearance. Shift-right-click with an empty hand to reset. Default texture is `miehex_revolution:block/idea`. Inherits amethyst sound and particles.

## 联动 | Cross-Mod Compat

理念方块作为"灵魂投射"的目标实体时，支持和 Hexal 的 咒灵 联动（软依赖，Hexal 未安装时不生效）。

Idea Blocks as monitor targets have soft compat with Hexal's wisps — automatically enabled when Hexal is present.
