# Changelog

## 0.1.1 - 2026-11-06

All changes since `v0.1.0-alpha`.

### New

- **NeoForge.** Every version is now built for Fabric and NeoForge.
- **Minecraft 26.1, 26.1.1, 26.1.2, 26.2 and 26.3** (Java 25). Supported range: 1.20.4 - 26.3, 19 versions.
- **Zoom on every version.** Hold Z (change it in Controls, category *NiHaoClient*), the mouse wheel changes how far. It narrows the field of view of the world and of the hand and hides the HUD like F1. Before, it existed only on 1.21, 1.21.1 and 26.x.
- **Settings screen** (Options, *NiHaoClient...*): turn Zoom on or off and choose **Show skins**: everyone, others only, only me, or off.
- **New sign-in window** in the look of the NiHao launcher: own title bar, cards for Mojang / Microsoft, Offline and Ely.by, an Ely.by form (username or email, password, `password:code` for 2FA) and a "Remember my choice" checkbox. Esc or the cross closes it.
- **Languages: English, Ukrainian, Russian.** All screens and the sign-in window are translated. The game language is used; the sign-in window (it starts before the game) reads `lang` from `options.txt`, else the system language.
- **Account profile** in the main and pause menus: your head (with the Ely.by skin), nickname and account type.
- **Authentication type of each server** in the server list (`online-mode=false`, `Ely.by`, `Mojang`).
- **Screenshot gallery:** favourites folder, a native *Save as* dialog for Download, copy to the clipboard.
- **Self-update** from GitHub / GitLab releases, with a release-notes screen after the update.

### Fixed

- Closing the sign-in window without choosing used to start the game anyway. It now quits the game. (If the window cannot be shown at all, for example without a display, the game still starts with the launch account.)
- Zoom was missing on 12 versions (1.20.4, 1.20.6 and 1.21.2 - 1.21.11).
- The screenshot context menu and the settings hints are sized for longer translations instead of cutting the text.

### Under the hood

- One source tree converted per version by a small preprocessor (`//#if`, feature flags, literal renames). The old single `NiHaoClient` class is split into classes; loader code lives in `loaders/<loader>`.
- New version constants: `V1_21_2` (`GameRenderer.getFov` returns `float`) and `V1_21_9` (`KeyMapping.Category`).
- IDE build output (`versions/*/bin`) is no longer tracked; `bin/` and `dev-mods/` are ignored.
- README: supported versions, features table, session type, preprocessor, languages. `scripts/publish-release.ps1` added.
