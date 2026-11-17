# modlist

A client-side Fabric mod for Minecraft: Java Edition that lets you edit the mod-related communication between your client and a multiplayer server:

- Join servers whose mods do not match yours, and see in chat what differs.
- Join servers that send blocks, items or other registry entries your client does not have, shown as placeholders you choose. This is off by default.
- Choose what the server learns about your mods: the client brand, which mods are hidden, and which mods are faked. One click mimics a vanilla client, mimics the server's mod set, or restores your real mods.
- Use different settings for each server.

## Risks

**Read this before using the mod in multiplayer.**

- **Server rules and anti-cheat.** Changing your client brand or the mods you report is a form of disguise. Servers may forbid it, and anti-cheat or mod detection systems may notice it. You may be kicked or banned. Check each server's rules.
- **Ignoring unknown registry entries.** The server's content that your client lacks is replaced with placeholders. That content cannot work properly:
  - Placeholder blocks do not behave like the real blocks; walking into or through them can get your movement set back.
  - When the server sends data your client cannot decode, for example an item carrying an unknown component, you are disconnected with a network protocol error.
  - If your client also has content mods and the server's extra content is mixed in between, blocks of other mods may look wrong.
  - In the worst case the game crashes.
- **Faked mods.** The server may send data to a faked mod or wait for an answer it never gets. You may then get stuck while joining and time out, or be kicked.
- **Hidden mods.** A hidden mod cannot talk to the server, so its multiplayer features stop working. Hiding Fabric API's `fabric` namespace gets you disconnected by servers whose mods add content.
- **Mimic Vanilla** disables the multiplayer features of all your mods, and Fabric servers whose mods add required content still refuse vanilla clients. **Mimic Server** fakes every mod the server announces, with the risks of faked mods.
- **No complete disguise.** Servers can still notice mods in other ways, for example through their behavior or through resource pack checks.
- **Fabric API internals.** Parts of the mod rely on Fabric API's internal classes and may stop working after a Fabric API update until the mod is updated.

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) and [Fabric API](https://modrinth.com/mod/fabric-api).
2. Download the jar from this repository's [Releases](https://github.com/urntt/modlist/releases) page. Each release supports a single Minecraft version, shown after the `+` in its version number. For example, `1.0.0+26.3` is for Minecraft 26.3.
3. Put the jar into your `.minecraft/mods` folder.

[Mod Menu](https://modrinth.com/mod/modmenu) is optional. When installed, it opens the mod's settings from its mod list.

## Usage

Out of the box the mod changes nothing about what your client sends. It only shows join notices and records a report about each server.

Open the settings with the **Open modlist Settings** key binding in **Options → Controls → Key Binds** (unbound by default), or from Mod Menu. All settings are saved to `config/modlist.json` as soon as you change them.

### How servers see your mods

Fabric servers do not receive a list of your mods. They see:

- your **client brand**, which is `fabric` on Fabric clients;
- the **channels** your mods register for networking, named like `modid:channel`, so their namespaces reveal most networking mods;
- the data your mods send, and their answers to the server's login queries;
- translations: a server can put a mod's translation key or key binding name on a sign or an item and read back what your client turns it into.

Mods without channels are invisible to servers, except through translations.

### Join notices

After you join a server, chat lists, when there are any:

- the registry entries that were ignored, grouped by namespace (hover for details);
- the mods the server has and your client does not;
- your mods the server announced no channels for. The server only announces the channels it receives on, so this also lists mods that only receive data from the server.

These are informational; differences never block joining. Turn them off with **Join Notices** on the main settings screen.

### Settings profiles

All connection settings belong to a profile. The **Default Settings** apply to every server without its own settings. **Server Settings** lists per-server profiles: type an address and press **Add** to add one as a copy of the default settings, then **Edit** it. Each matches by the address you connect to, ignoring case. An address without a port, such as `mc.example.com`, matches the server on any port; with a port, such as `mc.example.com:25566`, only that port. The first matching profile is used, and profiles that never match because an earlier one matches first are shown in red.

The **Server Report** shows the last server's address, which settings were used, the server's brand and channels by namespace, the channels hidden and faked, and the registry entries ignored. **Edit Settings for This Server** opens or creates that server's profile.

Singleplayer worlds, worlds you open to LAN, and server list pings are never affected.

### Joining

| Setting | Default | Meaning |
| --- | --- | --- |
| Ignore Unknown Registry Entries | Off | Join servers that send registry entries or registries your client does not have instead of being disconnected. See [Risks](#risks). When off and a server is refused, the disconnect message points to this setting. |
| Placeholders | Air, air, marker | What the server's unknown blocks, items and entity types appear as. The marker entity is invisible. Any block, item or entity type ID works, for example `minecraft:barrier`. Unknown entries of other registries use that registry's default entry or its first entry; `placeholders` in `config/modlist.json` can set them too. |

### Reported to the server

**Reported Mods** chooses what the server sees:

| Value | Brand | Hidden mods | Faked mods |
| --- | --- | --- | --- |
| Unchanged (default) | real | none | none |
| Mimic Vanilla | `vanilla` | all, and their data and login answers are suppressed | none |
| Mimic Server | real | those the server does not announce | every channel the server announces |
| Custom | the settings below | the settings below | the settings below |

The custom settings are only shown when **Custom** is selected. Choosing another value keeps them for later.

| Setting | Default | Meaning |
| --- | --- | --- |
| Client brand | empty | The brand to report, for example `vanilla`. Empty keeps the real brand. |
| Hidden Mods | none | Channel namespaces whose channels, data and login answers are hidden from the server. The `minecraft` namespace cannot be hidden. |
| Fake Mods | none | Mods to claim although they are not installed. A mod ID claims every channel the server announces in that namespace; a full channel ID such as `examplemod:handshake` claims that channel. The server only announces the channels it receives on, so a server mod that checks a different channel needs that channel's full ID. |
| Protect Hidden Mods' Translations | On | Shown unless the value is **Unchanged**. Signs you edit and anvil names keep hidden mods' translation keys and key binding names untranslated, as a client without those mods would. |

## Development

Building requires the JDK version set by `java_version` in `gradle.properties`.

Build the mod:

```bash
./gradlew build
```

The jar is written to `build/libs/`.

Run the client game tests, which start Minecraft, check the configuration and the decisions about what to report and what to ignore, and connect to a local dedicated server to check every report mode, ignored registry entries with their placeholders, and translation probing protection:

```bash
./gradlew runClientGameTest
```

The game tests need a display. On a headless Linux machine, run them under Xvfb. Xvfb offers no sRGB-capable OpenGL visuals, so install Mesa's Vulkan driver (`mesa-vulkan-drivers` on Ubuntu) for the game to fall back to:

```bash
xvfb-run -a -s "-screen 0 1920x1080x24" ./gradlew runClientGameTest
```

Screenshots taken by the tests are saved to `build/run/clientGameTest/screenshots/`.

The multiplayer tests start a local dedicated server in the same JVM, so the test setup in `build.gradle` accepts the [Minecraft EULA](https://aka.ms/MinecraftEULA) for that test server.

## License

[MIT](LICENSE)
