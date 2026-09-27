# 移植记录 —— stardust → Minecraft 26.1.2

> 正式版。起点：上游 <https://github.com/0xTas/stardust>（MC 1.21.4 / Yarn 命名）。
> 目标：26.1.2（**未混淆**，官方名即真实名）/ JDK 25 / Gradle 9.6.1 / Loom 1.17 / Fabric。
> 「收尾修复记录」一节记录 2026-09-28 这轮人工收尾的全部改法（自动改名流水线解决不了的部分）。

## 现状

| 项目 | 源码规模 | 起始编译错误 | 自动流水线平台期 | **当前编译错误** |
|---|---|---|---|---|
| stardust | 168 文件 / 31,934 行 | 2794 | 274 | **0** |
| BepHaxAddon（对照，另一 agent） | 231 文件 / 46,697 行 | 4686 | 213 | 0 |

- javac 校验：`python E:\Files\tools\portkit\compile.py` → `exit=0 errors=0 sources=168 cpJars=172` → `BUILD OK`
- gradle：`& .\gradlew.bat build --console=plain` → `BUILD SUCCESSFUL`
- 产物：`build\libs\stardust-1.14.0-26.1.2.jar`
- 实机冒烟：见文末「实机核对」一节（结论 + 证据路径）。

## 已完成的自动化（工具在 `E:\Files\tools\portkit\`）
1. `migrate_mappings.py`：obf 名做桥，yarn↔官方映射（1.21.4 与 1.21.11 两套映射都在 `.mappings\`），整体改名类名/简单名/mixin 描述符。
2. `classhier.py`：从 26.1.2 真实 jar 解析继承链（修继承来的成员用）。
3. `compile.py`：复用 MAIN 端真实 `-cp` + 本地 mod jar + `build/compile-cp.txt`（Loom 真编译期 classpath，含 Fabric API）的 javac 校验环。
4. `fix_classes.py` / `fix_symbols.py` / `fix_symtab.py`：按 (类,符号) 精确定位改名。
5. `fix_rules.py` / `fix_imports.py` / `fix_nested.py` / `fix_aw.py`：通用改名规则、yarn 残留 import、嵌套类名、access widener 换命名空间。
6. `dump-cp.gradle`：导出 Loom 编译期 classpath（`build/compile-cp.txt`）。
7. `verify_mixins.py` / `verify_mixins2.py` / `verify_at_targets.py`：@Mixin 目标 + `method="..."` + `@Accessor` + `@At(target)` 批量核对。
8. `mc-smoke.ps1`：装 jar → 启动 → 轮询 latest.log/crash-reports → 截图 → 写 verdict。

本仓库这轮新增的脚本（一次性，放在 `E:\Files\archive\sessions\2026-09\scripts\`）：
`err_group.py` / `err_by_file.py`（错误按簇/按文件聚合）、`probe261.py`（批量 javap 探针 + grep）、
`mixin_fixhelp.py` / `list_shadows.py` / `shadow_audit.py` / `shadow_field_audit.py` / `accessor_audit.py` / `at_audit.py` / `desc_audit.py`（mixin 五类目标批量核对）、
`stardust_patch1..17.py`（本轮补丁批次）、`smoke_stardust_isolated.ps1`（把 BepHax/milky 临时移出 mods 单独冒烟，跑完还原）；
以及 `E:\Files\tools\portkit\verify_mixins_stardust.py`、`E:\Files\tools\portkit\verify_mixins2.py`（原 `verify_mixins.py` 的 MIXDIR 写死成 BepHax 路径，对 stardust 恒返回 0 项）。

## 收尾修复记录（2026-09-28，274 → 0 错）

### A. GUI / 渲染管线（26.1 全面重做）
- `GuiGraphics` → `GuiGraphicsExtractor`；`drawGuiTexture(RenderType::getGuiTextured, sprite, …)` → `blitSprite(RenderPipelines.GUI_TEXTURED, sprite, …)`。
- `Screen.render/renderBackground` → `extractRenderState/extractBackground`；`close()` → `onClose()`；`addSelectableChild` → `addWidget`。
- `AbstractContainerScreen.focusedSlot` → **`hoveredSlot`**；`Slot.getStack()` → `getItem()`。
- `AbstractSelectionList`：`renderEntry` → `extractItem(GuiGraphicsExtractor,int,int,float,E)`；`getScrollY()` → `scrollAmount()`；
  `AbstractSelectionList.Entry` 是 **protected**；`Entry.render/outline` → `extractContent` + `GuiGraphicsExtractor.outline`；
  字段 `hoveredEntry→hovered`、`itemHeight→defaultEntryHeight`、`headerHeight` 已删（本 mixin 取 0）。
- `AbstractContainerWidget` 构造器多了 `AbstractScrollArea.ScrollbarSettings`。
- `BlockEntityRenderer<T,S>` 两类型参数；`Camera.getPosition/getXRot/getYRot` → `position()/xRot()/yRot()`。
- `Screenshot.grab(File,String,RenderTarget,int downscale,Consumer)`；`GameRenderer.setPanoramicMode` → `Camera.enable/disablePanoramicMode`；
  `LevelRenderer.reload()` → `allChanged()`；`GuiTheme.font()` → `textRenderer()`；`Window.getWindow()` → `handle()`。
- `SplashRenderer.extractRenderState` 第 4 参是 **float alpha**（旧 Yarn 是 int）；`GuiGraphicsExtractor.item` 有 3/4/5/6 参重载，注入必须写全描述符。

### B. 网络包记录化 / 存取器化
- `ServerboundContainerClickPacket` = `(int,int,short,byte,ContainerInput,Int2ObjectMap<HashedStack>,HashedStack)`：
  `AutoSmith`/`Grinder`/`AutoMason` 注入统一 `buildClickPacket(...)`，哈希器取 `ClientPacketListener.decoratedHashOpsGenenerator()` + `HashedStack.create`（11 处）。
- `PlayerMoveC2SPacketAccessor`：`@Accessor("pitch")` → `("xRot")`；`RocketMan` `setXRot` → `setPitch(float)`。
- `MultiPlayerGameMode.interact(Player,Entity,EntityHitResult,InteractionHand)`（3 参版已删）。
- `ServerboundInteractPacket(id, hand, Vec3, boolean)`；`ServerboundMovePlayerPacket.Pos`；`ServerboundPlayerActionPacket.getAction()`。
- `Connection`：`sendImmediately(Packet,PacketSendListener,boolean)` → `sendPacket(Packet,ChannelFutureListener,boolean)`（`PacketSendListener` 已删）。
- `ClientboundSetEntityDataPacket` 的 `trackedValues` → `packedItems`；`mc.getConnection().onDisconnect(new DisconnectionDetails(reason))`。

### C. 物品体系（`ArmorItem` / `DiggerItem` 整类删除）
- `ArmorItem` → `AutoSmith` 新增 `isArmor(ItemStack)`（`DataComponents.EQUIPPABLE` + `EquipmentSlot.Type.HUMANOID_ARMOR`），
  `getEquipmentType` 改吃 `ItemStack`，`ArmorItem.Type` → `equipment.ArmorType`（13 处）。
- `DiggerItem` → `stack.has(DataComponents.TOOL)`（`ExpThrowerMixin` / `PagePirate`）。
- `DyeItem.byColor` → 新增 `StardustUtil.dyeItem(DyeColor)`（按 `<color>_dye` 查 `BuiltInRegistries.ITEM`，8 处）。
- `ResolvableProfile` 抽象化 → `createResolved(GameProfile)`，纹理先写 `GameProfile.properties()`。
- `ItemStack.getEntityRepresentation()` 已删；`Category(String, Supplier<ItemStack>)`；`SelectableRecipe` 新 API。

### D. 文本 / 聊天事件（接口 + record 化）
- `ClickEvent`/`HoverEvent` 变**接口**：`new ClickEvent.OpenFile/RunCommand(...)`、`new HoverEvent.ShowText(...)`；`action()` / `command()` / `value()`。
- `Style.withParent` → `applyTo`；`Component.getContent` → `getContents`；final class → 接口强转要 `(Object)` 双转。
- `GameProfile` 变 record：`name()/id()/properties()`。
- `TextFieldHelper.insert` → `insertText`；`makeClipboardGetter/Setter` → `createClipboardGetter/Setter`；`StringRepresentable.asString()` → `getSerializedName()`。
- `Font.trimToWidth` → `plainSubstrByWidth`；`Text::getString` → `Component::getString`；`RawFilteredPair::raw` → `Filterable::raw`。

### E. 数据组件 / NBT / 注册表
- `TagParser.parseTag` → `parseCompoundFully`；`NbtUtils.fromBlockState` → `writeBlockState`；
  `BlockEntity.createFromNbt` → `loadStatic`，`getPosFromTag` 现在要 `ChunkPos`（见 R3）。
- `CustomData.read(MapCodec)` → `CODEC.parse(NbtOps.INSTANCE, copyTag().getCompoundOrEmpty(...))`。
- `SignBlock.getWoodType(Block)` 返回 `WoodType`（旧强转要删）；`ResourceKey.location()` → `identifier()`；
  `Level.isChunkLoaded` → `getChunkSource().hasChunk`；`BlockState.getOutlineShape` → `getShape`；`BlockPos.asLong()` 仍在。
- 音效：`SoundEvents` 字段部分是 `Holder.Reference`（要 `.value()`）、部分直接是 `SoundEvent`（不要）；
  `Sound.getIdentifier()` → `getLocation()`，但 `SoundInstance.getIdentifier()` **仍在**。
- 音乐：`MusicInfo` 删除 → `MusicManager.startPlaying(Music)`；`Minecraft.getMusicInstance` → `getSituationalMusic()`；
  `MusicManager` 字段 `current/timeUntilNextSong` → `currentMusic/nextSongDelay`。
- 声音引擎：`SoundEngine.sources` → `instanceToChannel`（`ChannelAccess.ChannelHandle.channel`，类型 `com.mojang.blaze3d.audio.Channel`，`setPitch/setVolume`）；`tick()` → `tick(Z)V`。
- `WeighedSoundEvents.sounds` → `list`；`AbstractSelectionList` 的 `getEntry/getEntryCount/isSelectedEntry` 已删。

### F. Mixin 目标对齐（`method=` / `@At(target=)` / `@Shadow` / `@Accessor` / `@Invoker`）
- `verify_mixins2.py`：注入 MISS 36 → 0（其中 8 处无法解析的曾加 `require = 0`，后续多数已按真实挂载点重写）。
- 已对齐的注入名（举要）：`onSlotUpdate→slotChanged`、`updateButtons/updatePageButtons→updateButtonVisibility`、
  `renderBossBar→extractBar`、`playSoundToPlayer→playSound`、`stopUsingItem→releaseUsingItem`、`setVelocity→setDeltaMovement`、
  `renderLabelIfPresent→submitNameDisplay`、`renderHeldItemTooltip→extractSelectedItemName`、`handledScreenTick→containerTick`、
  `getFormattedName/getHoverName` 相关 → `getStyledHoverName`、`getValue→get`(NeedleDirectionHelper)、
  `render→extractRenderState`(Screen/InventoryScreen/SplashRenderer)、`renderText→submitSignText`、`onMouseButton→onMouseClick`。
- `@At(target=)` 对齐：`LivingEntity.getVelocity→getDeltaMovement`、`FireworkRocketEntity.explodeAndRemove→explode`、
  `ItemStack.contains→has`、`GuiGraphicsExtractor.drawCenteredTextWithShadow→centeredText`、
  `TextFieldHelper.insert→insertText`、`ChatComponent.addMessage` 增 `GuiMessageSource` 参数。
- `@Shadow` 名对齐（javac 查不出、启动才抛 `InvalidMixinException`，靠实机冒烟逐轮捞出）：
  `getUuid→getUUID`、`getName→getHoverName`、`getAngle→calculate`、`shooter→attachedToEntity`、`currentStack→lastToolHighlight`、
  `nameField→name`、`dirty/signing`（已无对应字段 → mixin 本地 `@Unique`）、`contents→bookAccess`+`pageIndex→currentPage`+`cachedPageIndex→cachedPage`、
  `sounds→list`、`hovered`（泛型擦除描述符不匹配 → 删除，改 `Entry.isMouseOver`）。
- `@Accessor`/`@Invoker` 对齐：`levelCost→cost`、`grind→removeNonCursesFrom`、`transferEnchantments→mergeEnchantsFrom`、
  `sendImmediately→sendPacket`、`nameField→name`、`blockEntity→sign`、`trackedValues→packedItems`、`viewDistance→renderDistance`。
  两处目标已消失的访问器（`BookEditScreenAccessor`、`BookScreenContentsAccessor`）已停用并**从 `stardust.mixins.json` 摘除**（继续注册会抛 `InvalidAccessorException`）。
- `access widener`：Yarn 残留（`EntryListWidget$Entry`、`ResourceLocation` 描述符）已修，否则 `:validateAccessWidener` 让 gradle 构建失败。
- 元数据：`fabric.mod.json` 的 `depends.minecraft` 由 `["${mc_version}"]` 改为 `"~26.1"`。

## 实机核对
- **mixin 应用**：最终 jar 启动到客户端初始化（Render thread / Registering protocols）阶段 **0 条 stardust mixin 报错**。
  此前 11 轮逐条修掉了 Bootstrap / Initializing game 阶段的 stardust mixin 崩溃（`@Shadow` 名、`@Accessor`/`@Invoker` 目标、
  `@At(target=)` 调用点、注入描述符四类），详细过程见 `logs\stardust-smoke-isolated*.log`。
- 证据：`E:\Files\archive\sessions\2026-09\logs\smoke-stardust.txt`（`mc-smoke.ps1` 输出，含 `new crash reports` / `game process` / `errors` 三段），
  截图 `E:\Files\archive\sessions\2026-09\shots\mc-stardust.png`。
- **注意（证据限制）**：
  1. 当前 MAIN 实例里还有另一条线在写的 BepHax，最后一次冒烟（03:30:55）的 crash report 归属 **`bep.mixins.json:FireworkRocketEntityMixin from mod bephax`**
     （同样的 `getVelocity` 旧锚点问题，stardust 侧已修）——**不是 stardust 的**；stardust 自己的 mixin 在该次启动里 0 报错。
  2. PCL 会弹「mod 可能与当前 Minecraft 版本不兼容」的告警对话框，干扰自动启动，截图里目前是桌面+PCL 弹窗，
     **尚不是**「进主菜单 → 逐界面点开」的完整验收证据。已用 `smoke_stardust_isolated.ps1` 临时移出 BepHax/milky 单独冒烟，但进程在窗口出现前退出（无 stardust crash report）。
     **完整 UI 验收未完成**，见 R0。

## 遗留风险 / TODO
- **R0（验收缺口）**：未取得「主菜单可见 + 进世界后逐界面（物品栏/聊天/Tab/暂停菜单/告示牌/书/容器/Xaero/Meteor GUI）无异常」的截图证据。
  stardust 自身 mixin 已 0 报错，但 PCL 版本告警弹窗会干扰自动启动；需要手动点「继续」或在无其它在写 mod 的实例里复测。
- **R1（中）**：以下注入在 26.1 目标被删除/重做，目前 `require = 0` 或已停用（游戏能启动，对应功能降级失效）：
  - `AbstractSignBlockEntityRendererMixin` / `BannerBlockEntityRendererMixin` 的 `render(...)`：26.1 渲染改 `submit(...)` + `BlockEntityRenderState` 两段式，需按新签名重写；
  - `BookEditScreenMixin` 的 `charTyped/finalizeBook/changePage`：26.1 `BookEditScreen` 无此三方法（对应 `updatePageContent/appendPageToBook/pageBack/pageForward`），
    且 `TextFieldHelper.insert` 已改名 `insertText`；书本彩虹格式化写入已改成安全空操作（`stardust$insert` no-op）；
  - `LivingEntityMixin.calcGlidingVelocity`：26.1 该计算已内联，需重新定位 `Math.sqrt` 调用点；
  - `NarratorManagerMixin.narrate(Component)`：26.1 只有 `saySystemNow/narrateMessage`，需确认新播报入口；
  - `ScreenMixin.handleTextClick(Style)`：26.1 改为 `defaultHandleClickEvent(int,boolean,Style)` / `clickCommandAction(int,Style)`，签名不同；
  - `GameMenuScreenMixin`（暂停菜单「Illegal Disconnect」按钮）：`PauseScreen.init` 的 `RowHelper#addChild` 锚点实测扫不到；
  - `EntryListWidgetMixin`（服务端列表 2b2t 时钟加宽可选区）：`extractItem` 第 5 参擦除成 **protected** 的 `AbstractSelectionList$Entry`，
    Java 源码无法命名该类型（泛型上界只能写公开的 `ObjectSelectionList$Entry`）→ 描述符对不上，`require=0` 也无效（描述符不匹配而非找不到目标），
    已把注入摘除、原实现体保留为 `@Unique` 草案待重写。
- **R2（低）**：`mixin/xaero/MinimapRendererMixin` 改走 `renderOutsidePip(...)`（新 `onRender` 第 10 参是 `xaero.lib.*`，不在依赖里）；
  旧路径还会 enqueue PIP 贴图/雷达层，等价性无法静态确认。
- **R3（低）**：`SignHistorian` 读档 `BlockEntity.getPosFromTag` 现在要 `ChunkPos`（tag 内 x/z 是区块内相对坐标），当前按 tag 的 x/y/z 直接重建；跨区块存档若错位需校正。
- **R4（低）**：`AntiToS` 用 `PlayerListS2CPacketAccessor#setProfile` 对 record 字段 `@Mutable` 写入，运行期是否生效需实测；`GameProfileAccessor` 已无人引用（可从 mixins.json 摘除）。
- **R5（低）**：`Honker` 比较 `"minecraft:"+desiredCallId` 时取的是 soundEvent 的 Identifier（原样保留），语义上更像要比 instrument 名。
- **R6（低）**：`buildClickPacket` 未判 `mc.getConnection()` 空（沿用项目既有写法）。
- **R7（低）**：`ServerEntryMixin.canConnect()` 在 26.1 无对应实现 → 退化为恒 `true`（`ServerData.state` 也无访问器）。
- **R8（低）**：与 BepHax 同装时会互相覆盖 Meteor 的同一 mixin 方法（日志：`Method overwrite conflict for onDeactivate ... previously written by bep.hax.mixin.meteor.AutoLogMixin`），后加载者被跳过；单独装 stardust 时无此问题。

## 复现
```powershell
$env:JAVA_HOME='E:\Files\milky-addon-26.1.2\tools\jdk-25.0.4.1+1'
$env:PORT_PROJ='E:\Files\stardust-26.1.2'; $env:PORT_MCVER='1.21.4'
python E:\Files\tools\portkit\compile.py            # javac 校验 → errors=0
& .\gradlew.bat -I E:\Files\tools\portkit\dump-cp.gradle dumpCompileCp --console=plain
& .\gradlew.bat build --console=plain               # → build\libs\stardust-1.14.0-26.1.2.jar
python E:\Files\tools\portkit\verify_mixins2.py E:\Files\stardust-26.1.2\build\mixin-verify.txt
& E:\Files\tools\portkit\mc-smoke.ps1 -Label stardust -Jar .\build\libs\stardust-1.14.0-26.1.2.jar -WaitSec 180
```
