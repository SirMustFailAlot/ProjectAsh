# Project Ash: Annouce 'Em All!

**Project Ash** is a minecraft addon for the popular Cobblemon mod. With Ash, you'll be informed of all shiny and legendary spawns that may be popping up in your server, not just in-game but also in discord

## ✨ Features

- 🔊 Server-wide announcements when shiny or legendary Pokémon spawn
- 💬 Discord webhook notifications for rare Pokémon events
- ⚙️ Automatic config generation (ProjectAsh.conf) with customizable options
- 🧩 Modular design — fits easily into multi-loader environments (Fabric, Forge, etc.)  

---

## 📦 Installation

1. Download the latest version of Project Ash from Modrinth
 or CurseForge
2. Place the .jar into your server’s mods folder.
3. Start the server.
4. The first startup will automatically generate a configuration file:
`/config/ProjectAsh.conf`
5. Stop the server (optional) and open the config file to adjust settings as desired.

## ⚙️ Configuration
Project Ash creates a configuration file named `ProjectAsh.conf` inside your `/config/` directory.
| Option        | Description | Default |
| ------------- | ------ |------ |
| discord_announcements   | Turns on and off discord annoucements (True = On, False = Off) |True |
| discord_webook         | Webhook for the Discord Channel where the annoucements will be sent (see Settting up a Webhook for help) | "" |
| server_announcement   | Turns on and off discord announcements (True = On, False = Off) | True |

## 💬 Example Announcements
### 📢 In-Game
![In-Game_Announcement](https://github.com/user-attachments/assets/d3115993-9db7-4d3e-9da2-8db138b6f0f8)

### 🤖 Discord Notification
<img width="321" height="199" alt="Discord_Announcement" src="https://github.com/user-attachments/assets/3969fb5c-7b39-4265-ba1d-2fddaf1a2a63" />

