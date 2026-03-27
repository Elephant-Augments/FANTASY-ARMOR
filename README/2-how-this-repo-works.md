# How This Repo Works

Welcome! This document is your complete guide to understanding how the Fantasy Armor project is put together. Whether you're building the mod, adding features, or just trying to figure out where something lives, this is the place to start.

## The Project at a Glance

Fantasy Armor is a Minecraft mod that adds 30 unique armor sets with custom 3D models (powered by GeckoLib), dyeable textures, configurable stats, and animated capes. It supports three mod loaders (Forge, Fabric, NeoForge) across multiple Minecraft versions (1.19.4 through 1.21.10).

The repo is organized as a **multi-project Gradle build** where each loader-version combination is its own subproject, and shared code lives in a common module. Think of it like a monorepo for all the different builds of the same mod.

## Directory Layout

```
FANTASY-ARMOR/
  appCommon/           Shared Java code used by all loaders
  appForge/            Forge-specific code
    1.19.4/
    1.20.1/
    1.21.1/
  appFabric/           Fabric-specific code
    1.20.1/
    1.21.1/
    1.21.10/
  appNeoForge/         NeoForge-specific code
    1.21.1/
  shared/              Shared resources (textures, models, lang, tags)
    resources/
  resourcesGeneration/ Code generation system
  README/              Documentation (you are here)
  gradle/              Gradle wrapper and version catalog
  build.gradle.kts     Root build file with generation tasks
  settings.gradle      Project includes and plugin management
  gradle.properties    Mod metadata and JVM settings
```

## appCommon: The Shared Module

This is the heart of the unification. It's a plain Java 17 library with no loader dependencies (no Forge APIs, no Fabric APIs, no NeoForge APIs). That's what makes it safe to include everywhere.

### What It Contains

**FAArmorAttributes.java** (`shared/armor/`)
A record that holds all nine armor stat values: armor, armorToughness, knockbackResistance, movementSpeed, maxHealth, attackDamage, attackSpeed, luck, and durability. Comes with a Builder for convenient construction.

**FAArmorDefaults.java** (`shared/config/`)
A lazy-loading singleton that reads `armor_defaults.json` from the classpath. Call `FAArmorDefaults.get("hero", "helmet")` and you get back the default `FAArmorAttributes` for the Hero helmet. It caches everything after the first load. If a specific set-piece combination isn't found in the JSON, it falls back to sensible per-piece defaults.

**FAEffectDefaults.java** (`shared/config/`)
Same lazy-loading pattern but for potion effects. Call `FAEffectDefaults.get("eclipse_soldier")` and you get a `List<EffectEntry>` describing the status effects that set applies.

**EffectEntry.java** (`shared/config/`)
A simple record: `record EffectEntry(String id, int duration, int amplifier)`. One instance per potion effect.

**CapePhysics.java** (`shared/client/`)
Pure math for cape animation. Three static methods:
- `computeCapeRotation()` takes flap/lean/lean2 values and crouching state, returns rotation angles
- `computeCapeMotion()` takes player motion data, returns cape physics values
- `computeFrontCapeAngle()` calculates how the front cape drapes based on leg positions

**Resource Files**
- `armor_defaults.json` - All 30 armor sets with per-piece stat values
- `effect_defaults.json` - Potion effect definitions per armor set

### How Loaders Depend on It

Every version's `build.gradle` includes:
```gradle
implementation project(':appCommon')
```

And bundles it into the final JAR:
```gradle
jar {
    from project(':appCommon').sourceSets.main.output
}
```

AppCommon's sources are also registered with the mod loader so it can scan them:
```gradle
mods {
    "${mod_id}" {
        sourceSet(sourceSets.main)
        sourceSet(project(':appCommon').sourceSets.main)
    }
}
```

## Loader-Specific Modules (appForge, appFabric, appNeoForge)

Each of these directories contains version-specific subprojects. Every subproject has:

```
appLoader/version/
  build.gradle          Version-specific build config
  gradle.properties     Version numbers, mod version, dependencies
  src/main/
    java/               Loader-specific Java code
    resources/           Loader-specific resources
    templates/           Template files (like neoforge.mods.toml)
```

### What Lives in Loader Code

These are the files that are genuinely different between loaders and versions:

- **FantasyArmor.java** - Mod entry point. Registers items, creative tabs. Different registration APIs per loader.
- **FAItems.java** - Item registration using loader-specific deferred registries.
- **FAArmorItems.java** - Registers all 120 armor items (30 sets x 4 pieces). Uses the FAArmorSet factory pattern.
- **FAArmorItem.java** - The armor item class. Extends different base classes depending on version (ArmorItem in 1.21.1, Item in 1.21.10).
- **FAArmorSet.java** - Enum of all 30 armor sets with factory methods. Generated from templates.
- **FAArmorSets.java** - Inner classes, one per armor set. Generated from templates.
- **FAArmorRenderer.java** - GeckoLib armor renderer with extra bone support (capes, braids, cloth).
- **FAArmorModel.java** - GeckoLib model class pointing to geo/texture resources.
- **FADyeableGeoLayer.java** - Render layer for dye overlay textures.
- **FARenderUtils.java** - Render utility methods that delegate to CapePhysics in appCommon.
- **FACreativeModTabs.java** - Creative inventory tab registration.
- **FAArmorEffectHandler.java** - Applies potion effects when wearing full sets.
- **FAConfig.java** and config classes - Loader-specific config system.
- **FAClientEventHandler.java** / **FARenderEventHandler.java** - Client-side event handlers.

### The Factory Pattern for Armor Sets

Each armor set is represented by an enum value in `FAArmorSet` that holds a factory function reference:

```java
public enum FAArmorSet {
    ECLIPSE_SOLDIER(FAArmorSets.EclipseSoldierArmorItem::new),
    HERO(FAArmorSets.HeroArmorItem::new),
    // ... 28 more
}
```

When registering items, `FAArmorItems` calls `set.create(type, attributes)` which delegates to the factory. This lets each armor set have its own concrete class (for GeckoLib rendering) while keeping registration code clean.

The actual factory function signature varies by version:
- **1.19.4 through 1.21.1**: `TriFunction<FAArmorSet, ArmorItem.Type, Supplier<FAArmorAttributes>, FAArmorItem>`
- **1.21.10+**: `QuadFunction<FAArmorSet, ArmorType, Supplier<FAArmorAttributes>, Item.Properties, FAArmorItem>`

## shared/resources/: The Resource Hub

All resources that don't change between loaders live here. Each loader's `build.gradle` copies them into the right locations during `processResources`:

```gradle
tasks.named('processResources', ProcessResources).configure {
    from("${rootProject.projectDir}/shared/resources/item_models") { into 'assets/fantasy_armor/models/item' }
    from("${rootProject.projectDir}/shared/resources/items")       { into 'assets/fantasy_armor/items' }
    from("${rootProject.projectDir}/shared/resources/icons")       { into 'assets/fantasy_armor/textures/item' }
    from("${rootProject.projectDir}/shared/resources/armor")       { into 'assets/fantasy_armor/textures/armor' }
    from("${rootProject.projectDir}/shared/resources/geo")         { into 'assets/fantasy_armor/geo' }
    from("${rootProject.projectDir}/shared/resources/lang_forge")  { into 'assets/fantasy_armor/lang' }
    from("${rootProject.projectDir}/shared/resources/tags")        { into 'data/minecraft/tags/item' }
}
```

Note: Some versions use a different geo path. For example, NeoForge 1.21.10 maps geo to `geckolib/models` instead of just `geo`.

### What's in shared/resources/

| Directory | Contents | Count |
|-----------|----------|-------|
| `armor/` | Full-body armor textures (base + overlay per set) | ~62 files |
| `geo/` | GeckoLib 3D model definitions | ~31 files |
| `icons/` | Individual piece textures (helmet, chest, legs, boots + overlays) | ~240 files |
| `items/` | Component-based item definitions with dye conditional rendering | ~124 files |
| `item_models/` | Item model JSONs (base + dyed variants) | ~240 files |
| `lang_forge/` | Localization files (en_us, ja_jp, ru_ru, uk_ua, zh_cn) | 5 files |
| `tags/` | Dyeable + enchantable item tags | 5 files |

### Resources That Stay Local

Some resources are version-specific and stay in each loader's `src/main/resources/`:

- **Recipes** (`data/fantasy_armor/recipe/`) - Format differs between MC versions
- **logo.png** - Mod logo
- **moon_crystal.png + .mcmeta** - Animated crafting ingredient texture
- **moon_crystal.json** (item model) - Simple item model for the crystal
- **Mod metadata** (fabric.mod.json, neoforge.mods.toml) - Loader-specific

## The Resource Generation System

The `resourcesGeneration/` directory plus the root `build.gradle.kts` form a code generation system. This is how you keep 30 armor sets consistent across 7+ version directories without going insane.

### Data Files

- **armor_sets.json** - Array of all 30 armor set names
- **mc_versions_info.json** - Per-version config: recipe directory names, Java type names, import paths, factory function types
- **recipes/armor_recipes.json** - Per-set crafting ingredients (template item + addition item)
- **recipes/templates/** - Version-specific recipe JSON templates
- **item_models/templates/** - Templates for item model JSONs (armor.json, armor_dyed.json)
- **java_templates/** - Templates for FAArmorSet.java and FAArmorSets.java

### Gradle Generation Tasks

Run these from the root project:

| Task | What It Generates |
|------|------------------|
| `generateRecipesData` | Smithing transform recipes for all sets x all versions |
| `generateTagsData` | dyeable.json tag listing all armor items |
| `generateEnchantableArmorTags` | Four enchantable tags (head/chest/leg/foot) |
| `generateSharedItemDefinitions` | Component-based item JSONs with dye conditional rendering |
| `generateSharedItemModels` | Item model JSONs (base + dyed variants) |
| `generateJavaSources` | FAArmorSet.java + FAArmorSets.java for each version |
| `generateAll` | Runs all of the above |

## Building the Mod

### One version at a time
```bash
./gradlew :appForge:1.21.1:build
./gradlew :appFabric:1.21.10:build
./gradlew :appNeoForge:1.21.1:build
```

### Running a client for testing
```bash
./gradlew :appForge:1.21.1:runClient
./gradlew :appFabric:1.21.10:runClient
./gradlew :appNeoForge:1.21.1:runClient
```

### Regenerating all generated files
```bash
./gradlew generateAll
```

### Building everything
```bash
./gradlew build
```

The output JARs land in each subproject's `build/libs/` directory.

## Key Configuration Files

### gradle.properties (root)
```properties
mod_id=fantasy_armor
mod_name=Fantasy Armor
mod_license=DSMSLv2
mod_group_id=net.kenddie.fantasyarmor
mod_authors=Kenddie, mon1tor
```
Plus JVM settings for Gradle (6GB heap, daemon enabled, parallel builds, caching).

### gradle.properties (per version)
Each version directory has its own `gradle.properties` with:
- `minecraft_version` - Target MC version
- `loader_version` / `forge_version` / `neo_version` - Loader version
- `geckolib_version` - GeckoLib dependency version
- `mod_version` - Build version string
- `parchment_mappings_version` / `parchment_minecraft_version` - Mapping versions

## How the Armor Rendering Pipeline Works

This is the fun part. Every armor piece goes through this pipeline:

1. **Item Registration** - `FAArmorItems` creates items using `FAArmorSet.create()`, passing the set enum, armor type, and attribute supplier
2. **GeckoLib Integration** - Each item implements `GeoItem` and returns a renderer via `createGeoRenderer()`
3. **FAArmorRenderer** - Custom `GeoArmorRenderer` subclass that:
   - Sets up the dye overlay layer (`FADyeableGeoLayer`)
   - Handles extra bones (capes, braids, cloth pieces) not part of standard armor
   - Applies cape physics via `FARenderUtils` which delegates to `CapePhysics` in appCommon
4. **FAArmorModel** - Points GeckoLib to the right geo model and texture for each set
5. **FADyeableGeoLayer** - Checks if the item has a dye color applied and renders the overlay texture with that tint

The rendering architecture changed significantly between GeckoLib 4 (1.21.1 and earlier) and GeckoLib 5 (1.21.10), which is covered in detail in the API changes documents.

## How Config Works

Each loader has its own config system but they follow the same pattern:

- **FAConfig** - Top-level config holder with `enableDurability` and `applyModifiers` flags
- **FAArmorConfig** - Container for per-set armor configs
- **FAArmorAttributesConfig** - Per-set configurable stat values (defaults come from appCommon's FAArmorDefaults)
- **FAArmorEffectsConfig** - Per-set configurable potion effects (defaults come from appCommon's FAEffectDefaults)

Config values are loaded lazily and can't be accessed before the config system initializes. In versions where items are registered before config loads (like NeoForge 1.21.10), safe accessor methods with try-catch fallbacks handle this timing issue.

## Localization

All translations live in `shared/resources/lang_forge/` and are copied to every version during build:

- `en_us.json` - English (primary)
- `ja_jp.json` - Japanese
- `ru_ru.json` - Russian
- `uk_ua.json` - Ukrainian
- `zh_cn.json` - Simplified Chinese

Each entry follows the pattern:
```json
{
  "item.fantasy_armor.eclipse_soldier_helmet": "Eclipse Soldier Helmet",
  "item.fantasy_armor.eclipse_soldier_helmet.tooltip": "The armor of an infantryman..."
}
```
