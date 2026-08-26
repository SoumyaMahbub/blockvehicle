package com.blockvehicle.vehicle;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;

public final class VehiclePresetState extends PersistentState {
    private static final String STORAGE_KEY = "blockvehicle_presets";
    private static final Type<VehiclePresetState> TYPE = new Type<>(VehiclePresetState::new, VehiclePresetState::fromNbt, null);
    private final Map<String, Preset> presets = new LinkedHashMap<>();

    public static VehiclePresetState get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(TYPE, STORAGE_KEY);
    }

    public void put(String name, VehicleStructure structure) {
        this.presets.put(normalize(name), new Preset(name.trim(), structure));
        this.markDirty();
    }

    public VehicleStructure get(String name) {
        Preset preset = this.presets.get(normalize(name));
        return preset != null ? preset.structure() : null;
    }

    public boolean remove(String name) {
        boolean removed = this.presets.remove(normalize(name)) != null;
        if (removed) {
            this.markDirty();
        }
        return removed;
    }

    public List<String> getNames() {
        ArrayList<String> names = new ArrayList<>();
        for (Preset preset : this.presets.values()) {
            names.add(preset.name());
        }
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        NbtList list = new NbtList();
        for (Preset preset : this.presets.values()) {
            NbtCompound entry = new NbtCompound();
            entry.putString("name", preset.name());
            entry.put("structure", preset.structure().toNbt());
            list.add(entry);
        }
        nbt.put("presets", list);
        return nbt;
    }

    private static VehiclePresetState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        VehiclePresetState state = new VehiclePresetState();
        NbtList list = nbt.getList("presets", NbtElement.COMPOUND_TYPE);
        for (NbtElement element : list) {
            NbtCompound entry = (NbtCompound)element;
            String name = entry.getString("name").trim();
            if (name.isEmpty() || !entry.contains("structure")) {
                continue;
            }
            state.presets.put(normalize(name), new Preset(name, VehicleStructure.fromNbt(entry.getCompound("structure"))));
        }
        return state;
    }

    private static String normalize(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }

    private record Preset(String name, VehicleStructure structure) {
    }
}
