# How to Add a New Armor Set

So the creator wants to add a 31st armor set (or 32nd, or 50th). This guide walks you through every single step, from the art assets to the final build. It might look like a lot of steps, but most of them are just "put the file in the right place" and the generation system handles the tedious stuff for you.

## What You Need Before You Start

For a new armor set called (let's say) `crystal_guardian`, you'll need these assets ready:

1. **A GeckoLib 3D model** - `crystal_guardian_armor.geo.json`
2. **A full-body armor texture** - `crystal_guardian_armor.png`
3. **A full-body overlay texture** - `crystal_guardian_armor_overlay.png` (the dye mask)
4. **Four inventory icon textures** (base):
   - `crystal_guardian_helmet.png`
   - `crystal_guardian_chestplate.png`
   - `crystal_guardian_leggings.png`
   - `crystal_guardian_boots.png`
5. **Four inventory icon overlay textures** (for dye support):
   - `crystal_guardian_helmet_overlay.png`
   - `crystal_guardian_chestplate_overlay.png`
   - `crystal_guardian_leggings_overlay.png`
   - `crystal_guardian_boots_overlay.png`

The naming convention matters. Everything uses the set name in `snake_case` with the suffixes shown above.

## Step 1: Place the Art Assets

Drop each file into the corresponding `shared/resources/` directory:

| File | Destination |
|------|------------|
| `crystal_guardian_armor.geo.json` | `shared/resources/geo/` |
| `crystal_guardian_armor.png` | `shared/resources/armor/` |
| `crystal_guardian_armor_overlay.png` | `shared/resources/armor/` |
| `crystal_guardian_helmet.png` | `shared/resources/icons/` |
| `crystal_guardian_helmet_overlay.png` | `shared/resources/icons/` |
| `crystal_guardian_chestplate.png` | `shared/resources/icons/` |
| `crystal_guardian_chestplate_overlay.png` | `shared/resources/icons/` |
| `crystal_guardian_leggings.png` | `shared/resources/icons/` |
| `crystal_guardian_leggings_overlay.png` | `shared/resources/icons/` |
| `crystal_guardian_boots.png` | `shared/resources/icons/` |
| `crystal_guardian_boots_overlay.png` | `shared/resources/icons/` |

That's 11 files total for the art.

## Step 2: Register the Set in armor_sets.json

Open `resourcesGeneration/armor_sets.json` and add your new set name to the array:

```json
{
  "armor_sets": [
    "chess_board_knight",
    "crucible_knight",
    "crystal_guardian",
    ...
  ]
}
```

Keep it alphabetical if you want to stay organized, but it's not mandatory. This list drives everything else in the generation system.

## Step 3: Add a Recipe

Open `resourcesGeneration/recipes/armor_recipes.json` and add an entry for your set:

```json
{
  "crystal_guardian": {
    "template": "minecraft:gold_nugget",
    "addition": "fantasy_armor:moon_crystal"
  },
  ...
}
```

Every armor recipe is a **Smithing Transform**. The pattern is:
- **template** - The item that goes in the template slot (like a gold nugget, iron ingot, etc.)
- **addition** - The item that goes in the addition slot (usually `fantasy_armor:moon_crystal`)
- **base** - This is always `minecraft:netherite_[piece]` and is filled in automatically

So the player would combine a netherite helmet + gold nugget + moon crystal at a smithing table to get the Crystal Guardian helmet.

## Step 4: Add Armor Stats

Open `appCommon/src/main/resources/armor_defaults.json` and add a section for your set:

```json
{
  "crystal_guardian": {
    "helmet": {
      "armor": 3.0,
      "armorToughness": 3.0,
      "knockbackResistance": 0.1,
      "movementSpeed": 0.0,
      "maxHealth": 0.0,
      "attackDamage": 0.0,
      "attackSpeed": 0.0,
      "luck": 0.0,
      "durability": 440.0
    },
    "chestplate": {
      "armor": 8.0,
      "armorToughness": 3.0,
      "knockbackResistance": 0.1,
      "movementSpeed": 0.0,
      "maxHealth": 0.0,
      "attackDamage": 0.0,
      "attackSpeed": 0.0,
      "luck": 0.0,
      "durability": 640.0
    },
    "leggings": {
      "armor": 6.0,
      "armorToughness": 3.0,
      "knockbackResistance": 0.1,
      "movementSpeed": 0.0,
      "maxHealth": 0.0,
      "attackDamage": 0.0,
      "attackSpeed": 0.0,
      "luck": 0.0,
      "durability": 600.0
    },
    "boots": {
      "armor": 3.0,
      "armorToughness": 3.0,
      "knockbackResistance": 0.1,
      "movementSpeed": 0.0,
      "maxHealth": 0.0,
      "attackDamage": 0.0,
      "attackSpeed": 0.0,
      "luck": 0.0,
      "durability": 520.0
    }
  }
}
```

These are the baseline stats. Players can override them through the mod's config file. If you set durability to 0 or omit it, the vanilla netherite durability is used.

## Step 5: Add Potion Effects (Optional)

If your armor set should apply potion effects when the player wears the full set, edit `appCommon/src/main/resources/effect_defaults.json`:

```json
{
  "crystal_guardian": [
    {
      "id": "minecraft:regeneration",
      "duration": 200,
      "amplifier": 0
    },
    {
      "id": "minecraft:resistance",
      "duration": 200,
      "amplifier": 1
    }
  ]
}
```

Duration is in ticks (20 ticks = 1 second). The effects are re-applied continuously while the player wears all four pieces. If you don't want any effects, just don't add an entry for your set.

## Step 6: Add Translations

Open `shared/resources/lang_forge/en_us.json` and add entries for all four pieces plus tooltips:

```json
{
  "item.fantasy_armor.crystal_guardian_helmet": "Crystal Guardian Helmet",
  "item.fantasy_armor.crystal_guardian_helmet.tooltip": "A helm carved from living crystal...",
  "item.fantasy_armor.crystal_guardian_chestplate": "Crystal Guardian Chestplate",
  "item.fantasy_armor.crystal_guardian_chestplate.tooltip": "The chest piece resonates with a deep hum...",
  "item.fantasy_armor.crystal_guardian_leggings": "Crystal Guardian Leggings",
  "item.fantasy_armor.crystal_guardian_leggings.tooltip": "Greaves that shimmer in twilight...",
  "item.fantasy_armor.crystal_guardian_boots": "Crystal Guardian Boots",
  "item.fantasy_armor.crystal_guardian_boots.tooltip": "Boots that leave faint traces of light..."
}
```

If you have translations for other languages, add them to the corresponding files in `shared/resources/lang_forge/` (ja_jp.json, ru_ru.json, uk_ua.json, zh_cn.json).

## Step 7: Run the Generation System

This is where the magic happens. From the project root, run:

```bash
./gradlew generateAll
```

This single command does all of the following:

1. **generateRecipesData** - Creates smithing recipe JSONs for all four pieces in every version directory, using the right format for each MC version
2. **generateTagsData** - Updates `shared/resources/tags/dyeable.json` to include the new armor items
3. **generateEnchantableArmorTags** - Updates the four enchantable tags (head/chest/leg/foot) to include the new pieces
4. **generateSharedItemDefinitions** - Creates component-based item definition JSONs in `shared/resources/items/`
5. **generateSharedItemModels** - Creates item model JSONs (base + dyed) in `shared/resources/item_models/`
6. **generateJavaSources** - Regenerates `FAArmorSet.java` and `FAArmorSets.java` in every version directory with the new set

After this runs, you'll have recipe files in every version's resources, updated tags, new item models, new item definitions, and updated Java source files. All from that one command.

## Step 8: Verify the Generation

Quick sanity checks:

1. **Check FAArmorSet.java** in any version - should now have a `CRYSTAL_GUARDIAN` enum value
2. **Check FAArmorSets.java** in any version - should have a `CrystalGuardianArmorItem` inner class
3. **Check recipes** - look for `crystal_guardian_helmet.json` etc. in any version's recipe directory
4. **Check tags** - open `shared/resources/tags/dyeable.json` and verify your items are listed
5. **Check item models** - look for `crystal_guardian_boots.json` and `crystal_guardian_boots_dyed.json` in `shared/resources/item_models/`

## Step 9: Build and Test

Build one version to make sure everything compiles:

```bash
./gradlew :appForge:1.21.1:build
```

Then run it to test in-game:

```bash
./gradlew :appForge:1.21.1:runClient
```

In-game, you can:
- Use `/give @s fantasy_armor:crystal_guardian_helmet` to get pieces
- Check the creative tab (Fantasy Armor) - all four pieces should appear
- Try dyeing pieces with any dye at a crafting table
- Build at a smithing table using the recipe you defined
- Wear the full set and check if potion effects apply

## What If Your Armor Has Extra Bones?

Some armor sets have special bones beyond the standard helmet/chestplate/leggings/boots. Things like:
- **Capes** (bone name: `cape`)
- **Front capes / tabards** (bone name: `frontCape`)
- **Braids** (bone name: `braid`)
- **Leg cloth / skirt pieces** (bone names: `leftLegCloth`, `rightLegCloth`)

These are handled automatically by `FAArmorRenderer`. If your geo model includes bones with these names, the renderer will:
- Apply physics-based animation to capes using `CapePhysics`
- Sync front capes with leg movement
- Apply head rotation to braids
- Parent leg cloth to the corresponding leg bone

You don't need to write any extra code for this. Just name the bones correctly in your geo model and the renderer picks them up. If a bone doesn't exist in your model, it's silently skipped.

There's also support for an `epicFightCape` bone for compatibility with the Epic Fight mod. Same deal - include it in your model if you need it.

## What If You Need Custom Rendering?

If your armor set needs completely unique rendering behavior (not just the standard dye + extra bones), you'd need to create a custom subclass. Here's the pattern:

1. In `FAArmorSets.java` (or the template), your inner class already extends `FAArmorItem`
2. Override `createGeoRenderer()` to return a custom renderer
3. Create that custom renderer extending `FAArmorRenderer`

But honestly, the existing renderer handles a huge variety of armor designs. You probably won't need this unless you're doing something really exotic.

## Checklist Summary

Here's the quick reference version:

- [ ] Create geo model, textures (11 files total)
- [ ] Place them in `shared/resources/geo/`, `armor/`, `icons/`
- [ ] Add set name to `resourcesGeneration/armor_sets.json`
- [ ] Add recipe to `resourcesGeneration/recipes/armor_recipes.json`
- [ ] Add stats to `appCommon/src/main/resources/armor_defaults.json`
- [ ] (Optional) Add effects to `appCommon/src/main/resources/effect_defaults.json`
- [ ] Add translations to `shared/resources/lang_forge/en_us.json` (and others)
- [ ] Run `./gradlew generateAll`
- [ ] Build and test
