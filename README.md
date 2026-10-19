# NiHao Minecraft Client

Client-side multi-loader, multi-version mod for Minecraft Java (1.20.4 – 1.21.11). One source tree is built for every supported Minecraft version and mod loader. Fabric is supported today; NeoForge is planned (see the table below).

## Features

- **Ely.by skins and capes.** Player skins are fetched from `skinsystem.ely.by` and replace the vanilla ones (only you see them; there is no server side). Falls back to the vanilla skin if Ely.by has none.
- **Screenshot gallery.** An icon button next to the main-menu and pause-menu buttons opens a thumbnail grid. Click a thumbnail for the full-size view with mouse-wheel zoom and drag to pan. Right click gives *Open / Download / Copy to clipboard / Delete*.

## Supported versions

| Minecraft version | Fabric support | NeoForge support |
|-------------------|----------------|------------------|
| 1.20.4 | ✅ | ❌ (not yet) |
| 1.20.6 | ✅ | ❌ (not yet) |
| 1.21 | ✅ | ❌ (not yet) |
| 1.21.1 | ✅ | ❌ (not yet) |
| 1.21.2 | ✅ | ❌ (not yet) |
| 1.21.3 | ✅ | ❌ (not yet) |
| 1.21.4 | ✅ | ❌ (not yet) |
| 1.21.5 | ✅ | ❌ (not yet) |
| 1.21.6 | ✅ | ❌ (not yet) |
| 1.21.7 | ✅ | ❌ (not yet) |
| 1.21.8 | ✅ | ❌ (not yet) |
| 1.21.9 | ✅ | ❌ (not yet) |
| 1.21.10 | ✅ | ❌ (not yet) |
| 1.21.11 | ✅ | ❌ (not yet) |

Loader requirements — Fabric: Fabric Loader ≥ 0.15 and Fabric API. Java: 17 for 1.20.4, Java 21 for everything newer.

## Build

```
./gradlew.bat        # Windows; ./gradlew on Linux/macOS
```

Builds every version and loader and copies the jars to `release/nihao-<loader>-<mod>+<mc>.jar`, e.g. `nihao-fabric-0.1.0+1.21.1.jar`. The folder is git-ignored; publish the jars as GitHub Release assets. Build one target: `./gradlew :<loader>-<mc>:build` (e.g. `:fabric-1.21.4:build`). Run it in a dev client: `./gradlew :fabric-1.21.4:runClient`.

## How multi-version / multi-loader works

Each target is a Gradle subproject named `<loader>-<mc>`; the loader-independent code is shared.

- `src/main/` – code shared by all versions.
- `shims/<group>/java/` – the few classes whose Minecraft API differs (`Compat`, `MouseScreen`). Each version picks one group.
- `versions/<mc>/gradle.properties` – per version: `mc`, `fabric_api`, `java_version`, `shim`, and optional `remap=from>to;...` literal renames (e.g. `ResourceLocation>Identifier` in 1.21.11).
- `build.gradle` merges `src/main` + the chosen shim into `versions/<mc>/build/src`, applies the renames, and compiles it.

Add a version: create `versions/<mc>/gradle.properties`, pick the closest `shim`, build, and add a shim group only if it fails to compile. New loaders (NeoForge, later possibly Forge) plug in as another `<loader>-<mc>` subproject type; only Fabric is set up so far.

## Notes

- Mappings are the official Mojang ones.
- Fabric: launchers must supply the Fabric intermediary mappings (`net.fabricmc:intermediary:<mc>`); the official Fabric profile does this.

## Disclaimer

NiHao Minecraft Client is an unofficial, independent project. It is not affiliated with, endorsed by, or connected to Mojang Studios, Microsoft, or Minecraft. Minecraft is a trademark of Mojang Studios / Microsoft; all rights to it belong to their owners. Ely.by is a separate third-party service and is likewise not affiliated with this project.
