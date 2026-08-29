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

For a helicopter:

1. Select the complete build and mark a real **driver seat**.
2. In **Helicopter / Nose**, right-click the front/nose block. This enables Helicopter mode and defines forward. Sneak-right-click switches back to Ground mode.
3. In **Propeller**, right-click the **top or bottom face** of the main rotor hub, then sneak-right-click each blade block. This vertical axis is required and the hub should be near the build's horizontal center.
4. Optionally add a tail rotor: right-click a horizontal face of its hub and mark its blades. A tail rotor gives noticeably stronger yaw control; without one, main-rotor torque must be corrected manually.
5. Mark bottom wheels/skids as landing contacts if desired, then activate. Any number of blade blocks may form one synchronized rotor assembly around its hub.

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
| Left / right mouse button | Rudder left / right while piloting |
| Up / Down | Direct pitch up / down |
| Mouse look | Gentle, rate-limited pitch and heading guidance; looking up rotates for takeoff once runway speed is high enough |
| Left Alt | Stunt mode while held: faster aerobatic control, stronger flight-path following, and reduced auto-leveling |
| Space | Airbrake; wheel brake on the ground |
| Shift | Dismount |

Helicopter:

| Key | Action |
| --- | --- |
| W / S | Cyclic forward / backward pitch |
| A / D | Cyclic right / left roll |
| Space | Collective up; hold while tilted to maintain altitude |
| Left Ctrl | Collective down |
| Left / right mouse button | Yaw left / right |
| Left Alt | Precision mode: softer cyclic/yaw response |
| Shift | Dismount |

Rotors spool up after mounting. Neutral collective approximately hovers only when level; banking or pitching redirects rotor lift horizontally and causes a gradual descent unless Space is held. Forward speed improves rotor efficiency, low-altitude ground effect adds a small lift cushion, and a steep low-speed powered descent can enter vortex-ring state. Recover by lowering collective and moving forward or sideways. An unpiloted descending helicopter gets limited autorotation rather than hovering.

Mouse rudder is active only while controlling a plane, so normal mouse actions and unrelated keybindings work normally after dismounting. Attacking, mining, and using items are suppressed while controlling any BlockVehicle so the control clicks never swing the rider's arm or affect the world. The pitch and stunt actions remain configurable in Minecraft's Controls screen.

Planes preserve momentum: climbing trades speed for height, diving gains speed, banking turns the lift vector, sharp maneuvers add drag, and low-speed/high-angle flight stalls gradually. With no pilot, an airborne plane continues falling/gliding and eases its throttle toward idle.

Engine audio adapts to the build: two or three marked wheels use a fast bike engine, four wheels use the car profile, and more than four use the low heavy/monster-truck profile. Planes use a dedicated propeller drone and helicopters use a separate rotor-blade loop, alongside quiet speed-based wind. The local driver mix stays clearly audible without distance attenuation, while nearby observers hear normal 3D falloff. Ground braking and drifting use continuous tire layers that fade smoothly instead of repeatedly triggering short sounds; doors and engine transitions remain restrained one-shots.

## Presets and removal

Save the selected blocks before activation:

```text
/vehiclepreset save <custom name>
/vehiclepreset spawn <custom name>
/vehiclepreset list
/vehiclepreset delete <custom name>
```

Names can contain spaces. Presets are stored with the world and include aircraft mode, markers, seats, landing gear, rotor/propeller groups, and cached tuning. Old presets without a mode remain Ground vehicles.

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

- Ground, plane, and helicopter force models remain separate.
- Aircraft orientation uses quaternions, including rendering, collision probes, camera cues, and passenger positions.
- The server simulates authoritative flight from sequence-numbered input packets; the local pilot predicts and softly reconciles while observers interpolate snapshots.
- Geometry, mass, wing measurements, contact samples, collision extremities, and propeller render groups are cached rather than scanned every tick.
- Local plane reconciliation ignores harmless sub-block prediction noise and applies bounded corrections, preventing delayed chunk-render frames from producing visible backward tugs.
- Distant vehicles retain their cached body mesh while tiny signs, item frames, and decorative dynamic blocks use distance-based LOD.
- Swept/substep collision protects the nose, wings, tail, top, and bottom and safely holds planes at unloaded chunks/world bounds.
- Engine/wind loops and render caches are bounded per vehicle and released when entities unload.

## Building

```bash
./gradlew build
```

The remapped mod jar is written to `build/libs/`.
