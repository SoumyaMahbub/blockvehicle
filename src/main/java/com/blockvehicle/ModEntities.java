package com.blockvehicle;

import com.blockvehicle.BlockVehicleMod;
import com.blockvehicle.entity.VehicleEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public class ModEntities {
    public static final RegistryKey<EntityType<?>> VEHICLE_KEY = RegistryKey.of(RegistryKeys.ENTITY_TYPE, (Identifier)Identifier.of("blockvehicle", "vehicle"));
    public static final EntityType<VehicleEntity> VEHICLE = (EntityType)Registry.register((Registry)Registries.ENTITY_TYPE, VEHICLE_KEY, EntityType.Builder.create(VehicleEntity::new, (SpawnGroup)SpawnGroup.MISC).dimensions(4.0f, 3.0f).maxTrackingRange(10).trackingTickInterval(2).build(VEHICLE_KEY));

    public static void register() {
        BlockVehicleMod.LOGGER.info("Registering ModEntities for blockvehicle");
    }
}
