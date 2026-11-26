# AGENTS.md

Instructions for AI coding agents working in this repository. `CLAUDE.md` imports this file, so keep all agent guidance here.

## Project

`modlist` is a client-side [Fabric](https://fabricmc.net/) mod for Minecraft: Java Edition that lets players edit the mod-related communication between the client and a multiplayer server: what the client reports about its mods (client brand, registered channels), how it reacts to server content it does not have (registry entries), and how it reports mismatches.

[urntt/nojumpdelay](https://github.com/urntt/nojumpdelay) is the reference for the project layout, the configuration screens, the configuration file and the client game tests, and [urntt/hiteffect](https://github.com/urntt/hiteffect) and [urntt/serverlist](https://github.com/urntt/serverlist) for the configuration mode and the OneConfig integration. Follow their style, but do not add a dependency on nojumpdelay or hiteffect.

## Project decisions

These decisions are settled. Do not deviate from them without the user's explicit approval.

### Minecraft and toolchain

- Development started on Minecraft 26.3 with Java 25. `gradle.properties` is the single source of truth for the mod, Minecraft, Fabric Loader, Loom, Fabric API, Mod Menu, serverlist, OneConfig, and Java versions. `build.gradle`, `fabric.mod.json`, the mixin config, and the CI workflows read them from there; do not restate them elsewhere.
- Follow the latest official Fabric template ([FabricMC/fabric-example-mod](https://github.com/FabricMC/fabric-example-mod), also available from the [template generator](https://fabricmc.net/develop/template/)): the `net.fabricmc.fabric-loom` Gradle plugin, Mojang's official names with no `mappings` dependency, and `implementation` (not `modImplementation`) for dependencies. Do not use Yarn.
- Pin `loom_version` to a release version instead of the template's `-SNAPSHOT`, so builds are reproducible.
- Target only the latest stable (release) Minecraft version. Updates, fixes, and new features are always developed against it. Snapshots, pre-releases, and release candidates are not supported targets.
- Do not maintain older Minecraft versions and do not set up multi-version builds (no per-version branches, no Stonecutter or other preprocessors). When a new stable version is released, port the mod to it and drop the previous one.

### Identity

| Item | Value |
| --- | --- |
| Mod ID | `modlist` |
| Display name | `modlist` |
| Maven group | `com.urntt` |
| Base package | `com.urntt.modlist` |
| Version format | `<SemVer>+<Minecraft version>`, for example `1.0.0+26.3` |
| License | MIT |

The mod version itself follows [Semantic Versioning](https://semver.org/); the `+<Minecraft version>` suffix is build metadata naming the Minecraft version the build targets. Version numbers start at `1.0.0`.

### Distribution

- Releases are published only as GitHub Releases. Do not publish to Modrinth, CurseForge, or any other mod platform, and do not add publishing tooling for them.
- `README.md` must clearly describe the risks: changing what the client reports may break server rules and get the player kicked or banned; ignoring unknown registry entries can show wrong content, disconnect the player, or crash the client; hidden and faked mods break or confuse those mods' multiplayer features; and the mod cannot hide mods completely.

### Scope and behavior

- Client-only: `fabric.mod.json` declares `"environment": "client"`. There is no server-side component. The mod only changes what the client sends and how it handles what it receives.
- The mod never changes anything in singleplayer or on a world this client hosts (memory connections). Server list pings are not affected either; only game connections that go through the login phase are.
- Configuration mode, shown as the first button of the main settings screen and stored as `mode` in `modlist.json` (`custom` by default; unknown values become `custom`):
  - Disabled: the mod changes nothing. No connection gets a session, so nothing is rewritten or ignored, there are no join notices, and Fabric API's disconnect message gets no hint. The settings stay saved but are greyed out.
  - Custom: the mod's own settings apply.
  - OneConfig: the join notices and the default profile registered with OneConfig apply; the main screen shows them greyed out with the values OneConfig applies, and the default settings open read-only. Server profiles still come from the mod's own settings, because OneConfig cannot hold a list of profiles, and stay editable. Turning the mod off on its OneConfig card works like Disabled. This mode is only offered when OneConfig is installed. If the configuration file selects it but OneConfig is missing, the mod behaves as in Custom mode and keeps the saved mode until the player changes it.
- Settings are grouped in profiles. There is one default profile and any number of server profiles, each with an address. A connection uses the first server profile whose address matches (same rules as nojumpdelay: case-insensitive, an address without a port matches any port) and otherwise the default profile. A new server profile starts as a copy of the default profile that applies (OneConfig's in OneConfig mode); profiles do not inherit from each other afterwards. Do not reintroduce whitelist or blacklist modes.
- A profile's report mode, a single option on the profile screen, decides what the client reports:
  - `actual` ("Unchanged", the default): the real brand and channels.
  - `vanilla` ("Mimic Vanilla"): brand `vanilla`, every namespace hidden, empty channel registrations not sent, all login queries answered as not understood, and outgoing mod payloads dropped.
  - `server` ("Mimic Server"): real brand, every channel the server announced is faked, and local namespaces the server did not announce are hidden.
  - `custom`: the profile's own brand, hidden namespaces and faked channels. These settings are only shown in custom mode; switching modes never clears them, and editing them never changes the mode.
  The report mode never changes the registry options. Translation probing protection is shown for every mode except `actual`, where nothing is hidden.
- Mods are hidden by channel namespace, because that is what servers see. The `minecraft` namespace cannot be hidden. A fake channel entry is either a namespace (mirror every channel the server announces in it) or a full channel ID.
- Ignoring unknown registry entries is disabled by default. When enabled, unknown entries and registries are dropped from Fabric API's registry sync, the resulting raw ID holes are filled with the profile's placeholders (block `minecraft:air`, item `minecraft:air`, entity type `minecraft:marker` by default; other registries use their default or lowest-ID entry), unknown block state IDs resolve to the block placeholder's default state, and incompatible entity data values are skipped. When disabled, Fabric API's disconnect message gets a hint pointing to this option.
- Join notices (enabled by default) are chat messages after joining that list ignored registry entries and the namespaces only one side has. They are informational and never block joining.
- Translation probing protection (enabled by default, per profile) makes sign editing and anvil renaming treat translation keys and key binding names of the session's hidden namespaces as unknown.
- A key binding opens the settings screen. It is unbound by default.
- With serverlist installed, its Add Server and Edit Server Info screens get a "modlist Settings..." button that opens the server profile for the saved address, creating it from the default profile if none matches. It is greyed out while the server has no saved address and while the mod is off.

### Localization

- All user-facing text, including key binding names, the key binding category, chat messages, the configuration screens, and the OneConfig option titles, uses translation keys. Never hard-code display strings. Keys shared by the screens and the integrations are constants in `SettingKeys`; OneConfig translates a title, description or option that is a translation key, so its annotations take the keys directly (its deprecated `*Key` attributes are not used).
- Provide translations for `en_us` and `zh_cn`, and keep both complete whenever a translation key is added or changed.

### Dependencies

- Required: Fabric Loader and Fabric API.
- Optional: Mod Menu, declared under `suggests` in `fabric.mod.json`. The mod must load and work normally without it, so Mod Menu classes may only be referenced from the Mod Menu entrypoint.
- Optional: [OneConfig](https://modrinth.com/mod/oneconfig), approved by the user and declared under `suggests`. The build only compiles against its API (`clientCompileOnly`); it is never bundled. OneConfig classes may only be referenced from the `com.urntt.modlist.compat.oneconfig` package, which OneConfig reaches through its `oneconfig:config` entrypoint; the rest of the mod talks to it through `compat.OneConfigBridge`. OneConfig stores its settings in the same config directory as `modlist.json` and watches its files, so its config file must keep a different name (`modlist-oneconfig.json`). Its getters apply the same rules as `Profile` (`ProfileValues`), because OneConfig writes the fields directly.
- Optional: [serverlist](https://github.com/urntt/serverlist), approved by the user and declared under `suggests`. It is only published as GitHub Releases, so `build.gradle` reads its jar from there through an Ivy repository limited to `com.urntt:serverlist`. The build compiles against its API (`clientCompileOnly`) and loads it in the development client and the tests (`localRuntime`); it is never bundled. serverlist classes may only be referenced from the `com.urntt.modlist.compat.serverlist` package, which serverlist reaches through its `serverlist` entrypoint.
- The mod's own configuration is hand-written without a config library: a JSON file in the Fabric config directory, serialized with Gson (bundled with Minecraft). The configuration screens use vanilla widgets.
- Some mixins and classes depend on Fabric API internals (`ClientRegistrySyncHandler`, `RegistrySyncPayload`, `RegistrationPayload`, `CommonRegisterPayload`). Keep that dependency confined to the classes that need it, and expect to revisit them when Fabric API is updated; the client game tests catch breakage.
- Do not add other dependencies without the user's explicit approval.

### Implementation

- Language: Java only.
- Source sets: `src/main` holds only `fabric.mod.json` and the icon. All code and client resources live in `src/client`, and the client game tests live in `src/gametest`.
- The icon's source is `art/icon.svg`: urntt's hair clip above a list card. `src/main/resources/assets/modlist/icon.png` is rendered from it at 512 x 512 pixels with a transparent background (`rsvg-convert -w 512 -h 512 art/icon.svg -o src/main/resources/assets/modlist/icon.png`); render it again whenever the SVG changes.
- Mixins: prefer the MixinExtras injectors bundled with Fabric Loader (for example `@ModifyExpressionValue`, `@WrapOperation` and `@WrapMethod`) over `@Redirect` and `@Overwrite`, to stay compatible with other mods and keep porting work small. Name handler methods `modlist$<what>`.
- Keep the decisions about what to report (`ReportPlan`, `ChannelRewriter`) and what to drop from registry sync (`RegistrySyncFilter`) in plain classes without Minecraft state, so the logic game test can check them directly.
- Code that acts on a connection reads its profile through `ProfileSettings` (implemented by `Profile` and the OneConfig settings) and asks `ModListConfig` for the settings that apply (`effective()`, `effectiveProfileFor(...)`, `effectiveJoinNotices()`), never for the stored ones directly.
- Screens: vanilla runs `init()` only the first time a screen instance is shown and merely repositions it afterwards, and `rebuildWidgets()` runs `init()` again into the same `final` layout, which stacks a second copy of every widget. So refresh lists in place, and in `added()` when a screen is shown again; never call `rebuildWidgets()`. Option screens extend `ModListOptionsScreen`: use `section(...)` for titles (vanilla's `addHeader` titles take focus and show a border when clicked), `refreshOptions()` to rebuild options that depend on another option, and `refreshOptionsLater()` when that is triggered from a click inside the list.

### Testing

- The client game tests in `src/gametest` start Minecraft and check the configuration and its modes, the profile selection, the report modes, the channel rewriting, the registry filtering and placeholders, translation probing protection, the behavior of the configuration screens, and the serverlist button, including on a local dedicated server that runs in the same JVM. Keep them passing and extend them when behavior changes.
- They run twice: without OneConfig, and with `-PoneConfigRuntime=true`, which loads OneConfig and its dependencies (Fabric Language Kotlin, PolyCompose) from Modrinth's Maven into the development client. Their Modrinth version ids live in `gradle.properties` next to `oneconfig_version`; update them together. The tests set OneConfig's fields by reflection, so they compile without it.
- After porting to a new Minecraft or Fabric API version, run the client game tests. A successful build does not prove that the mixins still have the intended effect.
- `README.md` describes how to run them, including on a headless machine.

### CI, releases, and changelog

- GitHub Actions (`.github/workflows/build.yml`) builds the project and runs the client game tests, without and with OneConfig, on every push and pull request.
- Maintain `CHANGELOG.md` following [Keep a Changelog](https://keepachangelog.com/). Record every user-visible change under `Unreleased` in the same change that introduces it.
- `.github/workflows/release.yml` builds the mod and publishes a GitHub Release for the project version, with the jar attached and the matching `CHANGELOG.md` section as release notes. It runs when a tag `v<version>` (for example `v1.0.0+26.3`) is pushed, or when started manually on a branch, in which case it creates that tag on the branch's latest commit. It fails if a pushed tag does not match the project version, if the changelog has no section for the version, or if the release already exists.
- Release only when the user asks. To release, set `mod_version` in `gradle.properties`, rename `Unreleased` in `CHANGELOG.md` to `[<version>] - <YYYY-MM-DD>` above a new empty `Unreleased` section, commit, and push. Then start the release workflow on `main`. Claude Code cloud sessions cannot push tags, so start the workflow through the GitHub Actions API instead.

## Engineering principles

- Fix root causes, not symptoms. Diagnose the underlying cause before implementing a permanent fix. If an immediate mitigation is necessary, treat it as temporary and follow through with a root-cause fix.
- Prefer configuration-driven design for values that are expected to vary by environment, deployment, or product requirements. Avoid unexplained or duplicated magic values, but do not introduce configuration where a well-named constant is the clearer source of truth.
- Preserve a single source of truth and clear ownership for data, state, configuration, business logic, and authoritative documentation. Avoid duplicating canonical information across multiple locations.
- Do not maintain parallel legacy and replacement implementations without an explicit migration and removal plan.

## Documentation

- Keep documentation aligned with the code. When a code change affects documented behavior, APIs, architecture, configuration, workflows, or usage, update the relevant documentation in the same change.
- Keep each document's responsibility clear. For example, use `README.md` for project overview and usage, and `VISION.md` for product direction, architectural principles, or long-term decisions.
- Always specify a language identifier for fenced code blocks in Markdown.

## Language

- Communicate with the user in Chinese, including explanations, progress updates, and user-facing planning.
- Use English for development artifacts, including source code, comments, docstrings, documentation, READMEs, Git branch names, commit messages, and other deliverables intended to live in the repository.

## Git

- Do not change or override the Git author or committer identity. When an identity must be configured for commits created during the task, use:
  - Name: `urntt`
  - Email: `urntts@gmail.com`
- Do all actions on the user's behalf. Do not rewrite existing commit authorship unless explicitly requested. Do not add `Co-Authored-By` trailers or session links to commit messages or pull request descriptions.
- Develop on `main` and push directly to it. Branches and pull requests are not required.
- Because changes land on `main` without review, make sure `./gradlew build` and the client game tests pass locally before pushing.
- If a branch is used, give it a category-based prefix that reflects the purpose of the change, such as `feat/`, `fix/`, `refactor/`, `docs/`, `test/`, or `chore/`.
- Follow the [Conventional Commits](https://www.conventionalcommits.org/) specification for commit messages.
