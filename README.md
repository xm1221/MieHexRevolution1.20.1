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
