# Project Ash: Annouce 'Em All!

**Project Ash** is a minecraft addon for the popular Cobblemon mod. With Ash, you'll be informed of all shiny and legendary spawns that may be popping up in your server, not just in-game but also in discord

## ✨ Features

- 🔊 Server-wide announcements when shiny or legendary Pokémon spawn
- 💬 Discord webhook notifications for rare Pokémon events
- ⚙️ Automatic config generation (ProjectAsh.conf) with customizable options through commands
- 🧩 Modular design — fits easily into multi-loader environments (Fabric, Forge, etc.)  

---

## 📦 Installation

1. Download the latest version of Project Ash from github releases
2. Place the .jar into your server’s mods folder.
3. Start the server.
4. The first startup will automatically generate a configuration file:
`/config/ProjectAsh.conf`
5. Perform the update webhook command in-side the game to configure the discord server, no restart necessary! :)

## ⚙️ Configuration/Commands
Project Ash will create a config file for you which is split into 3 categories: Discord, In-game, and Player.
#### `**All commands are prefixed with /projectash**`

### **Discord**
This will hold the settings for discord announcements:

| Setting    | Description                                           | Default      | Type   | Minecraft Command    |
|------------|-------------------------------------------------------|--------------|--------|----------------------|
| Enabled    | Toggles Announcements in Discord                      | Enabled      | Bool   | DiscordEnabled       |
| Thumbnails | Toggles Thumbnails of sprites in embed message        | Enabled      | Bool   | DiscordThumbnails    |
| Webhook    | Updates the discord webhook without restarting server | Template URL | String | DiscordWebhookUpdate |

### **In-Game**
This will hold the settings for In-Game announcements:

| Setting | Description                      | Default      | Type   | Minecraft Command |
|---------|----------------------------------|--------------|--------|-------------------|
| Enabled | Toggles Announcements in In-Game | Enabled      | Bool   | InGameEnabled     |


## 💬 Example Announcements
### 📢 In-Game
<img alt="In-Game_Announcement" src="https://github.com/user-attachments/assets/d3115993-9db7-4d3e-9da2-8db138b6f0f8" />

### 🤖 Discord Notification
<img width="321" height="199" alt="Discord_Announcement" src="https://github.com/user-attachments/assets/3969fb5c-7b39-4265-ba1d-2fddaf1a2a63" />

