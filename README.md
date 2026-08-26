# BlockVehicle Mod — Fabric 1.21.4

> **Turn any Minecraft block build into a drivable vehicle with arcade physics.**

## Quick Start

### 1. Craft the Vehicle Wand
| Item | Recipe |
|------|--------|
| **Vehicle Wand** | Gold + Redstone diagonal |
| *(Optional legacy)* **Vehicle Core** | Diamond surrounded by Gold + Redstone (3×3) |
| *(Optional legacy)* **Driver Seat** | Blue Wool top row + Iron + Iron Bars middle + Iron bottom |
| *(Optional legacy)* **Passenger Seat** | Gray Wool top row + Iron + Iron Bars middle + Iron bottom (yields 2) |

---

### 2. Build your vehicle with ANY blocks!
Build anything out of **any vanilla or custom blocks** (slabs, stairs, concrete, wool, glass, etc.).
- Vanilla **stairs** are automatically recognized as driver and passenger seats!
- Or designate **any block** (wool, slab, custom chair) as a seat using the wand.

---

### 3. Using the Vehicle Wand

**Sneak + Right-Click in air** to cycle Wand modes:

1. 📐 **Select Region**
   - **Right-click** block → sets **Corner 1**
   - **Sneak + Right-click** block → sets **Corner 2**
2. 💺 **Set Driver Seat** *(Optional)*
   - **Right-click any block** → sets it as the **Driver Seat** (automatically captures the block's or your facing direction).
3. 👥 **Add/Remove Passenger Seat** *(Optional)*
   - **Right-click any block** → toggles it as a **Passenger Seat**.
4. ⚡ **Activate Vehicle**
   - **Right-click** in air or on the build → transforms the build into an active vehicle!

---

### 4. Drive!
**Right-click** the vehicle to hop in. The first player takes the driver seat.

| Key | Action |
|-----|--------|
| **W** | Accelerate forward |
| **S** | Reverse |
| **A** | Steer left |
| **D** | Steer right |
| **Space** | Brake |
| **Shift** | Exit seat |
| **Mouse** | Free 360° view |

---

### 5. Dismantling
Hold the **Vehicle Wand**, sneak (**Shift**), and **Right-Click** the vehicle to turn it back into normal world blocks at its current position.

---

## Technical Features
- **Arbitrary Blocks**: Turn any build into a vehicle without requiring special fixed core or seat blocks.
- **Auto-Detection**: Automatically detects stairs and chairs as seats if not manually designated.
- **Single Entity**: Entire vehicle renders as one optimized entity.
- **Multiplayer & Replay**: Smooth 144+ FPS client-side prediction, high-precision synchronization, and 100% stutter-free Flashback / ReplayMod compatibility.

## Building
Requires **Java 21** and **Gradle 8+**.

```bash
./gradlew build
```

The built JAR will be in `build/libs/`.
