# BlockVehicle — Fabric 1.21.4

Turn block builds into drivable ground vehicles or arcade-style aircraft. Requires Fabric Loader, Fabric API, and Java 21.

## Vehicle Wand workflow

Craft a Vehicle Wand, then use **Sneak + Right-click in air** to cycle its modes. The HUD shows the active mode and current markers.

For a ground vehicle:

1. In **Select Region**, right-click Corner 1 and sneak-right-click Corner 2.
2. Optionally mark a driver seat, passenger seats, and wheels.
3. In **Activate**, right-click and use the clickable confirmation message (or type `confirm`).

Vanilla stairs can be detected as seats. A manually marked driver seat takes priority.

For a plane:

1. Select the complete build and mark a real **driver seat**.
2. In **Plane / Nose**, right-click the nose block. If the nose is not marked, the driver-seat facing is used instead. Sneak-right-click in this mode switches back to Ground mode.
3. In **Wing Tips**, right-click the left tip and sneak-right-click the right tip. Clearly swapped markers are corrected automatically.
4. In **Propeller**, right-click the face of each hub that defines its rotation axis, then sneak-right-click every blade block. Sneak-right-click an already marked hub to toggle clockwise/counter-clockwise spin. Blades are assigned to their nearest hub. Leave both lists empty to build a glider.
5. Mark wheels as landing gear, then activate. The summary reports mass, seats, gear, wingspan, propellers, estimated takeoff speed, and setup warnings.

Propeller blades render as synchronized rigid block assemblies around their hubs. They are cosmetic for collision in this version; the aircraft body uses cached swept collision probes.

## Controls

Ground vehicle:

| Key | Action |
| --- | --- |
| W / S | Accelerate / reverse |
| A / D | Steer |
| Space | Brake; brake while turning to drift |
| H | Horn |
| Shift | Dismount |

Plane:

| Key | Action |
| --- | --- |
| W / S | Increase / decrease throttle |
| A / D | Roll; steer while taxiing |
| Q / E | Rudder left / right |
| Up / Down | Direct pitch up / down |
| Mouse | Gentle, rate-limited pitch and heading guidance |
| Left Alt | Stunt mode while held (greatly reduces flight assistance) |
| Space | Airbrake; wheel brake on the ground |
| Shift | Dismount |

Q and E do not drop an item or open inventory while piloting. All dedicated plane actions are configurable in Minecraft's Controls screen.

Planes preserve momentum: climbing trades speed for height, diving gains speed, banking turns the lift vector, sharp maneuvers add drag, and low-speed/high-angle flight stalls gradually. With no pilot, an airborne plane continues falling/gliding and eases its throttle toward idle.

## Presets and removal

Save the selected blocks before activation:

```text
/vehiclepreset save <custom name>
/vehiclepreset spawn <custom name>
/vehiclepreset list
/vehiclepreset delete <custom name>
```

Names can contain spaces. Presets are stored with the world and include plane mode, markers, seats, landing gear, propeller groups, and cached tuning. Old presets without a mode remain Ground vehicles.

`/removevehicle` restores a nearby vehicle to blocks. A plane must be landed, slow, and nearly level, and its cardinal-aligned restoration space must be clear. Operators can use `/removevehicle force` for the same collision-safe restoration check without the landed-state requirement.

## Plane configuration

The first launch creates `config/blockvehicle.json`:

| Setting | Default | Purpose |
| --- | ---: | --- |
| `maxVehicleBlocks` | 8192 | Hard structure-size limit |
| `maxVehicleAxis` | 96 | Prevents huge tracking/collision bounds |
| `maxPropellers` | 8 | Maximum propeller assemblies |
| `maxPropellerBladeBlocks` | 512 | Total blade-block limit |
| `planeThrustMultiplier` | 1.0 | Global engine thrust tuning |
| `planeLiftMultiplier` | 1.0 | Global lift tuning |
| `planeDragMultiplier` | 1.0 | Global aerodynamic drag tuning |
| `planeControlAssist` | 1.0 | Mouse/auto-level assistance |
| `cameraRollStrength` | 0.35 | How much aircraft roll reaches the camera |
| `cameraHorizonStabilization` | 0.65 | How strongly the camera favors the horizon |
| `cameraSizeDistanceMultiplier` | 0.90 | Automatic chase distance from plane size |
| `cameraMaxDistance` | 48.0 | Maximum automatic third-person distance |
| `cameraMaxFovBoost` | 12.0 | Maximum size-based third-person FOV increase |
| `entityCollisionDamage` | false | Enables collision damage |
| `propellerCollision` | false | Reserved for swept-disc propeller collision |

Values are clamped to server-safe ranges when loaded.

## Technical notes

- Ground and plane physics remain separate.
- Plane orientation uses quaternions, including rendering, collision probes, and passenger positions.
- The server simulates authoritative flight from sequence-numbered input packets; the local pilot predicts and softly reconciles while observers interpolate snapshots.
- Geometry, mass, wing measurements, contact samples, collision extremities, and propeller render groups are cached rather than scanned every tick.
- Swept/substep collision protects the nose, wings, tail, top, and bottom and safely holds planes at unloaded chunks/world bounds.
- Engine/wind loops and render caches are bounded per vehicle and released when entities unload.

## Building

```bash
./gradlew build
```

The remapped mod jar is written to `build/libs/`.
