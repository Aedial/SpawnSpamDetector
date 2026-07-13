# SSD - Spawn Spam Detector
A Minecraft 1.12 mod to detect concentrated spawning of mobs and warn players about it. Such spawns can be a sign of a mob farm, a buggy mod, or something else going rogue. Such spawns will cause heavy server lag, which might not be immediately obvious to players.

## Features
- Split the detection into regions (512x512 blocks) to be as lightweight as possible. The detection is intended to work continuously in the background without causing any noticeable lag. Due to how regions work, it is possible to have a cluster right at the intersection of 4 regions, which will take up to 4x the threshold to trigger a warning. This should be fairly rare.
- Mob counting and filters runs on the server across all currently loaded worlds and dimensions, while thresholds, cooldowns, and opt-out behavior remain local to each client.
- Configurable region/global detection threshold (per same type and total). Once the threshold is reached, the player will be prompted with a kill command for the specific mobs in that area.
- Configurable detection cooldown and per-player opt-out.

## Installation
- Install the mod on both the client and the server.
- Individual clients still control their own thresholds and cooldowns.

## Configuration
- Enable detection: `enableDetection` (default: `true`)
- Region detection threshold: `regionDetectionThreshold` (default: 1000)
- Region detection threshold per mob type: `regionDetectionThresholdPerType` (default: 100)
- Global detection threshold: `globalDetectionThreshold` (default: 1000)
- Global detection threshold per mob type: `globalDetectionThresholdPerType` (default: 100)
- Detection cooldown: `detectionCooldown` (default: 5 minutes)
- Whitelisted mobs: `whitelistedMobs`, the detection system will ONLY track these mobs. Use at your own risk. Example: `minecraft:bat`.
- Blacklisted mobs: `blacklistedMobs`, the detection system will ignore these mobs. Use at your own risk. Example: `minecraft:bat`.

## Building
Run:
```
./gradlew -q build
```
First build may take some time. Resulting jar will be under `build/libs/`.

## License
This project is licensed under the MIT License - see the LICENSE file for details.
