Keepinventory For Newbs
=======================

A NeoForge mod for Minecraft 1.21.1 that gives every new player a keep-inventory grace period.

Feature
-------

* The countdown starts at the **first login** of a player on the server. The moment is stored in
  the player data file, so it survives server restarts and is never granted twice.
* While the grace period is active the player behaves like the vanilla `keepInventory` game rule:
  nothing is dropped on death and the inventory, the armour, the offhand item and the experience
  are given back on respawn.
* When the grace period is over the normal death rules apply again.
* The command `/keepinv` shows how much of the grace period is left.

Configuration
-------------

The mod uses a server config, it is stored in
`config/keepinventoryfornewbs-server.toml` next to the other server configs.

| Option              | Default | Description                                                                     |
|---------------------|---------|---------------------------------------------------------------------------------|
| `gracePeriodHours`  | `24`    | Length of the grace period in hours. `0` disables the feature completely.        |
| `keepExperience`    | `true`  | Whether the experience level is kept on death as well.                           |
| `announceOnJoin`    | `true`  | Whether players are told about their remaining grace period when they join.      |
| `applyInHardcore`   | `false` | Whether the grace period also applies on hardcore servers, where a death is final. |

Notes
-----

* The grace period is checked at the moment of the death, so a player that is still inside the
  grace period when they die keeps everything, even if the grace period ends a second later.
* A player that logs out while dead keeps their items, the inventory is restored when they
  respawn after logging back in.
* The feature is compatible with the vanilla `keepInventory` game rule, if the game rule is
  enabled the whole inventory of every player is kept anyway.

Building
--------

```
./gradlew build
```

The mod jar is written to `build/libs`. Use `./gradlew runClient` or `./gradlew runServer` to test
it in a development environment.

Testing
-------

The mod ships with a game test that kills a player inside the grace period and checks that the
inventory, the armour and the experience are restored on respawn and that nothing was dropped
into the world:

```
./gradlew runGameTestServer
```

Additional Resources:
=====================
Community Documentation: https://docs.neoforged.net/
NeoForged Discord: https://discord.neoforged.net/
