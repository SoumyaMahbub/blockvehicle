package com.blockvehicle;

import com.blockvehicle.item.VehicleWandItem;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public class ModItems {
    private static final RegistryKey<Item> VEHICLE_WAND_KEY = RegistryKey.of(RegistryKeys.ITEM, (Identifier)Identifier.of("blockvehicle", "vehicle_wand"));
    public static final Item VEHICLE_WAND = new VehicleWandItem(new Item.Settings().registryKey(VEHICLE_WAND_KEY).maxCount(1));

    public static void register() {
        Registry.register((Registry)Registries.ITEM, VEHICLE_WAND_KEY, VEHICLE_WAND);
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(content -> content.add(VEHICLE_WAND));
    }
}

