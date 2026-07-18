package org.quartzlang.examples.paper.host

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage

/** Quartz writes messages in MiniMessage; this is the one place that turns them into components. */
object Text {
    private val mini = MiniMessage.miniMessage()

    fun of(message: String): Component = mini.deserialize(message)

    fun escape(text: String): String = mini.escapeTags(text)
}
