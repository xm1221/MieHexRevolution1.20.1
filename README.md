# MieHex:Revolution

[![powered by hexdoc](https://img.shields.io/endpoint?url=https://hexxy.media/api/v0/badge/hexdoc?label=1)](https://github.com/hexdoc-dev/hexdoc)

MieHex:Revolution 是 Hex Casting 的扩展模组，为咒法体系引入全新图案、理念方块（Idea Block）、理念碎片（Idea Shard）等实用机制。

MieHex:Revolution is a Hex Casting addon introducing new patterns, Idea Blocks, Idea Shards, and novel mechanics.

---

## 图案 | Patterns

### 法术 | Spells

| 图案 Pattern | 说明 Description |
|-------------|-----------------|
| **灵魂投射 / 灵质之纯化 / 反观之分解** *Soul Projection / Spiritual Purification / Retrospective Purification* | 投射意识至实体共享视野 / Project your soul to share an entity's vision |
| **时之锚** *Temporal Anchor* | 保持区块加载指定时长 / Keep a chunk loaded for a duration |
| **虚空之手** Ⅰ/Ⅱ/Ⅲ *Void Hand* | 远程右键方块 / 副手物品交互 / 指定物品交互 / Remote block/item interaction |
| **虚空之手 卓越型** *Void Hand: Superior* | 卓越法术：远程交互或攻击实体，消耗随距离指数增长 / Great Spell: interact with or attack a distant entity |

### 心理学法术 | Entity Manipulation

| 图案 Pattern | 说明 Description |
|-------------|-----------------|
| **心理暗示** *Suggestion* | 驱策生物AI导航至坐标 / Compel a creature's AI to walk to a point |
| **精神操作** *Compulsion* | 按向量偏移生物一步 / Force a creature to take one step along a vector |
| **涅墨西斯之纯化** *Nemesis' Purification* | 返回最后伤害目标生物的实体 / Return the entity that last hurt the target |
| **阿瑞斯之纯化** *Ares' Purification* | 读取生物的当前仇恨目标 / Read a mob's current attack target |
| **特异性躁狂** *Specific Mania* | 设定生物的仇恨/躲避目标 / Set a creature's hostility or fear target |
| **同步** *Synchronize* | 将施法者朝向复制到目标生物 / Copy caster's orientation to the target |

### 实用 | Utility

| 图案 Pattern | 说明 Description |
|-------------|-----------------|
| **极大/极小之精思** *Maximum/Minimum Reflection* | 获取数值极限 / Get max/min numerical limits |
| **学者之翻页** *Scholar's Page-Turn* | 隔空翻阅副手法术书 / Flip offhand spellbook remotely |
| **画地为牢之策略** *Delimiting Stratagem* | 以列表为栈执行图案 / Evaluate patterns with a list as stack |
| **大定位器之策略** *Locator's Stratagem* | 返回列表中所有匹配项的索引 / Return indices of matching list elements |
| **行程/足迹之精思** *Reflection of Journey/Footsteps* | 查询法环位置总数/已消耗数 / Query circle's known/reached positions |

### 物品 | Items

| 图案 Pattern | 说明 Description |
|-------------|-----------------|
| **构筑理念方块** *Construct Idea Block* | 生成可模仿方块外观的理念方块 / Manifest a chameleon Idea Block |
| **铭刻理念碎片** *Inscribe Idea Shard* | 消耗淬灵晶碎片，写入目标与法术 / Consume a Quenched Shard to inscribe target+spell |

---

## 理念方块 | Idea Blocks

理念方块是一种特殊的建筑方块——手持任意方块右键改变外观，空手潜行右键还原。声音为紫水晶。

An Idea Block mimics any block's appearance. Right-click with a block to apply, shift+empty hand to reset. Amethyst sound.

## 理念碎片 | Idea Shards

副手持淬灵晶碎片，施放「铭刻理念碎片」写入目标 iota 与法术列表。手持碎片右键远程执行——若写入的是向量或实体，无视距离限制。消耗碎片自身的 30 份紫水晶粉储魔。

Inscribe an iota and a pattern list into a Quenched Shard. Right-click to cast at any distance if the stored iota is a vector or entity. Draws from 30 amethyst dust stored in the shard; consumed on use.

## 联动 | Cross-Mod Compat

灵魂投射支持 Hexal 咒灵的软联动（Hexal 未安装时不生效）。

Soul Projection has soft compat with Hexal's wisps — enabled when Hexal is present.

### Sigils：咒印导入语法 | Sigils: Imports Syntax

安装 HexParse 时，ImportsIota 会在 HexParse 代码中以 `Sigils` 语法读写，可直接在 HexParse 编辑器中粘贴使用（未安装 HexParse 时此功能自动失效）。

When HexParse is installed, ImportsIota is spelled as `Sigils` syntax inside HexParse code — paste it straight into the HexParse editor. Without HexParse this feature is simply disabled.

**基本语法 | Basic syntax**

`Sigils:图案→值、图案→值`（空集合写作 `Sigils:`）：

```
Sigils:_NE_aqaa→5、hexal:foo→true
```

- 前缀 `Sigils` + 半角或全角冒号 `:` `：`
- 条目以 `、`（U+3001）分隔，每条在第一个 `→`（U+2192）处切开
- 值可为任意 HexParse 能解析的单 token iota，也可嵌套 `Sigils:` 或列表

**图案键 | Pattern keys**

图案键按**带起始方向的笔顺签名**导出，例如 `_NE_aqaa`：`_` + 起始方向码（`NE`/`E`/`SE`/`SW`/`W`/`NW`）+ `_` + 角度签名。这保证往返（导出再导入）时精确还原当初绑定的图案，不受世界注册表或图案名歧义影响。

Pattern keys are exported as stroke-order signatures **with the start direction**, e.g. `_NE_aqaa` — `_` + direction code (`NE`/`E`/`SE`/`SW`/`W`/`NW`) + `_` + angle signature. Round-trips reproduce the exact bound pattern, independent of world registries or name ambiguity.

**列表值 | List values**

值可为列表，使用全角花括号与全角逗号：

```
Sigils:pat→｛get_caster，entity_pos/eye，get_caster，get_entity_look，raycast｝
```

- 元素以 `，`（U+FF0C）分隔，支持空列表 `｛｝` 与嵌套
- 不能使用半角 `[ ]`（HexParse 会把半角括号切成独立 token，破坏单 token 语法）

List values use full-width braces and commas — empty `｛｝` and nesting are supported. Half-width `[ ]` cannot be used (HexParse splits them into separate tokens).

**嵌套 | Nesting**

值可以是另一个 `Sigils:`，递归展开，深度上限 16：

```
Sigils:a→Sigils:b→1
```

Values may nest another `Sigils:`, recursively (depth limit 16).
