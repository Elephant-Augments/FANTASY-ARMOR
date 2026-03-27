# What Changed From the Original Repo

Hey there! If you're looking at this repo and wondering "wait, this looks different from the original Fantasy Armor source," you're absolutely right. This document walks you through every single change we made and why.

## The Big Picture

The original Fantasy Armor repo (upstream) was a perfectly functional multi-loader Minecraft mod, but it had one major pain point: **code duplication**. Every loader (Forge, Fabric, NeoForge) and every Minecraft version had its own complete copy of shared logic like config handling, armor attributes, cape physics, and effect management. When you wanted to fix a bug in, say, the config system, you had to make that same fix in every single version directory. Miss one? Congrats, you've got a subtle inconsistency that'll bite you later.

We fixed that by introducing a **shared module** called `appCommon` and centralizing all the resources that don't change between loaders into a single `shared/resources/` directory. The result is a repo where common logic lives in exactly one place, and each loader version only contains the code that's genuinely unique to that platform.

## Before: The Original Structure

Here's what the original upstream repo looked like:

```
FANTASY-ARMOR/
  appForge/
    1.19.4/    (complete standalone mod - all Java, all resources)
    1.20.1/    (complete standalone mod - all Java, all resources)
    1.21.1/    (complete standalone mod - all Java, all resources)
  appFabric/
    1.20.1/    (complete standalone mod - all Java, all resources)
    1.21.1/    (complete standalone mod - all Java, all resources)
    1.21.10/   (complete standalone mod - all Java, all resources)
  appNeoForge/
    1.21.1/    (complete standalone mod - all Java, all resources)
  shared/
    resources/   (armor textures, geo models, icons - already shared)
  resourcesGeneration/
    (scripts for generating recipes, tags, item models)
  build.gradle.kts
  settings.gradle
```

Each of those version directories was basically its own self-contained mod. They each had their own copy of:

- `FAArmorAttributes.java` - the record holding armor stats (armor, toughness, knockback resistance, etc.)
- `FAArmorDefaults.java` - the singleton that loads default stats from JSON
- `FAEffectDefaults.java` - the singleton that loads potion effects from JSON
- `EffectEntry.java` - a simple data record for effect configuration
- `CapePhysics.java` - all the math for animating capes on armor
- `FAConfig.java` / config classes - duplicated config infrastructure
- All the resource files (recipes, tags, lang files, item models, textures)

The `shared/resources/` directory already existed for armor textures and geo models, which was great. But most other resources and all the shared Java code lived in every single version folder.

## After: The Unified Structure

```
FANTASY-ARMOR/
  appCommon/                     (NEW - shared Java module)
    build.gradle
    src/main/java/
      net/kenddie/fantasyarmor/shared/
        armor/
          FAArmorAttributes.java   (record with Builder pattern)
        config/
          EffectEntry.java         (simple record)
          FAArmorDefaults.java     (loads armor_defaults.json)
          FAEffectDefaults.java    (loads effect_defaults.json)
        client/
          CapePhysics.java         (pure math for cape animation)
    src/main/resources/
      armor_defaults.json          (all armor stats, one source of truth)
      effect_defaults.json         (all effect definitions)
  appForge/
    1.19.4/    (only Forge 1.19.4-specific code, depends on appCommon)
    1.20.1/    (only Forge 1.20.1-specific code, depends on appCommon)
    1.21.1/    (only Forge 1.21.1-specific code, depends on appCommon)
  appFabric/
    1.20.1/    (only Fabric-specific code, depends on appCommon)
    1.21.1/    (only Fabric-specific code, depends on appCommon)
    1.21.10/   (only Fabric 1.21.10-specific code, depends on appCommon)
  appNeoForge/
    1.21.1/    (only NeoForge-specific code, depends on appCommon)
  shared/
    resources/   (ALL shared resources now - expanded from original)
  resourcesGeneration/
    (generation scripts + Java templates for code generation)
  README/
    (you are here!)
  build.gradle.kts
  settings.gradle
```

## Exactly What Moved Into appCommon

### FAArmorAttributes.java
This is the core data record that represents an armor set's stats. It holds nine values: `armor`, `armorToughness`, `knockbackResistance`, `movementSpeed`, `maxHealth`, `attackDamage`, `attackSpeed`, `luck`, and `durability`. It also has a `Builder` class for convenient construction. Previously, every single version directory had its own copy of this file. Now there's one copy in `appCommon/src/main/java/net/kenddie/fantasyarmor/shared/armor/`.

### FAArmorDefaults.java
This singleton loads `armor_defaults.json` from the classpath and caches the parsed results. When any version of the mod needs to know "what are the default stats for the Eclipse Soldier helmet?", it calls `FAArmorDefaults.get("eclipse_soldier", "helmet")` and gets back an `FAArmorAttributes` record. The JSON file itself also lives in appCommon's resources now.

### FAEffectDefaults.java
Same pattern as FAArmorDefaults but for potion effects. It loads `effect_defaults.json` and provides `get(setName)` to retrieve the list of `EffectEntry` records for a given armor set.

### EffectEntry.java
A tiny record: `record EffectEntry(String id, int duration, int amplifier)`. Represents one potion effect that an armor set applies. This was duplicated everywhere before.

### CapePhysics.java
Pure math utilities for cape animation. Three key methods:
- `computeCapeRotation(flap, lean, lean2, isCrouching)` - returns rotation angles in radians
- `computeCapeMotion(dx, dy, dz, bodyRotDeg, walkBob, walkDist)` - returns `float[3]` with flap/lean/lean2 values
- `computeFrontCapeAngle(leftLegRotX, rightLegRotX)` - calculates front cape draping angle

These methods contain the actual math that makes capes look good. Every version's `FARenderUtils.java` now calls into these shared methods instead of duplicating the trigonometry.

## How Each Loader Integrates With appCommon

Every version's `build.gradle` now includes three key lines:

```gradle
implementation project(':appCommon')

mods {
    "${mod_id}" {
        sourceSet(sourceSets.main)
        sourceSet(project(':appCommon').sourceSets.main)
    }
}

jar {
    from project(':appCommon').sourceSets.main.output
}
```

The first line makes appCommon's classes available at compile time. The second tells the mod loader to treat appCommon's sources as part of the mod. The third ensures appCommon's compiled classes get bundled into the final JAR when you build for distribution.

## What Changed in settings.gradle

The original had seven `include` lines. We added `include(':appCommon')` at the top so Gradle knows about the shared module:

```gradle
include(':appCommon')
include(':appForge:1.21.1')
include(':appNeoForge:1.21.1')
include(':appForge:1.20.1')
include(':appForge:1.19.4')
include(':appFabric:1.20.1')
include(':appFabric:1.21.1')
include(':appFabric:1.21.10')
```

## What Changed in the Resource Generation System

The `resourcesGeneration/` directory got two new Java templates:

- **FAArmorSet.java.template** - A template for generating the `FAArmorSet` enum, which uses version-specific placeholders for factory types, import paths, and geo path expressions
- **FAArmorSets.java.template** - A template for generating the inner classes container

The root `build.gradle.kts` already had tasks for generating recipes, tags, item models, and item definitions. A new `generateJavaSources` task was added that reads `mc_versions_info.json` and these templates to produce version-correct Java source files for each project.

## What Changed in Individual Loader Code

Each loader's code got slimmed down. Here's what happened:

1. **Removed duplicate classes** - `FAArmorAttributes.java`, `FAArmorDefaults.java`, `FAEffectDefaults.java`, `EffectEntry.java`, and `CapePhysics.java` were deleted from every version directory
2. **Updated imports** - Files that referenced the moved classes (like `FAArmorItem.java`, `FARenderUtils.java`, `FAConfig.java`) had their import statements updated to point at `net.kenddie.fantasyarmor.shared.*`
3. **Updated method calls** - `FARenderUtils.java` in each version now delegates to `CapePhysics.*` methods instead of doing the math inline

The actual loader-specific code (registration, rendering, mixins, event handlers) stayed right where it was. Only the truly shared logic moved.

## What About the Config System?

The config infrastructure (`FAConfig.java`, `FAArmorConfig.java`, `FAArmorAttributesConfig.java`, `FAArmorEffectsConfig.java`) stayed in each loader because config systems are deeply loader-specific. Forge uses `ForgeConfigSpec`, NeoForge uses `ModConfigSpec`, and Fabric uses its own config approach. What changed is that these configs now reference `FAArmorDefaults` from appCommon for their default values instead of hardcoding them.

## The Durability System

As part of this refactor branch, a durability system was also added:
- Each armor set can have a configurable durability value
- `FAConfig.enableDurability` controls whether custom durability is active
- When enabled, `FAArmorItem.getMaxDamage()` returns the configured durability instead of the vanilla default
- The durability values come from `FAArmorDefaults` through appCommon

## The Dyeable Armor System (1.21.10)

The Fabric 1.21.10 version received fixes for the dyeable armor system:
- Added missing `dyeable` item tag
- Fixed the dye texture render layer
- Fixed a crash in `appendHoverText` when called from a background thread
- Refactored dye code to match the 1.21.1 naming conventions

## Numbers

To give you a sense of the impact:
- **Before**: Around 60+ duplicated files across versions
- **After**: 5 shared Java files + 2 shared JSON resources in appCommon, eliminating roughly 1,600 lines of duplicated code
- **Net result**: About 1,100 fewer lines of code across the repo while maintaining identical functionality
