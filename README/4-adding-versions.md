# How to Add a New Minecraft Version

When a new Minecraft version drops and you need to add support for it, this guide walks you through the entire process. Adding a new version means creating a new subproject directory, writing (or adapting) all the loader-specific Java code, setting up resources, and wiring everything into the build system.

Fair warning: this is the most involved task in the project. But if you follow these steps methodically, you'll get through it without too much pain.

## Before You Start

You'll want to have answers to these questions:

1. **Which loader?** Forge, Fabric, or NeoForge?
2. **What Minecraft version?** (e.g., 1.22.0)
3. **What loader version?** Check the loader's website for the latest stable release targeting your MC version
4. **What GeckoLib version?** Check [GeckoLib's releases](https://github.com/bertsmod/geckolib) for the version matching your MC + loader combo
5. **What Parchment mappings version?** Check [ParchmentMC](https://parchmentmc.org/) for available mappings
6. **What changed in the API?** This is the big one. Read the changelogs for MC, the loader, and GeckoLib

## Step 1: Create the Directory Structure

For a new version (let's say NeoForge 1.22.0), create:

```
appNeoForge/
  1.22.0/
    build.gradle
    gradle.properties
    src/
      main/
        java/
          net/kenddie/fantasyarmor/
            (Java source files go here)
        resources/
          (Local resources go here)
        templates/
          META-INF/
            neoforge.mods.toml   (for NeoForge)
```

For Fabric, the metadata file would be `fabric.mod.json` in resources instead of templates. For Forge, it's `mods.toml` in `META-INF/`.

## Step 2: Set Up gradle.properties

Create `gradle.properties` with version-specific values. Use the closest existing version as a starting point. Here's what a NeoForge example looks like:

```properties
minecraft_version=1.22.0
neo_version=22.0.XX
geckolib_version=X.X.X
mod_version=1.X.X-1.22.0
parchment_mappings_version=YYYY.MM.DD
parchment_minecraft_version=1.22.0
java_version=21
```

For Forge versions:
```properties
minecraft_version=1.22.0
forge_version=1.22.0-XX.X.X
geckolib_version=X.X.X
mod_version=1.X.X-1.22.0
parchment_mappings_version=YYYY.MM.DD
parchment_minecraft_version=1.22.0
```

For Fabric versions:
```properties
minecraft_version=1.22.0
loader_version=X.XX.X
fabric_version=X.XX.X+1.22.0
geckolib_version=X.X.X
mod_version=1.X.X-1.22.0
parchment_mappings_version=YYYY.MM.DD
parchment_minecraft_version=1.22.0
```

## Step 3: Write the build.gradle

Again, copy the closest existing version and adapt. The key sections:

### For NeoForge (ModDevGradle)
```gradle
plugins {
    id 'java-library'
    id 'maven-publish'
    id 'net.neoforged.moddev' version 'X.X.X'
    id 'idea'
}

neoForge {
    version = project.neo_version
    parchment {
        mappingsVersion = project.parchment_mappings_version
        minecraftVersion = project.parchment_minecraft_version
    }
    mods {
        "${mod_id}" {
            sourceSet(sourceSets.main)
            sourceSet(project(':appCommon').sourceSets.main)
        }
    }
}
```

### For Fabric (Fabric Loom)
```gradle
plugins {
    id 'fabric-loom' version 'X.X'
    id 'maven-publish'
}

dependencies {
    minecraft "com.mojang:minecraft:${project.minecraft_version}"
    mappings loom.layered() {
        officialMojangMappings()
        parchment("org.parchmentmc.data:parchment-${parchment_minecraft_version}:${parchment_mappings_version}@zip")
    }
    modImplementation "net.fabricmc:fabric-loader:${project.loader_version}"
    modImplementation "net.fabricmc.fabric-api:fabric-api:${project.fabric_version}"
}
```

### For Forge (ForgeGradle)
```gradle
plugins {
    id 'net.minecraftforge.gradle' version '[6.0,6.2)'
    id 'org.parchmentmc.librarian.forgegradle' version '1.+'
}

minecraft {
    mappings channel: 'parchment', version: "${parchment_mappings_version}-${parchment_minecraft_version}"
}
```

### Critical: The Shared Resources and appCommon Integration

Every build.gradle MUST include:

```gradle
implementation project(':appCommon')

jar {
    from project(':appCommon').sourceSets.main.output
}

tasks.named('processResources', ProcessResources).configure {
    from("${rootProject.projectDir}/shared/resources/item_models") { into 'assets/fantasy_armor/models/item' }
    from("${rootProject.projectDir}/shared/resources/items")       { into 'assets/fantasy_armor/items' }
    from("${rootProject.projectDir}/shared/resources/icons")       { into 'assets/fantasy_armor/textures/item' }
    from("${rootProject.projectDir}/shared/resources/armor")       { into 'assets/fantasy_armor/textures/armor' }
    from("${rootProject.projectDir}/shared/resources/geo")         { into 'assets/fantasy_armor/geo' }
    from("${rootProject.projectDir}/shared/resources/lang_forge")  { into 'assets/fantasy_armor/lang' }
    from("${rootProject.projectDir}/shared/resources/tags")        { into 'data/minecraft/tags/item' }
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}
```

Note: The geo destination path may change between versions. For example, GeckoLib 5 on NeoForge uses `geckolib/models` instead of `geo`.

## Step 4: Add the Project to settings.gradle

Add an include line to `settings.gradle`:

```gradle
include(':appNeoForge:1.22.0')
```

## Step 5: Update mc_versions_info.json

Open `resourcesGeneration/mc_versions_info.json` and add a section for the new version. This is important because it tells the code generation system how to generate Java code for your version:

```json
{
  "appNeoForge/1.22.0": {
    "recipes_dir": "recipe",
    "armor_type_class": "ArmorType",
    "armor_type_import": "net.minecraft.world.item.equipment.ArmorType",
    "factory_type": "QuadFunction",
    "factory_type_params": "A, B, C, D, R",
    "factory_apply_params": "A a, B b, C c, D d",
    "factory_field_generics": "FAArmorSet, ArmorType, Supplier<FAArmorAttributes>, Item.Properties, FAArmorItem",
    "create_params": "ArmorType type, Supplier<FAArmorAttributes> attributesSupplier, Item.Properties properties",
    "create_call": "factory.apply(this, type, attributesSupplier, properties)",
    "geo_path_expr": "name + \"_armor\"",
    "has_name_constructor": false
  }
}
```

The key things that change between major API versions:
- **recipes_dir**: `"recipes"` for 1.19.4/1.20.1, `"recipe"` for 1.21.1+
- **armor_type_class**: `"ArmorItem.Type"` for 1.19.4-1.21.1, `"ArmorType"` for 1.21.10+
- **factory_type**: `"TriFunction"` (3 params) for older versions, `"QuadFunction"` (4 params) for newer ones
- **has_name_constructor**: `true` for versions where FAArmorSet has a string name constructor, `false` for newer ones
- **geo_path_expr**: How the geo model path is constructed - older versions use the full path string, newer ones just use the name

## Step 6: Add a Recipe Template

If the new Minecraft version changes the recipe JSON format, create a new template in `resourcesGeneration/recipes/templates/`:

```json
{
    "type": "minecraft:smithing_transform",
    "category": "equipment",
    "template": "TEMPLATE_ITEM",
    "base": "BASE_ITEM",
    "addition": "ADDITION_ITEM",
    "result": "RESULT_ITEM"
}
```

Earlier versions (1.19.4, 1.20.1) use object-based references with `{"item": "..."}` syntax. Newer versions (1.21.1+) use plain string references. Check what format the new MC version expects.

## Step 7: Write the Java Source Files

This is the most time-consuming part. You need all of these files:

### Always Required (All Loaders)

| File | Purpose |
|------|---------|
| `FantasyArmor.java` | Mod entry point, registers everything |
| `FAItems.java` | Item registration (DeferredRegister) |
| `FAArmorItems.java` | Registers all 120 armor items with stats |
| `FAArmorItem.java` | Base armor item class (extends ArmorItem or Item) |
| `FAArmorSet.java` | Armor set enum with factories (GENERATED) |
| `FAArmorSets.java` | Inner classes per set (GENERATED) |
| `FACreativeModTabs.java` | Creative inventory tab |
| `FAArmorEffectHandler.java` | Applies potion effects for full sets |
| `FAConfig.java` | Top-level config holder |
| `FAArmorConfig.java` | Per-set config container |
| `FAArmorAttributesConfig.java` | Per-set stat config values |
| `FAArmorEffectsConfig.java` | Per-set effect config values |

### Client-Side (All Loaders)

| File | Purpose |
|------|---------|
| `FAArmorRenderer.java` | GeckoLib armor renderer |
| `FAArmorModel.java` | GeckoLib model class |
| `FADyeableGeoLayer.java` | Dye overlay render layer |
| `FARenderUtils.java` | Cape/braid physics utilities |
| `FAClientEventHandler.java` | Client-side events |
| `FARenderEventHandler.java` | Render events |

### Fabric-Only

| File | Purpose |
|------|---------|
| `FARenderMixin.java` | Mixin for armor rendering |
| `fantasy_armor.mixins.json` | Mixin config file |

The fastest approach is to copy all files from the closest existing version and adapt them to API changes. `FAArmorSet.java` and `FAArmorSets.java` are generated by `generateJavaSources`, so you just need to make sure mc_versions_info.json is set up (Step 5) and run the generator.

## Step 8: Place Local Resources

Some resources are version-specific and need to live in the subproject:

1. **logo.png** - Copy from any existing version
2. **moon_crystal.png + .mcmeta** - The animated crystal texture (copy from existing version)
3. **moon_crystal.json** (item model) - Simple `item/generated` model pointing to the crystal texture

These go in `src/main/resources/assets/fantasy_armor/`:
```
textures/item/moon_crystal.png
textures/item/moon_crystal.png.mcmeta
models/item/moon_crystal.json
```

4. **Mod metadata** - Create the appropriate metadata file:
   - NeoForge: `src/main/templates/META-INF/neoforge.mods.toml`
   - Fabric: `src/main/resources/fabric.mod.json`
   - Forge: `src/main/resources/META-INF/mods.toml`

## Step 9: Generate Resources

Run the generation system to create recipes and update tags:

```bash
./gradlew generateAll
```

This creates the version-specific recipe files and ensures the Java templates are generated for your new version.

## Step 10: Build and Test

First, make sure it compiles:
```bash
./gradlew :appNeoForge:1.22.0:compileJava
```

If there are compile errors, they're almost always API changes you need to adapt to. The most common ones:

- **Import paths changed** - Check what packages moved
- **Method signatures changed** - Check MC/loader changelogs
- **GeckoLib API changed** - Check GeckoLib migration guides
- **Class hierarchy changed** - Some versions extend different base classes

Once it compiles, run it:
```bash
./gradlew :appNeoForge:1.22.0:runClient
```

Common runtime issues:
- **"Cannot get config value before config is loaded"** - You're accessing config during item registration. Add safe accessor methods with try-catch that fall back to `getDefault()`
- **Missing textures** - Check that processResources is copying shared resources to the right paths
- **Geo model not found** - Check the geo destination path in your build.gradle
- **NullPointerException in rendering** - Usually means a GeckoLib API mismatch. Check your renderer against GeckoLib's current API

## Step 11: Validate In-Game

Once the game runs without crashing:

- [ ] All 30 armor sets appear in the creative tab
- [ ] Each piece can be equipped
- [ ] 3D models render correctly (no missing parts or T-posing)
- [ ] Dye colors apply and display properly
- [ ] Overlay textures render on top of dyed base
- [ ] Cape physics work on sets that have capes
- [ ] Smithing recipes work at the smithing table
- [ ] Config file generates and values can be changed
- [ ] Potion effects apply when wearing full sets

## Understanding API Breakpoints

Based on the versions already in this repo, here are the major API breakpoints to watch for:

### 1.19.4 to 1.20.1
- Recipe directory: `recipes` (stays the same)
- Minimal changes

### 1.20.1 to 1.21.1
- Recipe directory: `recipes` changed to `recipe`
- Various minor API shifts

### 1.21.1 to 1.21.10
This is the big one. See the dedicated API changes documents (docs 5 and 6) for the full breakdown, but the highlights:
- `ArmorItem.Type` moved to `ArmorType` from `net.minecraft.world.item.equipment`
- `FAArmorItem` now extends `Item` instead of `ArmorItem`
- Factory function gains a 4th parameter (`Item.Properties`)
- GeckoLib jumps from 4.x to 5.x (completely new rendering architecture)
- Render state system replaces direct entity access
- DataTicket system for passing data through the render pipeline

### Future Versions
Keep an eye on:
- GeckoLib major version changes (rendering architecture shifts)
- Minecraft item system changes (component system, property builders)
- Loader registration API changes (DeferredRegister patterns)
- Mapping changes (Parchment availability for new versions)

## Tips From Experience

1. **Start with compileJava, not runClient.** Get it compiling first, then worry about runtime.
2. **Copy the closest version.** Don't start from scratch. Take the nearest existing version and adapt.
3. **Check GeckoLib's examples.** Their wiki and example mods are the best reference for rendering changes.
4. **Read the crash report carefully.** It usually tells you exactly which method signature changed.
5. **Config timing is tricky.** In newer versions, items are registered before config loads. If you hit "Cannot get config value before config is loaded", wrap config access in try-catch blocks that fall back to defaults.
6. **Test one version before generating for all.** Get the new version working manually, then update mc_versions_info.json if needed and regenerate.
