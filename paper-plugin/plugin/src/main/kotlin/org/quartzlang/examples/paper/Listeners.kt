package org.quartzlang.examples.paper

import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.quartzlang.embed.QuartzModule
import org.quartzlang.examples.paper.host.BlockEvent
import org.quartzlang.examples.paper.host.Text
import org.quartzlang.examples.paper.quartz.ChatQuartz

/**
 * Hands Paper's events to the Quartz functions marked `@on(Event)`: PlayerJoin and PlayerQuit take the player and
 * return the message everybody sees, BlockBreak and BlockPlace take a BlockEvent and return whether it may happen.
 */
class Listeners(
    private val plugin: QuartzExamplePlugin,
    private val scripts: QuartzModule,
    private val chat: ChatQuartz,
) : Listener {
    private val byEvent = scripts.functions("on").groupBy { it.attribute().value() }
    val handlers get() = byEvent.values.sumOf { it.size }

    private fun message(player: Player, event: String): Component? {
        var shown: Component? = null
        for (handler in byEvent[event].orEmpty()) {
            val text = plugin.guard(player, event) { scripts.invoke(handler.function(), player) as String? }
            text?.let { shown = Text.of(it) }
        }
        return shown
    }

    private fun allow(event: Cancellable, player: Player, block: String, name: String) {
        val data = BlockEvent(player, block)
        for (handler in byEvent[name].orEmpty()) {
            val allowed = plugin.guard(player, name) {
                !scripts.accepts(handler, data) || scripts.invoke(handler.function(), data) == true
            }
            if (allowed == false) event.isCancelled = true
        }
    }

    @EventHandler
    fun join(event: PlayerJoinEvent) {
        message(event.player, "PlayerJoin")?.let { event.joinMessage(it) }
    }

    @EventHandler
    fun quit(event: PlayerQuitEvent) {
        message(event.player, "PlayerQuit")?.let { event.quitMessage(it) }
        // A player who left is released, so the modules do not keep the object alive.
        scripts.handles().release(event.player)
        synchronized(chat) { chat.module().handles().release(event.player) }
    }

    @EventHandler(ignoreCancelled = true)
    fun breakBlock(event: BlockBreakEvent) = allow(event, event.player, event.block.type.key.key, "BlockBreak")

    @EventHandler(ignoreCancelled = true)
    fun placeBlock(event: BlockPlaceEvent) = allow(event, event.player, event.block.type.key.key, "BlockPlace")

    /** Chat arrives on other threads, so its own module filters it, entered under its lock, through the facade. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun chat(event: AsyncChatEvent) {
        val typed = PlainTextComponentSerializer.plainText().serialize(event.message())
        val shown = plugin.guard(event.player, "chat") { synchronized(chat) { chat.filter(event.player, typed) } }
        when {
            shown == null -> event.isCancelled = true
            shown != typed -> event.message(Component.text(shown))
        }
    }
}
