package com.blockvehicle.vehicle;

import com.blockvehicle.vehicle.SeatData;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.block.AbstractRailBlock;
import net.minecraft.block.AbstractSignBlock;
import net.minecraft.block.BannerBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ButtonBlock;
import net.minecraft.block.CarpetBlock;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.FenceBlock;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.LadderBlock;
import net.minecraft.block.LanternBlock;
import net.minecraft.block.LeverBlock;
import net.minecraft.block.PaneBlock;
import net.minecraft.block.PressurePlateBlock;
import net.minecraft.block.RodBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.TorchBlock;
import net.minecraft.block.TransparentBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.block.TripwireBlock;
import net.minecraft.block.TripwireHookBlock;
import net.minecraft.block.WallBannerBlock;
import net.minecraft.block.WallBlock;
import net.minecraft.block.WallTorchBlock;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class VehicleStructure {
    private final List<StoredBlock> blocks;
    private final List<StoredBlock> renderableBlocks;
    private final List<StoredBlock> contactBlocks;
    private final List<StoredBlock> collisionProbeBlocks;
    private final List<SeatData> seats;
    private final List<WheelData> wheels;
    private final List<StoredItemFrame> itemFrames;
    private final int width;
    private final int height;
    private final int length;
    private final Vec3d localOrigin;
    private final float initialYaw;
    private final float totalMass;

    public static float getBlockMass(BlockState state) {
        if (state == null || state.isAir()) {
            return 0.0f;
        }
        Block b = state.getBlock();
        if (b instanceof ButtonBlock || b instanceof LeverBlock || b instanceof TorchBlock || b instanceof WallTorchBlock || b instanceof TripwireHookBlock || b instanceof TripwireBlock) {
            return 0.02f;
        }
        if (b instanceof PressurePlateBlock || b instanceof CarpetBlock) {
            return 0.05f;
        }
        if (b instanceof LadderBlock || b instanceof AbstractSignBlock || b instanceof BannerBlock || b instanceof WallBannerBlock || b instanceof RodBlock || b instanceof AbstractRailBlock) {
            return 0.08f;
        }
        if (b instanceof PaneBlock || b instanceof TrapdoorBlock || b instanceof LanternBlock) {
            return 0.12f;
        }
        if (b instanceof DoorBlock) {
            return 0.25f;
        }
        if (b instanceof TransparentBlock) {
            return 0.4f;
        }
        if (b instanceof SlabBlock) {
            return 0.5f;
        }
        if (b instanceof StairsBlock || b instanceof FenceBlock || b instanceof FenceGateBlock || b instanceof WallBlock) {
            return 0.7f;
        }
        if (b == Blocks.ANVIL || b == Blocks.CHIPPED_ANVIL || b == Blocks.DAMAGED_ANVIL || b == Blocks.HEAVY_CORE) {
            return 4.0f;
        }
        if (b == Blocks.NETHERITE_BLOCK || b == Blocks.OBSIDIAN || b == Blocks.CRYING_OBSIDIAN || b == Blocks.RESPAWN_ANCHOR) {
            return 3.5f;
        }
        if (b == Blocks.IRON_BLOCK || b == Blocks.GOLD_BLOCK || b == Blocks.COPPER_BLOCK || b == Blocks.WAXED_COPPER_BLOCK) {
            return 2.5f;
        }
        return 1.0f;
    }

    public VehicleStructure(List<StoredBlock> blocks, List<SeatData> seats, int width, int height, int length, Vec3d localOrigin, float initialYaw) {
        this(blocks, seats, List.of(), List.of(), width, height, length, localOrigin, initialYaw);
    }

    public VehicleStructure(List<StoredBlock> blocks, List<SeatData> seats, List<WheelData> wheels, int width, int height, int length, Vec3d localOrigin, float initialYaw) {
        this(blocks, seats, wheels, List.of(), width, height, length, localOrigin, initialYaw);
    }

    public VehicleStructure(List<StoredBlock> blocks, List<SeatData> seats, List<WheelData> wheels, List<StoredItemFrame> itemFrames, int width, int height, int length, Vec3d localOrigin, float initialYaw) {
        this.blocks = Collections.unmodifiableList(new ArrayList<StoredBlock>(blocks));
        Map<BlockPos, StoredBlock> byPosition = new HashMap<>();
        for (StoredBlock block : blocks) {
            byPosition.put(block.relativePos(), block);
        }
        ArrayList<StoredBlock> visible = new ArrayList<>();
        ArrayList<StoredBlock> collisionProbes = new ArrayList<>();
        for (StoredBlock block : blocks) {
            BlockPos pos = block.relativePos();
            boolean fullyEnclosed = block.state().isOpaqueFullCube();
            if (fullyEnclosed) {
                for (net.minecraft.util.math.Direction direction : net.minecraft.util.math.Direction.values()) {
                    StoredBlock neighbor = byPosition.get(pos.offset(direction));
                    if (neighbor == null || !neighbor.state().isOpaqueFullCube()) {
                        fullyEnclosed = false;
                        break;
                    }
                }
            }
            if (!fullyEnclosed) {
                visible.add(block);
            }
            if (!byPosition.containsKey(pos.east()) || !byPosition.containsKey(pos.west())
                || !byPosition.containsKey(pos.north()) || !byPosition.containsKey(pos.south())) {
                collisionProbes.add(block);
            }
        }
        this.renderableBlocks = Collections.unmodifiableList(visible);
        if (collisionProbes.size() > 256) {
            ArrayList<StoredBlock> sampled = new ArrayList<>(256);
            for (int i = 0; i < 256; ++i) {
                sampled.add(collisionProbes.get((int)((long)i * collisionProbes.size() / 256L)));
            }
            collisionProbes = sampled;
        }
        this.collisionProbeBlocks = Collections.unmodifiableList(collisionProbes);
        double minRy = Double.MAX_VALUE;
        for (StoredBlock storedBlock : blocks) {
            if (!(storedBlock.ry() < minRy)) continue;
            minRy = storedBlock.ry();
        }
        ArrayList<StoredBlock> contact = new ArrayList<StoredBlock>();
        for (StoredBlock b : blocks) {
            if (!(Math.abs(b.ry() - minRy) < 0.1)) continue;
            contact.add(b);
        }
        this.contactBlocks = Collections.unmodifiableList(contact);
        this.seats = Collections.unmodifiableList(new ArrayList<SeatData>(seats));
        this.wheels = Collections.unmodifiableList(new ArrayList<WheelData>(wheels != null ? wheels : List.of()));
        this.itemFrames = Collections.unmodifiableList(new ArrayList<StoredItemFrame>(itemFrames != null ? itemFrames : List.of()));
        this.width = width;
        this.height = height;
        this.length = length;
        this.localOrigin = localOrigin;
        this.initialYaw = initialYaw;
        float f = 0.0f;
        for (StoredBlock b : this.blocks) {
            f += VehicleStructure.getBlockMass(b.state());
        }
        this.totalMass = Math.max(f += (float)this.itemFrames.size() * 0.05f, 1.0f);
    }

    public List<StoredBlock> getBlocks() {
        return this.blocks;
    }

    public List<StoredBlock> getRenderableBlocks() {
        return this.renderableBlocks;
    }

    public List<StoredBlock> getContactBlocks() {
        return this.contactBlocks;
    }

    public List<StoredBlock> getCollisionProbeBlocks() {
        return this.collisionProbeBlocks;
    }

    public List<SeatData> getSeats() {
        return this.seats;
    }

    public List<WheelData> getWheels() {
        return this.wheels;
    }

    public List<StoredItemFrame> getItemFrames() {
        return this.itemFrames;
    }

    public boolean isWheel(double rx, double ry, double rz) {
        for (WheelData w : this.wheels) {
            if (!(Math.abs(w.rx() - rx) < 0.25) || !(Math.abs(w.ry() - ry) < 0.25) || !(Math.abs(w.rz() - rz) < 0.25)) continue;
            return true;
        }
        return false;
    }

    public boolean isSteeringWheel(double rx, double ry, double rz) {
        for (WheelData w : this.wheels) {
            if (!w.isSteering() || !(Math.abs(w.rx() - rx) < 0.25) || !(Math.abs(w.ry() - ry) < 0.25) || !(Math.abs(w.rz() - rz) < 0.25)) continue;
            return true;
        }
        return false;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public int getLength() {
        return this.length;
    }

    public Vec3d getLocalOrigin() {
        return this.localOrigin;
    }

    public float getInitialYaw() {
        return this.initialYaw;
    }

    public float getTotalMass() {
        return this.totalMass;
    }

    public List<SeatData> getDriverSeats() {
        return this.seats.stream().filter(s -> s.isDriver).toList();
    }

    public List<SeatData> getPassengerSeats() {
        return this.seats.stream().filter(s -> !s.isDriver).toList();
    }

    public NbtCompound toNbt() {
        NbtCompound tag = new NbtCompound();
        NbtList blockList = new NbtList();
        for (StoredBlock storedBlock : this.blocks) {
            blockList.add(storedBlock.toNbt());
        }
        tag.put("blocks", blockList);
        NbtList seatList = new NbtList();
        for (SeatData seatData : this.seats) {
            seatList.add(seatData.toNbt());
        }
        tag.put("seats", seatList);
        NbtList nbtList = new NbtList();
        for (WheelData w : this.wheels) {
            nbtList.add(w.toNbt());
        }
        tag.put("wheels", nbtList);
        NbtList nbtList2 = new NbtList();
        for (StoredItemFrame f : this.itemFrames) {
            nbtList2.add(f.toNbt());
        }
        tag.put("frames", nbtList2);
        tag.putInt("width", this.width);
        tag.putInt("height", this.height);
        tag.putInt("length", this.length);
        tag.putDouble("originX", this.localOrigin.x);
        tag.putDouble("originY", this.localOrigin.y);
        tag.putDouble("originZ", this.localOrigin.z);
        tag.putFloat("initialYaw", this.initialYaw);
        return tag;
    }

    public static VehicleStructure fromNbt(NbtCompound tag) {
        NbtList blockList = tag.getList("blocks", 10);
        ArrayList<StoredBlock> blocks = new ArrayList<StoredBlock>();
        for (NbtElement el : blockList) {
            blocks.add(StoredBlock.fromNbt((NbtCompound) el));
        }
        NbtList seatList = tag.getList("seats", 10);
        ArrayList<SeatData> seats = new ArrayList<SeatData>();
        for (NbtElement el : seatList) {
            seats.add(SeatData.fromNbt((NbtCompound) el));
        }
        ArrayList<WheelData> wheels = new ArrayList<WheelData>();
        if (tag.contains("wheels")) {
            NbtList wheelList = tag.getList("wheels", 10);
            for (NbtElement el : wheelList) {
                wheels.add(WheelData.fromNbt((NbtCompound) el));
            }
        }
        ArrayList<StoredItemFrame> itemFrames = new ArrayList<StoredItemFrame>();
        if (tag.contains("frames")) {
            NbtList frameList = tag.getList("frames", 10);
            for (NbtElement el : frameList) {
                itemFrames.add(StoredItemFrame.fromNbt((NbtCompound) el));
            }
        }
        int w = tag.getInt("width");
        int h = tag.getInt("height");
        int l = tag.getInt("length");
        Vec3d origin = new Vec3d(tag.getDouble("originX"), tag.getDouble("originY"), tag.getDouble("originZ"));
        float yaw = tag.getFloat("initialYaw");
        return new VehicleStructure(blocks, seats, wheels, itemFrames, w, h, l, origin, yaw);
    }

    public record StoredBlock(BlockState state, double rx, double ry, double rz, NbtCompound blockEntityNbt) {
        public StoredBlock(BlockState state, double rx, double ry, double rz) {
            this(state, rx, ry, rz, null);
        }

        public BlockPos relativePos() {
            return new BlockPos((int)Math.round(this.rx), (int)Math.round(this.ry), (int)Math.round(this.rz));
        }

        public NbtCompound toNbt() {
            NbtCompound tag = new NbtCompound();
            tag.putInt("rawId", Block.getRawIdFromState(this.state));
            tag.putString("id", Registries.BLOCK.getId(this.state.getBlock()).toString());
            NbtCompound props = new NbtCompound();
            this.state.getEntries().forEach((prop, val) -> props.putString(prop.getName(), StoredBlock.getPropName(prop, val)));
            tag.put("props", props);
            tag.putDouble("rx", this.rx);
            tag.putDouble("ry", this.ry);
            tag.putDouble("rz", this.rz);
            if (this.blockEntityNbt != null && !this.blockEntityNbt.isEmpty()) {
                tag.put("beNbt", this.blockEntityNbt);
            }
            return tag;
        }

        @SuppressWarnings("unchecked")
        private static <T extends Comparable<T>> String getPropName(Property<T> prop, Comparable<?> val) {
            return prop.name((T) val);
        }

        public static StoredBlock fromNbt(NbtCompound tag) {
            BlockState state = null;
            if (tag.contains("rawId")) {
                int rawId = tag.getInt("rawId");
                state = Block.getStateFromRawId((int)rawId);
            }
            if (state == null) {
                String id = tag.getString("id");
                Block block = (Block)Registries.BLOCK.get(Identifier.of(id));
                if (block == null) {
                    block = Blocks.AIR;
                }
                state = block.getDefaultState();
                if (tag.contains("props")) {
                    NbtCompound props = tag.getCompound("props");
                    for (String key : props.getKeys()) {
                        for (Property prop : state.getProperties()) {
                            String val;
                            Optional parsed;
                            if (!prop.getName().equals(key) || !(parsed = prop.parse(val = props.getString(key))).isPresent()) continue;
                            state = withProperty(state, prop, parsed.get());
                        }
                    }
                }
            }
            double rx = tag.contains("rx", 6) ? tag.getDouble("rx") : (double)tag.getInt("rx");
            double ry = tag.contains("ry", 6) ? tag.getDouble("ry") : (double)tag.getInt("ry");
            double rz = tag.contains("rz", 6) ? tag.getDouble("rz") : (double)tag.getInt("rz");
            NbtCompound beNbt = tag.contains("beNbt") ? tag.getCompound("beNbt") : null;
            return new StoredBlock(state, rx, ry, rz, beNbt);
        }
    }

    public record WheelData(double rx, double ry, double rz, boolean isSteering) {
        public BlockPos pos() {
            return new BlockPos((int)Math.round(this.rx), (int)Math.round(this.ry), (int)Math.round(this.rz));
        }

        public NbtCompound toNbt() {
            NbtCompound tag = new NbtCompound();
            tag.putDouble("rx", this.rx);
            tag.putDouble("ry", this.ry);
            tag.putDouble("rz", this.rz);
            tag.putBoolean("steering", this.isSteering);
            return tag;
        }

        public static WheelData fromNbt(NbtCompound tag) {
            double rx = tag.contains("rx", 6) ? tag.getDouble("rx") : (double)tag.getInt("rx");
            double ry = tag.contains("ry", 6) ? tag.getDouble("ry") : (double)tag.getInt("ry");
            double rz = tag.contains("rz", 6) ? tag.getDouble("rz") : (double)tag.getInt("rz");
            return new WheelData(rx, ry, rz, tag.getBoolean("steering"));
        }
    }

    public record StoredItemFrame(NbtCompound itemTag, String facingName, double rx, double ry, double rz, int rotation, boolean isGlow) {
        public NbtCompound toNbt() {
            NbtCompound tag = new NbtCompound();
            if (this.itemTag != null && !this.itemTag.isEmpty()) {
                tag.put("item", this.itemTag);
            }
            tag.putString("facing", this.facingName);
            tag.putDouble("rx", this.rx);
            tag.putDouble("ry", this.ry);
            tag.putDouble("rz", this.rz);
            tag.putInt("rotation", this.rotation);
            tag.putBoolean("glow", this.isGlow);
            return tag;
        }

        public static StoredItemFrame fromNbt(NbtCompound tag) {
            NbtCompound itemTag = tag.contains("item") ? tag.getCompound("item") : null;
            String facing = tag.getString("facing");
            return new StoredItemFrame(itemTag, facing, tag.getDouble("rx"), tag.getDouble("ry"), tag.getDouble("rz"), tag.getInt("rotation"), tag.getBoolean("glow"));
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T extends Comparable<T>, V extends T> BlockState withProperty(BlockState state, Property<T> prop, Object val) {
        return state.with(prop, (V) val);
    }
}
