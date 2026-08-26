package com.blockvehicle;

import com.blockvehicle.block.DriverSeatBlock;
import com.blockvehicle.block.PassengerSeatBlock;
import com.blockvehicle.block.VehicleCoreBlock;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;

public class ModBlocks {
    private static final RegistryKey<Block> VEHICLE_CORE_KEY = ModBlocks.key("vehicle_core");
    private static final RegistryKey<Block> DRIVER_SEAT_KEY = ModBlocks.key("driver_seat");
    private static final RegistryKey<Block> PASSENGER_SEAT_KEY = ModBlocks.key("passenger_seat");
    public static final Block VEHICLE_CORE = new VehicleCoreBlock(AbstractBlock.Settings.create().registryKey(VEHICLE_CORE_KEY).mapColor(MapColor.CYAN).strength(3.0f, 6.0f).luminance(state -> (Boolean)state.get(VehicleCoreBlock.ACTIVATED) != false ? 12 : 0));
    public static final Block DRIVER_SEAT = new DriverSeatBlock(AbstractBlock.Settings.create().registryKey(DRIVER_SEAT_KEY).mapColor(MapColor.BLUE).strength(1.5f, 3.0f).nonOpaque());
    public static final Block PASSENGER_SEAT = new PassengerSeatBlock(AbstractBlock.Settings.create().registryKey(PASSENGER_SEAT_KEY).mapColor(MapColor.GRAY).strength(1.5f, 3.0f).nonOpaque());

    private static RegistryKey<Block> key(String name) {
        return RegistryKey.of(RegistryKeys.BLOCK, (Identifier)Identifier.of("blockvehicle", name));
    }

    public static void register() {
        ModBlocks.registerBlock(VEHICLE_CORE_KEY, VEHICLE_CORE);
        ModBlocks.registerBlock(DRIVER_SEAT_KEY, DRIVER_SEAT);
        ModBlocks.registerBlock(PASSENGER_SEAT_KEY, PASSENGER_SEAT);
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(content -> {
            content.add(VEHICLE_CORE.asItem());
            content.add(DRIVER_SEAT.asItem());
            content.add(PASSENGER_SEAT.asItem());
        });
    }

    private static void registerBlock(RegistryKey<Block> blockKey, Block block) {
        Identifier id = blockKey.getValue();
        Registry.register((Registry)Registries.BLOCK, blockKey, block);
        RegistryKey itemKey = RegistryKey.of(RegistryKeys.ITEM, (Identifier)id);
        Registry.register((Registry)Registries.ITEM, itemKey, new BlockItem(block, new Item.Settings().registryKey(itemKey)));
    }
}

