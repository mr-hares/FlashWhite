![flash white](https://cdn.modrinth.com/data/cached_images/fadbdd68885f729f124cc20bbfef09fb4d1e3002.png)

**FlashWhite** - plugin that automates server management, allowing players to submit requests through Discord modals and administrators to approve them with one-click buttons **approve/reject**.

---

**Features**
- Ability to create Discord statements.
- Saving a player by UUID. Licensed accounts can change their in-game nickname without worrying about losing access to the server.
- Support for hexadecimal color in messages
- Lightweight and high performance

---

**Installation**
-  `1.` Disable the default Minecraft white list in `server.properties` by setting the value to `white-list=false`
- `2.` Install the `.jar` file plugin in the plugins folder
- `3.` Start or restart your server

---

**Minecraft command list**
- /flashwhite reload - reload the plugin.
- /flashwhite add [nickname] [time] - add a player to the list.
- /flashwhite remove [nickname] - remove a player from the list.
- /flashwhite list [page] - show the white list.

**Discord command list**
- /setup - sending an information message.

**List of permissions**
- flashwhite.use - the ability to use the plugin's basic commands.
- flashwhite.reload - the ability to reload the configuration.
