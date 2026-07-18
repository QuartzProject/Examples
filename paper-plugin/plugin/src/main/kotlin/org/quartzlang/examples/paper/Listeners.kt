package org.quartzlang.examples.paper

import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.quartzlang.examples.paper.host.Player
import org.quartzlang.examples.paper.host.Sender
import org.quartzlang.examples.paper.host.Text
import org.quartzlang.examples.paper.quartz.ChatQuartz
import org.quartzlang.examples.paper.quartz.ExampleQuartz

/** Hands each event to Quartz and applies the answer; the decisions are made in `src/main/quartz/events`. */
class Listeners(
    private val plugin: QuartzExamplePlugin,
    private val scripts: ExampleQuartz,
    private val chat: ChatQuartz,
) : Listener {
    @EventHandler
    fun join(event: PlayerJoinEvent) {
        plugin.guard(event.player, "join") { event.joinMessage(Text.of(scripts.onJoin(Player.of(event.player)))) }
    }

    @EventHandler
    fun quit(event: PlayerQuitEvent) {
        plugin.guard(event.player, "quit") { event.quitMessage(Text.of(scripts.onQuit(Player.of(event.player)))) }
        // The handles of a player who left are released, so the plugin does not keep them alive.
        val player = Player.forget(event.player)
        val sender = Sender.forget(event.player)
        for (module in listOf(scripts.module(), chat.module())) {
            player?.let { module.handles().release(it) }
            sender?.let { module.handles().release(it) }
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun breakBlock(event: BlockBreakEvent) {
        val allowed = plugin.guard(event.player, "break") {
            scripts.canBreak(Player.of(event.player), event.block.type.key.key)
        }
        if (allowed == false) event.isCancelled = true
    }

    @EventHandler(ignoreCancelled = true)
    fun placeBlock(event: BlockPlaceEvent) {
        val allowed = plugin.guard(event.player, "place") {
            scripts.canPlace(Player.of(event.player), event.block.type.key.key)
        }
        if (allowed == false) event.isCancelled = true
    }

    /** Chat arrives on other threads; its module is only entered under its own lock. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun chat(event: AsyncChatEvent) {
        val typed = PlainTextComponentSerializer.plainText().serialize(event.message())
        val shown = plugin.guard(event.player, "chat") {
            synchronized(chat) { chat.module().invoke("filter", Player.of(event.player), typed) as String? }
        }
        when {
            shown == null -> event.isCancelled = true
            shown != typed -> event.message(Component.text(shown))
        }
    }
}
