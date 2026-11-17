# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html). Each version carries the targeted Minecraft version as build metadata, for example `1.0.0+26.3`.

## [Unreleased]

### Added

- Show join notices in chat that list ignored registry entries, the mods only the server has, and the client's mods the server announced no channels for. Mismatched mods never block joining.
- Add an option, off by default, to join servers that send registry entries or registries the client does not have. Unknown blocks, items and entity types appear as configurable placeholders (air, air and the invisible marker by default), and the disconnect message points to the option while it is off.
- Edit what the server sees: a custom client brand, hidden mods (by channel namespace, including their outgoing data and login answers) and fake mods (by namespace, mirroring the server's channels, or by channel ID).
- Add one-click buttons to mimic a vanilla client, mimic the server's mod set, or restore the real mods.
- Keep hidden mods' translation keys and key binding names untranslated when signs and anvils send text back to the server.
- Add settings profiles: default settings plus per-server settings matched by address.
- Add a server report that shows what the last server announced and what the client reported.
- Add a configuration screen built from vanilla widgets, reachable from Mod Menu (optional) and an "Open modlist Settings" key binding, unbound by default.
- Add English and Simplified Chinese translations.
