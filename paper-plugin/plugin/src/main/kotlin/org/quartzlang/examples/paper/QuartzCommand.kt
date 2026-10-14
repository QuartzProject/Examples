package org.quartzlang.examples.paper

import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.quartzlang.embed.QuartzModule
import org.quartzlang.embed.api.ModuleMetadata.AttributedFunction

/**
 * A Paper command for a Quartz function marked `@command("name", description = ..., permission = ...,
 * suggest = fn)`. A command takes the sender and, when it wants them, the arguments; a suggester takes the
 * arguments and, before them, the sender when it wants it. States come on their own.
 */
class QuartzCommand(
    private val plugin: QuartzExamplePlugin,
    private val module: QuartzModule,
    private val function: AttributedFunction,
) : BasicCommand {
    val name: String = function.attribute().value()
    val description: String = function.attribute().get("description", "")
    private val permission: String? = function.attribute().named("permission").map { it.value() }.orElse(null)
    private val suggester: String? = function.attribute().named("suggest").map { it.value() }.orElse(null)
    private val takesArguments = function.parameters().size > 1

    override fun execute(source: CommandSourceStack, args: Array<String>) {
        val sender = source.sender
        plugin.guard(sender, name) {
            if (takesArguments) {
                module.invoke(function.function(), sender, args.toList())
            } else {
                module.invoke(function.function(), sender)
            }
        }
    }

    override fun suggest(source: CommandSourceStack, args: Array<String>): Collection<String> {
        val suggest = suggester ?: return emptyList()
        val found = plugin.guard(source.sender, "$name suggestions") {
            val export = module.export(suggest)
            if (export.signature().arity() > 1) {
                module.invoke(export, source.sender, args.toList())
            } else {
                module.invoke(export, args.toList())
            }
        }
        return (found as? List<*>)?.map { it.toString() } ?: emptyList()
    }

    override fun permission(): String? = permission
}
