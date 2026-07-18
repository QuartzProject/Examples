package org.quartzlang.examples.paper.host

import org.bukkit.Bukkit
import org.quartzlang.embed.api.QuartzFunction

/** What Quartz may ask of the server as a whole. */
object Server {
    @QuartzFunction
    fun onlinePlayers(): List<Player> = Bukkit.getOnlinePlayers().map { Player.of(it) }

    @QuartzFunction
    fun broadcast(message: String) {
        Bukkit.getServer().broadcast(Text.of(message))
    }

    @QuartzFunction
    fun serverName(): String = "${Bukkit.getName()} ${Bukkit.getVersion()}"

    @QuartzFunction
    fun minecraftVersion(): String = Bukkit.getMinecraftVersion()

    @QuartzFunction
    fun maxPlayers(): Int = Bukkit.getMaxPlayers()

    @QuartzFunction
    fun worldNames(): List<String> = Bukkit.getWorlds().map { it.name }

    /** Quartz has no clock of its own: the host decides what time it is. */
    @QuartzFunction
    fun nowMillis(): Long = System.currentTimeMillis()

    /** Escapes MiniMessage tags in text that came from a player, so it shows as typed. */
    @QuartzFunction
    fun plain(text: String): String = Text.escape(text)
}
