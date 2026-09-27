# 移植进度（WIP）—— BepHaxAddon / stardust → Minecraft 26.1.2

> 这份是中途存档，记录当前状态与剩余工作；完成后会改写为正式 PORT-NOTES。

## 现状（本轮结束时）
| 项目 | 源码规模 | 起始编译错误 | 当前编译错误 |
|---|---|---|---|
| BepHaxAddon（起点 1.21.11） | 231 文件 / 46,697 行 | 4686 | **460** |
| stardust（起点 1.21.4） | 168 文件 / 31,934 行 | 2794 | **345** |

## 已完成的自动化（工具在 `E:\Files\tools\portkit\`）
1. `migrate_mappings.py`：obf 名做桥，yarn↔官方映射（1.21.4 与 1.21.11 两套映射都在 `.mappings\`），整体改名类名/简单名/mixin 描述符。
2. `classhier.py`：从 26.1.2 真实 jar 解析继承链（修继承来的成员用）。
3. `compile.py`：复用 MAIN 端真实 `-cp` + 本地 mod jar 的 javac 校验环（含从 fabric-loader 里解出的 MixinExtras）。
4. `fix_classes.py` / `fix_symbols.py`：按 (类,符号) 精确定位改名（描述符精确配对，避免同名串号）。
5. `fix_rules.py`：通用改名规则表（背包访问器、Vec3/Identifier/Registry 静态工厂、距离比较、注册键、
   实体遍历、movement 包类、`sendPacket→send`、`handleInventoryMouseClick→handleContainerInput`、
   `displayClientMessage→sendSystemMessage`、`GuiGraphics→GuiGraphicsExtractor` 等）。
6. `fix_imports.py`：yarn 名残留 → 官方名；挪包/大小写类 → 真实类表；ChunkPos 私有字段 → 访问器。
7. `fix_nested.py`：嵌套类名（`ServerboundMovePlayerPacket.Full → .PosRot` 这类）。
8. `fix_aw.py`：access widener 换 `official` 命名空间 + 类/成员改名。

## 剩余工作（按类）
- **API 被 26.1 重做的部分**（要改代码逻辑，不是改名）：
  - GUI 渲染：`GuiGraphics` → `GuiGraphicsExtractor`（方法也变了：`drawString`→`text`、`drawBorder`→`outline`）；
  - 物品：`DiggerItem`/`ArmorItem` 已不存在（26.1 用 data component/`Equippable`）；
  - 书本：`BookViewScreen.Contents` 没了、`BookEditScreen` 相关 API 变化；
  - 音乐：`MusicInfo` 没了（`MusicManager` 体系重做）；
  - 文本：`ClickEvent`/`HoverEvent` 变成 record；
  - 包/命令：`then(LiteralArgumentBuilder<ClientSuggestionProvider>)`（Meteor 命令 API 变化）；
  - 杂项：`Reference<Item>`→`Item`、`ClientboundDisconnectPacket`→`DisconnectionDetails`、
    `Entity.getRotationVector()` 返回 `Vec2`、`SignBlock.getWoodType()`→`type()`。
- **mixin 复核**：stardust ~70 个、BepHax 若干，需逐个对着 26.1.2 真实成员核注入目标（`verify_mixins.py`）。
- **构建验证**：`gradlew build`（Loom 1.17 / JDK 25 / Gradle 9.6.1，镜像已配好）。
- **实机冒烟 + fork 推送**。

## 复现
```powershell
$env:PORT_PROJ='E:\Files\BepHaxAddon-26.1.2'; $env:PORT_MCVER='1.21.11'
python E:\Files\tools\portkit\fix_rules.py
python E:\Files\tools\portkit\compile.py
python E:\Files\tools\portkit\port_loop.py 8
```

## 2026-09-28 02:30 存档（重启 dsh 前）
- 本轮自动化已收敛到平台期：**BepHax 213 错 / stardust 274 错**（自动 pass 每轮几乎无改动）。
- 剩余属于「26.1 重做 API」清单（见上）：GUI extractor 方法族、packet 记录化、`PlayerFaceRenderer`、`Box.from` 等零散点。
- 工具链：`E:\Files\tools\portkit\`（grind.py 串起全部 pass；`PORT_PROJ` / `PORT_MCVER` / `PORT_MAPS` 环境变量指定项目与映射）。
- 恢复方式：设好环境变量 → `python E:\Files\tools\portkit\grind.py 3` 看收敛情况，再按 docs 里清单人工改。
