@file:Suppress("EXTENSION_SHADOWED_BY_MEMBER")
@file:JvmName("SenderApi")

package org.quartzlang.examples.paper.host

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.quartzlang.embed.api.QuartzFunction

// Whoever runs a command, the console or a player: Paper's CommandSender as the Quartz handle `Sender`.

@QuartzFunction(property = true)
fun CommandSender.name(): String = name

/** Sends a MiniMessage text. */
@QuartzFunction
fun CommandSender.send(message: String) = sendMessage(Text.of(message))

@QuartzFunction
fun CommandSender.hasPermission(node: String): Boolean = hasPermission(node)

/** The player behind this sender, or null for the console. */
@QuartzFunction
fun CommandSender.player(): Player? = this as? Player
