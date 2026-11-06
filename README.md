# NiHao Minecraft Client

Client-side multi-loader, multi-version mod for Minecraft Java (1.20.4 – 26.3). One source tree is built for every supported Minecraft version and mod loader. Fabric and NeoForge are supported (see the table below).

## Features

- **Ely.by skins and capes.** Player skins are fetched from `skinsystem.ely.by` and replace the vanilla ones (only you see them; there is no server side). Falls back to the vanilla skin if Ely.by has none. *Options → NiHaoClient → Show skins* limits it to everyone, others only, only you, or off.
- **Screenshot gallery.** An icon button next to the main-menu and pause-menu buttons opens a thumbnail grid. Click a thumbnail for the full-size view with mouse-wheel zoom and drag to pan. Right click gives *Open / Download / Copy to clipboard / Delete*.
- **Account profile in menus.** The top-left corner of the main/pause menu shows your head (with the Ely.by skin), nickname and account type (`offline`, `elyby` or `microsoft`).
- **Server authentication in the server list.** Next to each server name: `[online-mode=false]`, `[Ely.by]` or `[Mojang/Ely.by]`. It is detected by starting a login (Encryption Request = online-mode). The protocol cannot tell Mojang from Ely.by, so list Ely.by server hosts (one per line) in `config/nihao-elyby-servers.txt`.
- **Languages.** English, Ukrainian and Russian (the game language; the sign-in window uses the `lang` of `options.txt`, else the system language).
- **Zoom.** Hold the Zoom key (Z by default, set it in Controls, category *NiHaoClient*): the field of view of the world and of the hand narrows smoothly, the HUD (hotbar, health, crosshair) is hidden like with F1, and the mouse wheel changes how far. Can be switched off in *Options > NiHaoClient...*. Not on every version: see [Features](#features).

## Session type

Before Minecraft starts, the mod picks the account type (`offline`, `elyby` or `microsoft`):

1. The launcher flag `-Dnihao.session=offline|elyby|microsoft` wins (for `elyby` the token is read from the `NIHAO_TOKEN` environment variable).
2. Otherwise the choice saved in `session.nihao` in the game (instance) folder is used (an Ely.by token is re-validated or refreshed).
3. Otherwise a window offers: **Mojang / Microsoft** (only if the game was started from a licensed account), **Offline** (nickname from the launch arguments) and **Ely.by** (username/email + password; for 2FA enter `password:code`). "Remember my choice" saves it; the state of that checkbox is itself remembered (`nihao/login.properties`).

The saved file contains an Ely.by access token; do not share it. The window runs in its own small Java process (the game's JVM is headless at that point); if it cannot be shown (no display), the game keeps the account it was started with, which the profile label then shows (`microsoft` for a licensed launch). Closing the window without a choice quits the game. Joining Ely.by-only servers additionally needs authlib-injector, which the mod does not provide.

## Self-update

On a clean start (no launcher flags, i.e. without `-Dnihao.session`) the mod checks the latest release on **GitHub** (`NiHaoLauncher/nihao-minecraft-client`) and on its **GitLab** mirror (`nihaolauncher/nihao-minecraft-client`) and takes the newer one; if one host is down, the other is used. When a newer release has a jar for your Minecraft version, the main menu shows *"Доступна нова версія NiHaoClient X. Оновити?"*. Clicking it downloads the jar; it replaces the old one after the game exits. On the next start the release notes (Markdown, PNG images included) are shown once. Tags must look like `v0.1.1`; release assets are named `nihao-<loader>-<ver>+<mc>.jar` (`fabric` or `neoforge`).

## Supported versions

| Minecraft version | Fabric support | NeoForge support |
|-------------------|----------------|------------------|
| 1.20.4 | ✅ | ✅ |
| 1.20.6 | ✅ | ✅ |
| 1.21 | ✅ | ✅ |
| 1.21.1 | ✅ | ✅ |
| 1.21.2 | ✅ | ✅ |
| 1.21.3 | ✅ | ✅ |
| 1.21.4 | ✅ | ✅ |
| 1.21.5 | ✅ | ✅ |
| 1.21.6 | ✅ | ✅ |
| 1.21.7 | ✅ | ✅ |
| 1.21.8 | ✅ | ✅ |
| 1.21.9 | ✅ | ✅ |
| 1.21.10 | ✅ | ✅ |
| 1.21.11 | ✅ | ✅ |
| 26.1 | ✅ | ✅ |
| 26.1.1 | ✅ | ✅ |
| 26.1.2 | ✅ | ✅ |
| 26.2 | ✅ | ✅ |
| 26.3 | ✅ | ✅ |

### Features

"all" = every version and both loaders.

| Feature | Where it works |
|---|---|
| Ely.by skins and capes | all |
| Screenshot gallery: thumbnails, viewer with zoom and pan, context menu, favourites | all |
| Gallery "Download" with a Save-as dialog | all |
| Account profile in the menus | all |
| Authentication type in the server list | all |
| Sign-in window (offline / Ely.by / Microsoft) and saved session | all |
| Self-update and release notes | all |
| NiHaoClient settings (Options screen) | all |
| Zoom (hold a key, mouse wheel changes it) | all |

Loader requirements — Fabric: Fabric Loader ≥ 0.15 and Fabric API. NeoForge: the matching NeoForge version for your Minecraft version (no extra mods). Java: 17 for 1.20.4, Java 21 for 1.20.6 – 1.21.11, Java 25 for 26.x. Minecraft 26.x is unobfuscated (no mappings, no remapping); its Fabric build uses the `net.fabricmc.fabric-loom` plugin and needs a JDK 25 to compile (downloaded automatically).

## Build

```
./gradlew.bat        # Windows; ./gradlew on Linux/macOS
```

Builds every version and loader and copies the jars to `release/nihao-<loader>-<mod>+<mc>.jar`, e.g. `nihao-fabric-0.1.0+1.21.1.jar`. The folder is git-ignored; publish the jars as release assets with `scripts/publish-release.ps1 <tag>` (needs a GitLab token with `api` scope in `GITLAB_TOKEN`). Build one target: `./gradlew :<loader>-<mc>:build` (e.g. `:fabric-1.21.4:build`). Run it in a dev client: `./gradlew :fabric-1.21.4:runClient`.

## How multi-version / multi-loader works

Each target is a Gradle subproject named `<loader>-<mc>`; the loader-independent code is shared.

- `src/main/` – code shared by all versions.
- `shims/<group>/java/` – the few classes whose Minecraft API differs (`Compat`, `MouseScreen`, some mixins). Each version picks one group. Prefer a `//#if` block in the shared file over a new shim copy (see below).
- `loaders/<loader>/` – loader-specific code (`Platform`, the NeoForge entry class) and resources (`fabric.mod.json`, `neoforge.mods.toml`).
- `versions/<mc>/gradle.properties` – per version: `mc`, `fabric_api`, `neoforge`, `java_version`, `shim`, optional `features=zoom` (feature flags) and optional `remap=from>to;...` literal renames (e.g. `ResourceLocation>Identifier` in 1.21.11, `GuiGraphics>GuiGraphicsExtractor` in 26.x; no `;` inside a rename).
- `build.gradle` merges `src/main` + the chosen shim + the loader folder into `build/src`, runs the preprocessor over it, and compiles that.

**Preprocessor.** One source, converted per version. In any Java file under `src/`, `shims/` or `loaders/`:

```java
//#if ZOOM && MC >= V26_2
    ...code for 26.x...
//#elif ZOOM
    ...code for the older versions that have zoom...
//#else
    ...everything else...
//#endif
```

`MC` is the version as a number (1.20.4 = 12004, 1.21 = 12100, 1.21.11 = 12111, 26.3 = 260300); `V26_1`, `V26_2` and `V26_3` are the numbers of the 26.x versions where the API changed (26.1: `GuiGraphicsExtractor` renames; 26.2: `Hud` split out of `Gui`, `setScreen` moved to `Gui`; 26.3: SDL instead of GLFW, no tinyfd, mouse buttons numbered 1/2/3); a feature flag from `features=` is a boolean named in upper case (`ZOOM`). The expression is Groovy; directive lines and inactive branches are dropped before compiling. Blocks must be balanced within one file, and the directive must be alone on its line. Then the version's `remap` renames are applied to what is left. Differences that are pure renames go into `remap`, differences in logic into `//#if`, and a separate shim file is only for a class whose whole implementation differs.

Add a version: create `versions/<mc>/gradle.properties`, pick the closest `shim`, build, and fix what fails with `remap` or `//#if` (a new shim group only as a last resort). Check that you did not change other versions: `./gradlew prepareSources` and diff each `versions/*/build/src` against a copy taken before. A new loader plugs in as another `<loader>-<mc>` subproject type (a `loaders/<loader>` folder with `Platform` and its metadata). The mod hooks Minecraft with mixins only, so no loader API is needed besides `Platform`.

## Notes

- Mappings are the official Mojang ones.
- Both loaders use the official Mojang names at runtime. NeoForge builds use ModDevGradle and need a JDK 17 for 1.20.4 tooling (downloaded automatically).
- Fabric: launchers must supply the Fabric intermediary mappings (`net.fabricmc:intermediary:<mc>`); the official Fabric profile does this.

## Disclaimer

NiHao Minecraft Client is an unofficial, independent project. It is not affiliated with, endorsed by, or connected to Mojang Studios, Microsoft, or Minecraft. Minecraft is a trademark of Mojang Studios / Microsoft; all rights to it belong to their owners. Ely.by is a separate third-party service and is likewise not affiliated with this project.
