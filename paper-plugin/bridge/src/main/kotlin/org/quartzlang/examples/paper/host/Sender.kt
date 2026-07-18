package org.quartzlang.examples.paper.host

import org.bukkit.command.CommandSender
import org.quartzlang.embed.api.QuartzFunction
import org.quartzlang.embed.api.QuartzHandle
import java.util.Collections
import java.util.IdentityHashMap
import org.bukkit.entity.Player as BukkitPlayer

/** Whoever runs a command: the console or a player. Quartz holds it as an opaque handle. */
@QuartzHandle
class Sender private constructor(val bukkit: CommandSender) {
    @QuartzFunction
    fun name(): String = bukkit.name

    /** Sends a MiniMessage text. */
    @QuartzFunction
    fun send(message: String) = bukkit.sendMessage(Text.of(message))

    @QuartzFunction
    fun hasPermission(node: String): Boolean = bukkit.hasPermission(node)

    @QuartzFunction
    fun isPlayer(): Boolean = bukkit is BukkitPlayer

    /** The player behind this sender; a trap in Quartz when it is the console, so ask isPlayer() first. */
    @QuartzFunction
    fun player(): Player = Player.of(bukkit as? BukkitPlayer ?: error("${bukkit.name} is not a player"))

    companion object {
        // One wrapper per sender, so Quartz sees the same handle every time; players are forgotten when they quit.
        private val known = Collections.synchronizedMap(IdentityHashMap<CommandSender, Sender>())

        fun of(sender: CommandSender): Sender = known.getOrPut(sender) { Sender(sender) }

        fun forget(sender: CommandSender): Sender? = known.remove(sender)
    }
}
