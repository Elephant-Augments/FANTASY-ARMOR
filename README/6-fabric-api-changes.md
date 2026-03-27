# Fabric API Changes: 1.21.1 to 1.21.10

This covers every API change between Fabric 1.21.1 and 1.21.10 for this mod. A lot of the changes mirror what happened on NeoForge (same Minecraft version, same GeckoLib version), but Fabric has its own quirks — especially around item registration, mixins, and the dye color system.

If you've already read the NeoForge API changes document, some of this will feel familiar. But Fabric handles things differently enough that you'll want to read through this too.

## The Big Picture

17 Java files are shared between both versions. One file — `FAClientEventHandler.java` — exists only in 1.21.1 and was completely removed in 1.21.10. No new files were added. Of the 17 shared files, 3 are identical (`FantasyArmor.java`, `FAArmorEffectHandler.java`, `FAConfig.java`), and the rest range from minor type changes to complete rewrites.

### Dependencies

| Dependency | 1.21.1 | 1.21.10 |
|---|---|---|
| `minecraft_version` | `1.21.1` | `1.21.10` |
| `loader_version` | `0.18.2` | `0.18.4` |
| `fabric_api_version` | `0.116.7+1.21.1` | `0.138.4+1.21.10` |
| `geckolib_version` | **`4.8.2`** | **`5.3-alpha-3`** |
| `cloth-config` | `15.0.140` | `20.0.149` |
| Parchment mappings | `1.21.1:2024.11.17` | `1.21.10:2025.10.12` |

The GeckoLib jump from 4.x to 5.x is the single biggest driver of changes.

---

## Recurring Changes

Just like on NeoForge, these show up everywhere:

| What Changed | 1.21.1 | 1.21.10 |
|---|---|---|
| Armor type enum | `ArmorItem.Type` | `ArmorType` from `net.minecraft.world.item.equipment.ArmorType` |
| Armor materials package | `net.minecraft.world.item.ArmorMaterials` | `net.minecraft.world.item.equipment.ArmorMaterials` |
| GeckoLib AnimatableManager | `software.bernie.geckolib.animation.AnimatableManager` | `software.bernie.geckolib.animatable.manager.AnimatableManager` |

---

## FAItems.java — Registration Overhaul

Fabric doesn't have NeoForge's `DeferredRegister`, so item registration works differently. The change here is significant.

### 1.21.1: Direct Registration

```java
public static final Item MOON_CRYSTAL = register("moon_crystal", new Item(new Item.Properties()));

private static Item register(String name, Item item) {
    return Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(..., name), item);
}
```

### 1.21.10: ResourceKey + Properties Factory

```java
public static Item MOON_CRYSTAL;  // non-final, set later

public static Item register(String name, Function<Item.Properties, Item> factory) {
    ResourceKey<Item> key = ResourceKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(..., name));
    Item.Properties props = new Item.Properties().setId(key);
    Item item = factory.apply(props);
    return Registry.register(BuiltInRegistries.ITEM, key, item);
}
```

In 1.21.10, items must be created with a `ResourceKey` that's set on the properties *before* the item is constructed. The register method now takes a factory function instead of a pre-built item. And `MOON_CRYSTAL` becomes non-final because it's assigned during registration rather than at field declaration.

---

## FAArmorItems.java — Armor Registration

Same conceptual change as `FAItems`, but with armor-specific details.

### Registration Pattern

```java
// 1.21.1
ResourceLocation id = ResourceLocation.fromNamespaceAndPath(FantasyArmor.MOD_ID, name);
FAArmorItem item = set.create(type, attributesSupplier);
Item registered = Registry.register(BuiltInRegistries.ITEM, id, item);
```

```java
// 1.21.10
ResourceKey<Item> key = ResourceKey.create(Registries.ITEM,
        ResourceLocation.fromNamespaceAndPath(FantasyArmor.MOD_ID, name));
Item.Properties props = new Item.Properties().setId(key);
FAArmorItem item = set.create(type, attributesSupplier, props);
Item registered = Registry.register(BuiltInRegistries.ITEM, key, item);
```

The key difference: `ResourceLocation` became `ResourceKey<Item>`, and `Item.Properties` with `.setId(key)` must be created before the item.

### Null Safety

1.21.10 added null checks to `getArmorItem()` that throw `IllegalStateException` if a set or piece isn't found, instead of returning null silently.

---

## FAArmorItem.java — Same Major Rewrite as NeoForge

The changes here are almost identical to the NeoForge version. The fundamental shift is the same: `extends ArmorItem` → `extends Item`.

### Constructor: 3 Params → 4 Params

```java
// 1.21.1
protected FAArmorItem(FAArmorSet armorSet, ArmorItem.Type type,
                      Supplier<FAArmorAttributes> attributesSupplier) {
    super(ArmorMaterials.NETHERITE, type, buildProperties(attributesSupplier));
}
```

```java
// 1.21.10
protected FAArmorItem(FAArmorSet armorSet, ArmorType type,
                      Supplier<FAArmorAttributes> attributesSupplier, Item.Properties properties) {
    super(buildProperties(properties, type, attributesSupplier.get()));
    this.armorType = type;
}
```

### buildProperties Now Takes Properties

```java
// 1.21.1
private static Properties buildProperties(Supplier<FAArmorAttributes> attributesSupplier) {
    return new Properties().stacksTo(1).fireResistant();
}
```

```java
// 1.21.10
private static Item.Properties buildProperties(Item.Properties properties, ArmorType armorType,
                                                FAArmorAttributes armorAttributes) {
    return properties.stacksTo(1).fireResistant()
            .humanoidArmor(ArmorMaterials.NETHERITE, armorType)
            .component(DataComponents.ATTRIBUTE_MODIFIERS, buildAttributeModifiers(armorType, armorAttributes));
}
```

### Attribute Modifiers: Instance → Static

Same as NeoForge: `buildModifiers()` instance method became `buildAttributeModifiers(ArmorType, FAArmorAttributes)` static method. The `cachedModifiers` field and `getDefaultAttributeModifiers()` override are gone.

### New Field

```java
private final ArmorType armorType;
```

With a getter `getArmorType()`.

### Removed Fields

- `private final Supplier<FAArmorAttributes> attributesSupplier` — no longer stored
- `private ItemAttributeModifiers cachedModifiers` — modifiers baked into properties

### Removed Methods

Same list as NeoForge: `isEnchantable()`, `getDefaultAttributeModifiers()`, `createArmorRenderer()`. The durability lifecycle hooks (`syncDurabilityComponent`, `getDefaultInstance`, etc.) were already not present in the Fabric 1.21.1 version, so that's not a change here.

### DyedItemColor Constructor

```java
// 1.21.1: new DyedItemColor(color, false)
// 1.21.10: new DyedItemColor(color)
```

### Tooltip API

```java
// 1.21.1
void appendHoverText(ItemStack, Item.TooltipContext, List<Component>, TooltipFlag)
// Methods: tooltip.add(component), currentLine.length() > 0

// 1.21.10
void appendHoverText(ItemStack, TooltipContext, TooltipDisplay, Consumer<Component>, TooltipFlag)
// Methods: tooltipAdder.accept(component), !currentLine.isEmpty()
```

Also, 1.21.10 adds a guard: `if (!RenderSystem.isOnRenderThread()) return;` at the top of `appendHoverText()`. This prevents crashes when the tooltip is accessed from a non-render thread.

### GeoRenderProvider

Same change as NeoForge:
```java
// 1.21.1: getGeoArmorRenderer(LivingEntity, ItemStack, EquipmentSlot, HumanoidModel) → HumanoidModel<?>
// 1.21.10: getGeoArmorRenderer(ItemStack, EquipmentSlot) → GeoArmorRenderer<?, ?>
```

---

## FAArmorSet.java — TriFunction → QuadFunction

Identical changes to NeoForge:

```java
// 1.21.1
TriFunction<FAArmorSet, ArmorItem.Type, Supplier<FAArmorAttributes>, FAArmorItem> factory;

// 1.21.10
QuadFunction<FAArmorSet, ArmorType, Supplier<FAArmorAttributes>, Item.Properties, FAArmorItem> factory;
```

`create()` gains the `Item.Properties` parameter. `getGeoPath()` changes from `"geo/" + name + "_armor.geo.json"` to just `name + "_armor"`.

One difference from NeoForge: in 1.21.1, Fabric's `FAArmorSet` had two constructors (default name + custom name). In 1.21.10, only the default name constructor remains.

---

## FAArmorSets.java — Constructor Ripple

Same as NeoForge. All 29 inner classes change from:
```java
(FAArmorSet, Type, Supplier<FAArmorAttributes>)
```
to:
```java
(FAArmorSet, ArmorType, Supplier<FAArmorAttributes>, Item.Properties)
```

---

## FARenderMixin.java — Major Rewrite

This is unique to Fabric (NeoForge uses event subscribers instead of mixins). The mixin target and injection point both changed.

### 1.21.1: Mixin into PlayerRenderer.render()

```java
@Mixin(PlayerRenderer.class)
public class FARenderMixin {
    @Inject(method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;render(...)V"))
    private void onRender(AbstractClientPlayer player, float entityYaw, float partialTick,
                         PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                         CallbackInfo ci) {
        Item chestItem = player.getItemBySlot(EquipmentSlot.CHEST).getItem();
        if (chestItem instanceof FAArmorItem) {
            PlayerModel<?> playerModel = ((PlayerRenderer)(Object)this).getModel();
            FARenderUtils.setArmsVisibility(playerModel, false);
        }
    }
}
```

### 1.21.10: Mixin into PlayerModel.setupAnim()

```java
@Mixin(PlayerModel.class)
public class FARenderMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V",
            at = @At("TAIL"))
    private void fa_hideArmsIfArmor(AvatarRenderState state, CallbackInfo ci) {
        ItemStack chestStack = state.chestEquipment;
        if (chestStack != null && chestStack.getItem() instanceof FAArmorItem) {
            PlayerModel model = (PlayerModel)(Object)this;
            model.rightArm.visible = false;
            model.leftArm.visible = false;
            model.rightSleeve.visible = false;
            model.leftSleeve.visible = false;
        }
    }
}
```

Key changes:
- **Mixin target**: `PlayerRenderer` → `PlayerModel` — we now hook into the model's animation setup instead of the renderer
- **Injection point**: `INVOKE` of parent `render()` → `TAIL` of `setupAnim()` — we inject at the end of animation setup
- **Entity access**: `AbstractClientPlayer player` → `AvatarRenderState state` — the render state replaces direct entity access
- **Chest item access**: `player.getItemBySlot(EquipmentSlot.CHEST)` → `state.chestEquipment`
- **Arm hiding**: Delegates to `FARenderUtils.setArmsVisibility()` → Sets visibility directly on model parts
- **Method name**: `onRender` → `fa_hideArmsIfArmor` — prefixed to avoid mixin conflicts

---

## FAClientEventHandler.java — Removed Entirely

This file only exists in 1.21.1. It was a `ClientModInitializer` that registered:

1. **Item color providers** via `ColorProviderRegistry.ITEM` — applied dye tint colors to armor items at tint index 1
2. **Item properties** via `ItemProperties.register()` — added a `fantasy_armor:dyed` boolean predicate that returned `1.0` if the item had a `DYED_COLOR` component

In 1.21.10, both of these are replaced by the data-driven item model system. The `assets/fantasy_armor/items/*.json` files now handle dye color rendering using `minecraft:condition`/`minecraft:has_component` checks and `minecraft:dye` tint types.

The `fabric.mod.json` also reflects this: the `entrypoints.client` array that pointed to `FAClientEventHandler` is removed.

---

## FAArmorRenderer.java — Complete Rendering Overhaul

Same massive changes as NeoForge.

### Class Signature

```java
// 1.21.1
FAArmorRenderer<T extends FAArmorItem> extends GeoArmorRenderer<T>

// 1.21.10
FAArmorRenderer<T extends FAArmorItem, R extends HumanoidRenderState & GeoRenderState>
        extends GeoArmorRenderer<T, R>
```

### Instance Fields → Static Methods

1.21.1 had 5 `GeoBone` instance fields. All gone in 1.21.10, replaced by per-frame `bakedModel.getBone()` lookups in static methods.

### Render Color

```java
// 1.21.1: Color getRenderColor(T, float, int) → Color.WHITE
// 1.21.10: int getRenderColor(T, RenderData, float) → 0xFFFFFFFF
```

### Layer Registration

```java
// 1.21.1: addRenderLayer(new FADyeableGeoLayer<>(this))
// 1.21.10: this.withRenderLayer(r -> new FADyeableGeoLayer<>((GeoArmorRenderer) r))
```

### Removed Methods (all moved to new pipeline)

`grabRelevantBones()`, `applyBoneVisibilityBySlot()`, `applyBoneVisibilityByPart()`, `preRender()`, `applyBaseTransformations()`, `setAllVisible()`

### Added Methods

`captureDefaultRenderState()`, `buildRenderTask()`, `applyExtraBonesStatic()`, `renderExtraBonesStatic()`, `renderExtraBone()`, `isRenderedBySegmentTree()`

---

## FADyeableGeoLayer.java — DataTicket System

Same rewrite as NeoForge. `GeoRenderLayer<T>` became `TextureLayerGeoLayer<T, RenderData, R>`. The `render()` method became `submitRenderTask()`. DataTickets (`HAS_DYE_TICKET`, `DYE_COLOR_TICKET`, `OVERLAY_TEX_TICKET`) replaced direct animatable access.

---

## FAArmorModel.java — Model Resource Methods

```java
// 1.21.1: getModelResource(T animatable), getTextureResource(T animatable)
// 1.21.10: getModelResource(GeoRenderState), getTextureResource(GeoRenderState)
// getAnimationResource(T animatable) — unchanged in both
```

---

## FARenderUtils.java — Player → AvatarRenderState

Same changes as NeoForge:

- `applyCapeRotation(Player, GeoBone, float)` → `applyCapeRotation(AvatarRenderState, GeoBone)`
- `setFrontLegCapeAngle(GeoArmorRenderer<T>, GeoBone)` → `setFrontLegCapeAngle(GeoBone, GeoBone, GeoBone)`
- `applyBraidRotation(Player, GeoBone, float)` → `applyBraidRotation(AvatarRenderState, GeoBone)`
- `setArmsVisibility(PlayerModel<T>, boolean)` → `setArmsVisibility(PlayerModel, boolean)` (raw type)

---

## FAArmorEffectsConfig.java — One API Change

```java
// 1.21.1: BuiltInRegistries.MOB_EFFECT.getHolder(id)
// 1.21.10: BuiltInRegistries.MOB_EFFECT.get(id)
```

---

## FAConfigs.java (Fabric's Config Entry Point)

```java
// 1.21.1: armorSupplier() takes ArmorItem.Type
// 1.21.10: armorSupplier() takes ArmorType
```

Also gains a private constructor in 1.21.10.

---

## Resource Changes

### GeckoLib Geo Models Path

```
# 1.21.1 (build.gradle)
from("${rootProject.projectDir}/shared/resources/geo") { into 'assets/fantasy_armor/geo' }

# 1.21.10 (build.gradle)
from("${rootProject.projectDir}/shared/resources/geo") { into 'assets/fantasy_armor/geckolib/models' }
```

GeckoLib 5 expects geo models under `geckolib/models/` instead of `geo/`.

### Item Definitions (New in 1.21.10)

1.21.10 adds `assets/fantasy_armor/items/*.json` files. These are copied from `shared/resources/items/` during `processResources`. They use the data-driven item model system:

```json
{
  "model": {
    "type": "minecraft:condition",
    "property": "minecraft:has_component",
    "component": "minecraft:dyed_color",
    "on_true": {
      "type": "minecraft:model",
      "model": "fantasy_armor:hero_helmet_dyed",
      "tints": [{ "type": "minecraft:constant", "value": -1 },
                { "type": "minecraft:dye", "default": -1 }]
    },
    "on_false": {
      "type": "minecraft:model",
      "model": "fantasy_armor:hero_helmet"
    }
  }
}
```

This replaces the programmatic `ItemProperties.register()` + `ColorProviderRegistry` approach from `FAClientEventHandler`.

### Recipe JSON Format

```json
// 1.21.1 — nested objects
{
  "template": { "item": "minecraft:iron_ingot" },
  "base": { "item": "minecraft:iron_helmet" },
  "addition": { "item": "fantasy_armor:moon_crystal" },
  "result": { "id": "fantasy_armor:hero_helmet" }
}

// 1.21.10 — flat strings
{
  "template": "minecraft:iron_ingot",
  "base": "minecraft:iron_helmet",
  "addition": "fantasy_armor:moon_crystal",
  "result": "fantasy_armor:hero_helmet"
}
```

### fabric.mod.json

- Removed: `entrypoints.client` pointing to `FAClientEventHandler`
- Changed: `depends.minecraft` from `~1.21.1` to `~1.21.10`
- Changed: `depends.geckolib` from `>=4.8.2` to `>=5.3-alpha-3`

### fantasy_armor.mixins.json — Unchanged

The mixin config file is identical even though `FARenderMixin.java` was rewritten. The class reference `FARenderMixin` didn't change.

---

## Fabric vs NeoForge: Key Differences in the Migration

While most API changes are shared (they're the same Minecraft version and GeckoLib version), here's what's different about the Fabric side:

| Aspect | Fabric Approach | NeoForge Approach |
|---|---|---|
| Item registration | `Registry.register()` with `ResourceKey` + factory pattern | `DeferredRegister.Items.registerItem()` with lambda |
| Config timing | Not an issue (Fabric loads config earlier) | `safeGet()` wrapper needed for early config access |
| Arm visibility | Mixin into `PlayerModel.setupAnim()` | Was in `RenderPlayerEvent.Pre/Post`, now in renderer pipeline |
| Color/dye registration (1.21.1) | `FAClientEventHandler` as `ClientModInitializer` | `FAClientEventHandler` as `@EventBusSubscriber` |
| Color/dye registration (1.21.10) | Both use data-driven item models (same) | Both use data-driven item models (same) |
| Config system | Cloth Config + custom JSON files | NeoForge's built-in `ModConfigSpec` |

---

## Quick Reference: Import Migration Table

| 1.21.1 Import | 1.21.10 Import |
|---|---|
| `net.minecraft.world.item.ArmorItem` | `net.minecraft.world.item.equipment.ArmorType` |
| `net.minecraft.world.item.ArmorMaterials` | `net.minecraft.world.item.equipment.ArmorMaterials` |
| `net.minecraft.world.entity.player.Player` | `net.minecraft.client.renderer.entity.state.AvatarRenderState` |
| `net.minecraft.client.player.AbstractClientPlayer` | *(removed from mixin)* |
| `net.minecraft.client.renderer.entity.player.PlayerRenderer` | `net.minecraft.client.model.PlayerModel` *(mixin target)* |
| `software.bernie.geckolib.animation.AnimatableManager` | `software.bernie.geckolib.animatable.manager.AnimatableManager` |
| `software.bernie.geckolib.util.Color` | *(removed — use raw int ARGB)* |
| `software.bernie.geckolib.renderer.layer.GeoRenderLayer` | `software.bernie.geckolib.renderer.layer.TextureLayerGeoLayer` |
| *(not needed)* | `software.bernie.geckolib.renderer.base.GeoRenderState` |
| *(not needed)* | `software.bernie.geckolib.constant.DataTickets` |
