package org.quartzlang.examples.paper

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin
import org.quartzlang.embed.QuartzEmbed
import org.quartzlang.embed.QuartzScriptException
import org.quartzlang.examples.paper.host.PluginHost
import org.quartzlang.examples.paper.quartz.ChatQuartz
import org.quartzlang.examples.paper.quartz.ExampleQuartz
import org.quartzlang.jvm.Sandbox
import java.io.File

/** Wiring, and nothing else: the commands and listeners are in `src/main/quartz`, found by their attributes. */
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

        val commands = scripts.module().functions("command").map { QuartzCommand(this, scripts.module(), it) }
        lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
            commands.forEach { event.registrar().register(it.name, it.description, it) }
        }
        val listeners = Listeners(this, scripts.module(), chat)
        server.pluginManager.registerEvents(listeners, this)
        server.scheduler.runTaskTimer(this, Runnable { save() }, AUTOSAVE_TICKS, AUTOSAVE_TICKS)
        val stats = scripts.stats()
        logger.info("Quartz modules ${embed.modules()}: ${commands.size} commands, ${listeners.handlers} listeners; " +
            "remembering ${stats.homes()} homes, ${stats.warps()} warps and ${stats.cooldowns()} cooldowns")
    }

    override fun onDisable() {
        if (!::scripts.isInitialized) return
        save()
        scripts.close()
        chat.close()
    }

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

    private fun load(sandbox: Sandbox, file: File) {
        if (file.isFile) {
            sandbox.importState(file.readText())
        }
    }

    private companion object {
        const val AUTOSAVE_TICKS = 20L * 60 * 5
    }
}
