package net.kenddie.fantasyarmor.item;

import net.kenddie.fantasyarmor.config.FAArmorAttributesConfig;
import net.kenddie.fantasyarmor.config.FAArmorConfig;
import net.kenddie.fantasyarmor.shared.armor.FAArmorAttributes;
import net.kenddie.fantasyarmor.item.armor.FAArmorSet;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class FAArmorItems {
    public static final ArrayList<ArmorType> VALID_ARMOR_TYPES = new ArrayList<>(List.of(
        ArmorType.HELMET,
        ArmorType.CHESTPLATE,
        ArmorType.LEGGINGS,
        ArmorType.BOOTS
    ));
    // EnumEntry -> (ArmorPiece -> RegistryObject<Item> )
    public static final Map<FAArmorSet, Map<ArmorType, DeferredItem<Item>>> ARMOR_ITEMS = new HashMap<>();

    @SuppressWarnings("unchecked")
    private static double safeGet(ModConfigSpec.DoubleValue val) {
        try {
            return val.get();
        } catch (IllegalStateException e) {
            return (double) ((ModConfigSpec.ConfigValue<Double>) (ModConfigSpec.ConfigValue<?>) val).getDefault();
        }
    }

    public static void register(IEventBus eventBus) {
        for (FAArmorSet set : FAArmorSet.values()) {
            Map<ArmorType, DeferredItem<Item>> setPieces = new HashMap<>();

            for (ArmorType type : VALID_ARMOR_TYPES) {
                String name = set.getName() + "_" + type.getName();

                FAArmorAttributesConfig config = FAArmorConfig.ARMOR_CONFIGS.get(set.getName()).get(type.getName());

                Supplier<FAArmorAttributes> attributesSupplier = () -> new FAArmorAttributes.Builder()
                        .armor(safeGet(config.armor))
                        .armorToughness(safeGet(config.armorToughness))
                        .knockbackResistance(safeGet(config.knockbackResistance))
                        .movementSpeed(safeGet(config.movementSpeed))
                        .maxHealth(safeGet(config.maxHealth))
                        .attackDamage(safeGet(config.attackDamage))
                        .attackSpeed(safeGet(config.attackSpeed))
                        .luck(safeGet(config.luck))
                        .durability(safeGet(config.durability))
                        .build();

                DeferredItem<Item> item = FAItems.ITEMS.registerItem(name, props -> set.create(type, attributesSupplier, props));
                setPieces.put(type, item);
            }


            ARMOR_ITEMS.put(set, setPieces);
        }
    }

    public static DeferredItem<Item> getArmorItem(FAArmorSet set, ArmorType type) {
        return ARMOR_ITEMS.get(set).get(type);
    }

    private FAArmorItems() {}
}
