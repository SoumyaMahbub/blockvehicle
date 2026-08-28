package com.blockvehicle.config;

import com.blockvehicle.BlockVehicleMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.math.MathHelper;

/** Small dependency-free JSON config with aggressively clamped server-safe values. */
public final class BlockVehicleConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static volatile Values values = new Values();

    private BlockVehicleConfig() {
    }

    public static Values get() {
        return values;
    }

    public static void load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("blockvehicle.json");
        Values loaded = new Values();
        try {
            if (Files.exists(path)) {
                try (Reader reader = Files.newBufferedReader(path)) {
                    Values parsed = GSON.fromJson(reader, Values.class);
                    if (parsed != null) loaded = parsed;
                }
            }
            loaded.sanitize();
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(loaded, writer);
            }
        } catch (IOException | RuntimeException exception) {
            BlockVehicleMod.LOGGER.warn("Could not load blockvehicle.json; using safe defaults", exception);
            loaded = new Values();
            loaded.sanitize();
        }
        values = loaded;
    }

    public static final class Values {
        public int maxVehicleBlocks = 8192;
        public int maxVehicleAxis = 96;
        public int maxPropellers = 8;
        public int maxPropellerBladeBlocks = 512;
        public boolean entityCollisionDamage = false;
        public boolean propellerCollision = false;
        public double planeThrustMultiplier = 1.0;
        public double planeLiftMultiplier = 1.0;
        public double planeDragMultiplier = 1.0;
        public double planeControlAssist = 1.0;
        public double cameraRollStrength = 0.35;
        public double cameraHorizonStabilization = 0.65;
        public double cameraSizeDistanceMultiplier = 0.90;
        public double cameraMaxDistance = 48.0;
        public double cameraMaxFovBoost = 12.0;

        private void sanitize() {
            this.maxVehicleBlocks = MathHelper.clamp(this.maxVehicleBlocks, 64, 8192);
            this.maxVehicleAxis = MathHelper.clamp(this.maxVehicleAxis, 16, 128);
            this.maxPropellers = MathHelper.clamp(this.maxPropellers, 0, 16);
            this.maxPropellerBladeBlocks = MathHelper.clamp(this.maxPropellerBladeBlocks, 0, 1024);
            this.planeThrustMultiplier = finiteClamp(this.planeThrustMultiplier, 0.25, 2.5, 1.0);
            this.planeLiftMultiplier = finiteClamp(this.planeLiftMultiplier, 0.4, 2.0, 1.0);
            this.planeDragMultiplier = finiteClamp(this.planeDragMultiplier, 0.5, 2.0, 1.0);
            this.planeControlAssist = finiteClamp(this.planeControlAssist, 0.0, 1.5, 1.0);
            this.cameraRollStrength = finiteClamp(this.cameraRollStrength, 0.0, 1.0, 0.35);
            this.cameraHorizonStabilization = finiteClamp(this.cameraHorizonStabilization, 0.0, 1.0, 0.65);
            this.cameraSizeDistanceMultiplier = finiteClamp(this.cameraSizeDistanceMultiplier, 0.35, 2.0, 0.90);
            this.cameraMaxDistance = finiteClamp(this.cameraMaxDistance, 8.0, 72.0, 48.0);
            this.cameraMaxFovBoost = finiteClamp(this.cameraMaxFovBoost, 0.0, 25.0, 12.0);
        }

        private static double finiteClamp(double value, double min, double max, double fallback) {
            return Double.isFinite(value) ? MathHelper.clamp(value, min, max) : fallback;
        }
    }
}
