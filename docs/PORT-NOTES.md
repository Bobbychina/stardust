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

### H. 联合冒烟暴露的运行期问题（04:11–04:29 修复）
- **`NullPointerException: Components not bound yet`（Bootstrap 崩整局）**：`StardustUtil` 的
  `discIcons/doorIcons/menuIcons` 是 `static final` 立即初始化、内含 `Items.X.getDefaultInstance()`；
  任何一次 `StardustUtil.<clinit>`（首个触发者是 `RocketMan` 的字段 `private String rcc = StardustUtil.rCC();`）
  都会在 Meteor 建模块阶段（组件系统尚未 bind）炸。→ 三个表改**惰性方法**（`discIcons()/doorIcons()/menuIcons()`，首次调用才 `new ItemStack[]`）；
  `RocketMan`/`MusicTweaks` 的 `rcc` 字段同样改惰性 `rcc()`。
- **模块查找空指针（渲染/音效高频路径）**：`X x = modules.get(Y.class);` 在启动期可能返回 null，
  直接 `.isActive()` 会在 `Minecraft.renderFrame` / `SoundEngine.tick` 里炸。→ 全项目统一补空守卫
  **41 处自动插入 + 6 处人工**（含 `SoundSystemMixin`、`MinecraftClientMixin`、`EntityRendererMixin`、
  `ChatHudMixin`、`ItemStackMixin`、`AbstractSignBlockEntityRendererMixin`、`EntityMixin`、
  `FireworkRocketEntityMixin`、`SignHistorian`、`SignatureSign` 等；非 void 方法用 `return <原值>`）。
- **`@ModifyVariable` 隐式变量修饰失败 → `EntityRendererMixin` 改挂 `getNameTag`**：
  26.1 名字显示改为 `EntityRenderer.getNameTag(T)` 产出 `Component`，原来的
  `@ModifyVariable(method="submitNameDisplay", argsOnly=true)` 既命中重载歧义、又根本没有 Component 参数
  （26.1 的参数是 `RenderState/PoseStack/SubmitNodeCollector/CameraRenderState`）→ 改在 `getNameTag` 的 `RETURN` 上 `cir.setReturnValue(...)`。
- **`CategoryAccessor` 类型错**：Meteor 26.1 的 `Category.icon` 是 `Supplier<ItemStack>`（不是 `ItemStack`）→
  `@Accessor("icon") void setIcon(Supplier<ItemStack>)`，调用点改 `setIcon(StardustUtil::chooseMenuIcon)`。
- **`PeekScreenMixin` 描述符过期**：26.1 鼠标回调是 `mouseClicked(MouseButtonEvent, boolean)` →
  全描述符 `mouseClicked(Lnet/minecraft/client/input/MouseButtonEvent;Z)Z`，按钮号走 `event.button()`。
- **`SplashTextRendererMixin` 颜色 arg 已不存在**：26.1 splash 文本改走
  `ActiveTextCollector.accept(TextAlignment,int,int,Parameters,Component)`，没有颜色 int 参数 →
  改成 `@ModifyArg(index=4)` 改写传入的 `Component` 样式（`withColor(0x54FB54)`）；
  代价：`Style` 颜色不含 alpha，原实现叠加的淡出 alpha 丢失（TODO 已注明）。
- **三处「裸方法名 + 目标类有重载」的歧义注入**（新写的 `ambig_audit.py` 抓到，静态核对盲区）：
  `BossHealthOverlay.extractBar`（2 重载）、`InventoryScreen.extractRenderState`（2）、`Level.playLocalSound`（3）
  → 全部改成完整描述符。
- `BookEditScreenMixin` 的失效锚点 `TextFieldHelper.insert` → `insertText`（该注入本身仍 `require = 0`，见 R1）。
- 新增核对器（`E:\Files\archive\sessions\2026-09\scripts\`）：`ambig_audit.py`（重载歧义 + 隐式 `@Accessor` 推导字段）、
  `shadow_field_audit.py`、`accessor_audit.py`、`at_audit.py`、`desc_audit.py` —— 五个维度把
  `@Shadow` / `@Accessor`+`@Invoker` / `@At(target=)` / 完整描述符 / 重载歧义全部静态核一遍（stardust 侧现均 0 MISS）。

## 实机核对
### 联合冒烟（stardust + BepHax + 全部其它 mod，同一 MAIN 实例）
- 轮次：`joint3`（04:11，撞 BepHax 的 SoundEngine NPE）→ `joint3b`（04:15，撞 BepHax 的 Minecraft.renderFrame NPE）
  → `joint3c`（04:20，BepHax 修复版尚未安装）→ `joint3d`（04:24，修掉 stardust 自己的 `SplashTextRendererMixin`）
  → **`joint3e`（04:29）结果：`new crash reports (0)` + `errors (0)`，stardust 侧 0 条 mixin 报错**。
- 证据：`E:\Files\archive\sessions\2026-09\logs\smoke-joint3e.txt`；
  **主菜单截图 `E:\Files\archive\sessions\2026-09\shots\mc-joint3e-menu.png`
  （sha256 `1E741074418B9FC5812BCE4BD737A01EF23C6AD37651D56F6FBFF2698D481BE9`，肉眼可见 Minecraft Java Edition 主菜单：单人/多人/Realms/模组）**。
  取图脚本 `scripts\shot_mc_printwindow.ps1`（`PrintWindow(PW_RENDERFULLCONTENT)` 离屏抓窗口，绕开前台锁）。
- ⚠️ `mc-smoke.ps1` 的 `javaw alive` 判据对本机会误报 `False`：PCL 用 `java.exe` 启动，
  实际进程是 `java`（PID 2716，`MainWindowTitle = 'Minecraft* 26.1.2'`），游戏当时**确实在运行**。
- ⚠️ **进世界 + 逐界面截图未完成**：本机 MC/GLFW 不处理脚本注入的鼠标/键盘事件（`mouse_event`/`keybd_event`，
  即使已用 `AttachThreadInput` 拿到前台 `forceForeground=True`），7 张 `mc-joint3e-*.png` 里点击后仍停在主菜单 —— 见 R0。
- 本轮之前共 12 轮 stardust 单独冒烟，逐条修掉了 Bootstrap / Initializing game 阶段的 mixin 崩溃
  （`@Shadow`/`@Accessor`/`@Invoker` 名、`@At(target=)` 调用点、注入描述符、歧义重载、静态初始化触组件 六类），
  过程见 `logs\stardust-smoke-isolated*.log`。

## 遗留风险 / TODO
- **R0（验收缺口，已部分补上）**：**主菜单截图已拿到**（joint 环境，见「实机核对」一节，含 sha256）；
  「进世界 + 逐界面（暂停/物品栏/聊天/Tab/告示牌/书/容器/Xaero/Meteor GUI）」**仍未完成** ——
  本机 MC/GLFW 不响应脚本注入的鼠标/键盘（`mouse_event`/`keybd_event` 无效，即使窗口已拿到前台），需要人工点几下。
- **R9（中，主 agent 加）**：`mixin/FireworkRocketEntityMixin.java` 的
  `@ModifyConstant(method = "tick", constant = @Constant(doubleValue = 1.5))` 在 26.1 里 `Scanned 0 target(s)`
  （`tick()` 内的 1.5 常量已被改写/消除），Bootstrap 期直接崩整局 → 加 `require = 0` 兜底。
  **代价：RocketMan 的 `boostSpeed`（烟花火箭加速倍率）失效**。要恢复需按 26.1 `tick()` 的
  实际常量/调用序列重新定位锚点。
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

## 联合实机验收（2026-09-28 04:44，main agent 复核）
- milky / stardust / bephax 三者同时装载启动 MC 26.1.2（79 mod）→ `logs\smoke-final.txt`：**`new crash reports (0)` + `errors (0)` + `game alive = True` + `verdict: PASS`**；
  截图 `shots\mc-final.png`、`shots\mc-joint3e-menu.png`（sha256 `1E741074…`）肉眼确认主菜单（`Minecraft* 26.1.2`，右上角 Meteor Client / Bep Hax 品牌行）。
- 更新 R0：**主菜单级联合验收已通过**；剩余仅为「进世界逐界面点击」这一人工项（本机 GLFW 不响应脚本注入键鼠）。
- 已知脚本坑（已修）：PCL 以 `java.exe` 启动，`mc-smoke.ps1` 早期只查 `javaw` 会误报；现改为 `java|javaw` + 窗口标题过滤。

## 与另一 mod 共存（BepHax × stardust 同装方案，2026-09-28 08:50）
两个 mod 同源（BepHax 是 stardust 的分支加强版），同装时原始状态会互相顶掉，落地了三件事：

1. **同名模块/命令去重**：重名共 33 个（模块+命令）。规则=设置项数量多者胜、平局保留 bephax（其 mixin 当前优先应用）。
   - stardust 侧停用 31 条注册（`Stardust.java` 中带 `// [共存去重]` 注释的行），bephax 侧停用 2 条（`AdBlocker`、`AutoSmith` —— 这两项 stardust 版设置项更多：4>2、23>11）。
   - 结果：功能取**并集**，两边独有模块都保留（bephax 90 + stardust 独有的 9 个左右）。
   - 需要恢复某一项时：把对应注释行还原、并停用另一侧同名注册即可。
2. **混入同名方法重命名**：13 个非覆盖方法在 stardust 侧加 `stardust$` 前缀（消除 `Method overwrite conflict ... Skipping method`），声明与调用点同步改。
   - 唯一保留的冲突：`AutoLogMixin.onDeactivate`（`@Override` 目标类既有方法，无法双份）→ **bephax 侧生效**，stardust 侧同名功能已在上一步去重中由 bephax 提供。
3. **争抢同一注入点的处理**：`FireworkRocketEntity` 的 `@ModifyConstant(tick, 1.5)` 两边都改 → 移除 stardust 侧实现（`require=0` 已无意义），加速由 bephax 的 RocketMan 提供（R9 相应更新）。

**验证（同装、MC 26.1.2、79 mod）**：
- `logs\smoke-coexist.txt` → `new crash reports (0)` + `errors (0)` + `game alive = True` + `verdict: PASS`
- 本次实机（用户操作）：`latest.log` 里 `Mixin apply for mod stardust/bephax failed` **0 条**，`BEPHAX LOADED.` 与 `Stardust initialized.` 都在；多人游戏界面（`Minecraft* 26.1.2 - 多人游戏（第三方服务器）`）正常。
- 仅剩 1 条已知 WARN：`Method overwrite conflict for onDeactivate`（见上，功能已由 bephax 提供）。

### 懒加载 mixin 修复（本次实机暴露，同上述共存改动一起提交）
- stardust `ServerEntryMixin`：26.1 的 `ServerSelectionList$OnlineServerEntry` 无 `list` 字段 → 改 `@Shadow @Final ServerSelectionList this$0`；其 `swap(int,int)` 为 private（Java 不允许 `private abstract` 影子）→ 新增 `accessor/ServerEntrySwapInvoker`（`@Invoker("swap")`）调用。
- stardust `meteor/WHeaderMixin`：26.1 Meteor `WWindow$WHeader.onMouseClicked(MouseButtonEvent, boolean)` → 处理函数签名同步改。
- 教训：**主菜单只覆盖启动路径**，GUI/界面的 mixin 要开对应界面才应用；验收必须逐个界面点开。
