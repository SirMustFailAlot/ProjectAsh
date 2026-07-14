# Project Ash: Annouce 'Em All!

**Project Ash** is a minecraft adding that is pushing the announcement space, not just including shiny, legendary, or perfect spawns, but also battle tracking and showcasing!

## ✨ Features

### 📢 Milestone & Spawn Announcements
* **Shiny & Perfect Tracking:** Instantly flags and logs shiny spawns or perfect 100% IV Pokémon catches/spawns.
* **Rarity Tier Labels:** Formats announcements using built-in labels for specific groups like `Legendary`, `Mythical`, `Ultra Beast`, and `Paradox`.
* **Egg Hatch Notifications:** Announces to the community when a player successfully hatches an egg.
* **Entity Load Catching:** Optional `unknownspawns` tracking logs entities loading directly from world generation/chunk loads.
* **CatchEmAll Dex Modes:** As a player, chose to enable CatchEmAll and have triggers if a Pokemon that spawns that would contribute to your LivingDex, FormDex, ShinyDex etc.

### ⚔️ Battle Tracking & Fog of War
* **In-Game Start Flags:** Automatically prints a announcement when an engagement initializes.
* **Discord Match Summaries:** Dispatches rich telemetry data upon battle completion, detailing the Pokémon deployed, their living/fainted condition, and automated sprite thumbnails.
* **Fog of War Protection:** Keeps unseen or un-battled opponent slots hidden behind a `???` layout to avoid strategic scouting.

### 🃏 Showcases
* **CobbleTCG Integration** Announce your cool cards within the chat, with the ability to see the card when hovering over the card name. This can be shown off either by pressing the showcase key in hand, or by hovering an item in the inventory.
* **Pokemon Announcing Integration** When an announcement from pokemon spawns, appear, these will be integrated with showcase! Hover over the species name to see the pokemon sprite.
* **Cord Teleportation** Enhancing announcements that have cords, these will now have a clickable cord position that will teleport you.


### 🏆 Supported Dex Modes
Enabling CatchEmAll will allow you to chose a dex type to proceed, below are a list of the supported dex and their criteria.

`LIVINGDEX`

Rule: The player must physically own at least one copy of every Pokémon species in their PC or Party.
Note: This mode does not track shiny status or specific regional forms—any variation of the species counts.

`SHINYDEX`

Rule: The player must physically own a Shiny variant of the Pokémon species in their PC or Party to get credit. Normal versions are ignored.

`EVERYDEX`

Rule: Checks the player's historical Pokédex data. As long as the base species has been caught at least once in their career, they get credit—even if they have since traded or released the Pokémon.

`FORMDEX`

Rule: An advanced Living Dex challenge. The player must physically own a copy of every single regional and seasonal form variation (e.g., Alolan vs. Kantonian, or the different Deerling seasons) in their PC or Party.
Note: Overwhelming aesthetic patterns like Spinda or Vivillon are excluded to keep the challenge fun.

`MASTERLIVINGDEX`

Rule: The ultimate collector's challenge. To achieve completeness, the player's PC and Party must physically hold both the normal version and the shiny version for every single species and form variation.

---

## 📦 Installation
1. Download the latest version of Project Ash.
2. Place the .jar into your server’s mods folder and the client mods folder.
3. Start the server.
4. The first startup will automatically generate a configuration file:
`/config/ProjectAsh.conf`
5. Perform the update webhook command in-side the game to configure the discord server, no restart necessary! :)
---
## 🛠️ Command Navigation & Reference

Instead of forcing users to memorize long strings of syntax or modify backend files, configurations can be fully manipulated natively using interactive click elements directly within the Minecraft chat window.

### 🖱️ Clickable UI Routing Map

Running the base `/projectash` command opens a responsive textual wizard. Clicking the brackets automatically populates and advances the next node in the tree until you execute an action:

```text
/projectash
 └── [Server]    [Player]

*Example: Modifying Shiny Spawns*
 👤 Click [Server] 
    └── [Back] [Discord] [Showcase] [Ingame] [Perfect] [Shiny] [UnknownSpawns] [Label] [Special] [Blacklist]

 👤 Click [Shiny]
    └── [Back] [Check] [Enable] [Disable]

 👤 Click [Enable]
    └── 💬 Output: "[Project Ash] Shiny Checks: ENABLED"
```

### 👤 Player Commands
*These properties dictate **in-game announcements only** and are available to all active players.*

| Function / Subfunction | Description | Command Base | Actions Available / Arguments |
| :--- | :--- | :--- | :--- |
| `special` | A custom personal whitelist of target species a player wants to track for themselves. | `/ash player special` | `check`, `enable`, `disable`, `add <species> [shinyFlag]`, `remove <species>`, `clear` |
| `catchemall` | Tracks form metrics, alerting the player if a spawn matches an entry lacking within their specific form/shiny Pokédex. | `/ash player catchemall` | `check`, `update` |
| `catchemall / localspawnsonly` | Safeguards tracking by only firing "Catch 'Em All" logs if you are the closest player driving that spawn radius. | `/ash player catchemall localspawnsonly` | `check`, `enable`, `disable` |

### 👑 Server Admin Commands
*These properties handle **global server-wide components and Discord pipelines**. Requires `server op` authority.*

| Function / Subfunction | Description | Command Base | Actions Available / Arguments |
| :--- | :--- | :--- | :--- |
| `discord` | Global system toggle turning the Discord webhook dispatch system on or off. | `/ash server discord` | `check`, `enable`, `disable` |
| `discord / webhook` | Views or updates the operational endpoint target string for your Discord channel. | `/ash server discord webhook` | `check`, `add <url>`, `remove` |
| `discord / thumbnails` | Toggles rendering visual asset additions (such as custom Pokémon sprites) into webhook embeds. | `/ash server discord thumbnails` | `check`, `enable`, `disable` |
| `ingame` | Master switch controlling all global in-game chat announcement elements. | `/ash server ingame` | `check`, `enable`, `disable` |
| `showcase` | Runs a high-level status sweep evaluating all downstream item showcase modules at once. | `/ash server showcase` | `check` |
| `showcase / cobbletcg` | Adjusts authorization rules regarding player hotkey card/pack presentations. | `/ash server showcase cobbletcg` | `check`, `enable`, `disable` |
| `perfect` | Manages server-wide announcement covering maximum 100% IV generation alerts. | `/ash server perfect` | `check`, `enable`, `disable` |
| `shiny` | Manages server-wide announcement covering standard shiny wildlife spawns. | `/ash server shiny` | `check`, `enable`, `disable` |
| `unknownspawns` | Toggles filtering notifications for species that materialize off an entity or chunk reload cycle. | `/ash server unknownspawns` | `check`, `enable`, `disable` |
| `label` | Dictates custom string tags evaluated by the announcement engine (e.g., Paradox, Mythical). | `/ash server label` | `check`, `add <label>`, `remove <label>`, `clear` |
| `special` | Core master whitelist. Matching targets fire across both local chat channels and Discord channels. | `/ash server special` | `check`, `add <species> [shinyFlag]`, `remove <species>`, `clear` |
| `blacklist` | Suppresses global tracking for particular species. *(Note: This cannot override individual player lists).* | `/ash server blacklist` | `check`, `add <species> [shinyFlag]`, `remove <species>`, `clear` |

### 💡 Flag Constraints & Syntax Examples

When using whitelist or blacklist mutation commands (`add`), you can attach an optional `[shinyFlag]` conditional modifier:
* `INCLUDE` *(Default)* — Appends the filtering behavior to standard and shiny variants equally.
* `EXCLUDE` — Explicitly drops shiny variations out of this specific filter rule scope.
* `ONLY` — Commands the filter rule to apply *exclusively* if the targeted entity generates shiny.

```text
/ProjectAsh player special add shuckle ONLY
/ProjectAsh server blacklist add pidgey INCLUDE
/ProjectAsh server label add "Ultra Beast"
/ProjectAsh player catchemall localspawnsonly enable
```
---
🤝 Credits

This project is proudly built as an extension of Cobblemon. We are incredibly grateful to the Cobblemon authors, artists, and developers whose hard work and open-source API made this enhancement possible.

Note: Project Ash is an unofficial fan-made addon and is not affiliated with or endorsed by the official Cobblemon team.
