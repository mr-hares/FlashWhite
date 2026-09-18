## FlashWhite — Automated Whitelist Management via Discord

**FlashWhite** is a lightweight, high-performance Minecraft plugin that completely automates your server's whitelist management. Players can submit access requests using intuitive **Discord Modals**, while administrators can instantly review, accept, or deny applications with **one-click buttons** right from their Discord server.

---

## Key Features

* **Seamless Discord Integration:** Painless application process via Discord Modals and easy management using interactive **Approve / Reject** buttons.
* **UUID-Based Storage:** Players are saved by their unique UUID. Licensed account holders can change their in-game nicknames without losing access or data.
* **HEX Color Support:** Full support for hexadecimal color codes to create beautiful, branded, and vibrant chat messages.
* **Ultra-Lightweight & Performant:** Highly optimized codebase that runs instantly without impacting your server's TPS or performance.

---

## Installation

1. Open your `server.properties` file and disable the default Minecraft whitelist by changing the value to:
   ```properties
   white-list=false
   ```
2. Drop the downloaded `.jar` file into your server's `/plugins/` folder.
3. Start or restart your server to generate the configuration files.
4. Set up your Discord Bot token and channel IDs in the generated `config.yml`.

---

## Commands & Permissions

### Minecraft In-Game Commands

| Command | Description | Permission Node |
| :--- | :--- | :--- |
| `/fw reload` | Reloads the plugin configuration files | `flashwhite.reload` |
| `/fw add [nickname]` | Manually adds a player to the whitelist | `flashwhite.add` |
| `/fw addtemp [nickname] [time]` | Temporarily adds a player for a specific duration | `flashwhite.add` |
| `/fw remove [nickname]` | Removes a player from the whitelist | `flashwhite.remove` |
| `/fw check [nickname]` | Displays player info and their linked Discord ID | `flashwhite.check` |
| `/fw list [page]` | Views the structured list of whitelisted participants | `flashwhite.list` |
| `/fw on` / `/fw off` | Enables or disables the whitelist mode | `flashwhite.toggle` |

> 💡 **Bypass Permission:** Grant `flashwhite.bypass` to allow specific users or staff groups to connect to the server regardless of the whitelist status.

### Discord Slash Commands

* `/setup_plugin` — Sends the initial information embed with the application button to the designated text channel.
