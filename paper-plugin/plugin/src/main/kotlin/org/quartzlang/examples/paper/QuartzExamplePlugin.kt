package org.quartzlang.examples.paper

import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin
import org.quartzlang.embed.QuartzEmbed
import org.quartzlang.embed.QuartzScriptException
import org.quartzlang.examples.paper.host.PluginHost
import org.quartzlang.examples.paper.host.Sender
import org.quartzlang.examples.paper.quartz.ChatQuartz
import org.quartzlang.examples.paper.quartz.ExampleQuartz
import java.io.File

/** Wiring, and nothing else: the commands and listeners are in `src/main/quartz`. */
class QuartzExamplePlugin : JavaPlugin() {
    private lateinit var scripts: ExampleQuartz
    private lateinit var chat: ChatQuartz
    private val stateFile get() = File(dataFolder, "state.json")
    private val chatStateFile get() = File(dataFolder, "chat-state.json")

    override fun onEnable() {
        if (!File(dataFolder, "kits.toml").exists()) {
            saveResource("kits.toml", false)
        }
        val embed = QuartzEmbed.builder()
            .classLoader(javaClass.classLoader)
            .host(PluginHost(this) { scripts.module().sandbox().memoryUsed() })
            .build()
        scripts = ExampleQuartz.open(embed)
        chat = ChatQuartz.open(embed)
        load(scripts.module().sandbox(), stateFile)
        load(chat.module().sandbox(), chatStateFile)

        val names = scripts.commands().map { it.substringBefore('|') to it.substringAfter('|') }
        lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
            names.forEach { (name, description) -> event.registrar().register(name, description, Command(name)) }
        }
        server.pluginManager.registerEvents(Listeners(this, scripts, chat), this)
        server.scheduler.runTaskTimer(this, Runnable { save() }, AUTOSAVE_TICKS, AUTOSAVE_TICKS)
        logger.info("Quartz modules ${embed.modules()} running ${names.size} commands")
    }

    override fun onDisable() {
        if (!::scripts.isInitialized) return
        save()
        scripts.close()
        chat.close()
    }

    /** `run` and `suggest` take states, and the generated facade does not cover such functions yet. */
    fun run(sender: CommandSender, command: String, args: List<String>) {
        guard(sender, command) { scripts.module().invoke("run", Sender.of(sender), command, args) }
    }

    @Suppress("UNCHECKED_CAST")
    fun suggest(sender: CommandSender, command: String, args: List<String>): List<String> =
        guard(sender, command) { scripts.module().invoke("suggest", Sender.of(sender), command, args) as List<String> }
            ?: emptyList()

    fun <T> guard(sender: CommandSender, what: String, call: () -> T): T? = try {
        call()
    } catch (e: QuartzScriptException) {
        logger.warning("Quartz failed in $what: ${e.message}")
        sender.sendMessage("Something went wrong running that; the console has the details.")
        null
    }

    private fun save() {
        dataFolder.mkdirs()
        stateFile.writeText(scripts.module().sandbox().exportState())
        chatStateFile.writeText(synchronized(chat) { chat.module().sandbox().exportState() })
    }

    private fun load(sandbox: org.quartzlang.jvm.Sandbox, file: File) {
        if (file.isFile) {
            sandbox.importState(file.readText())
        }
    }

    private inner class Command(private val name: String) : BasicCommand {
        override fun execute(source: CommandSourceStack, args: Array<String>) = run(source.sender, name, args.toList())

        override fun suggest(source: CommandSourceStack, args: Array<String>): Collection<String> =
            suggest(source.sender, name, args.toList())
    }

    private companion object {
        const val AUTOSAVE_TICKS = 20L * 60 * 5
    }
}
