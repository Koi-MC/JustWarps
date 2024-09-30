# Just Warps

### What is it?
No fuss, easy to use and configure warp teleports plugin for Bukkit/Spigot/Paper. Operators set up warps, players use the warps. That's it.

### Why?
- Reduce large plugin bloat by keeping warps separated out to its own simple and easy to follow plugin
- Reduce permission bloat by having Ops control what locations are considered warps, while letting all players teleport to them
- Easily configure warp cooldown
- Easily configure if players can tele through dimensions or not

### Config
[config.yml](https://github.com/Koi-MC/JustWarps/blob/master/src/main/resources/config.yml) contains the following adjustable variables:

```
cooldown: 10
through_dimension: false
```
- Cooldown lets you change time between warps in seconds. Setting to 0 disables cooldown. Default 10 seconds
- Through Dimension allows you to decide if you want players to be able to warp to locations that are in different dimensions from them. For example, allowing a player to warp from the Overworld to a warp that was set in The Nether or The End. Default false, disable through dimension

### Commands
By default, ONLY server Ops will have access to these commands:
```
/setwarp <name>
/delwarp <name>
/reloadwarpconfig (and /warpconfigreload)
```
All players will have access to these commands:
```
/warp <name>
/warplist (and /listwarp)
```
