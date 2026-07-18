package org.quartzlang.examples.paper.host

import org.bukkit.plugin.java.JavaPlugin
import org.quartzlang.embed.api.QuartzFunction
import java.io.File

/** The plugin's own services; the instance is given to QuartzEmbed.Builder.host by the plugin. */
class PluginHost(private val plugin: JavaPlugin, private val memory: () -> Long) {
    @QuartzFunction
    fun log(message: String) = plugin.logger.info(message)

    /** A text file from the plugin's data folder, or null; names cannot leave the folder. */
    @QuartzFunction
    fun dataFile(name: String): String? {
        val folder = plugin.dataFolder.canonicalFile
        val file = File(folder, name).canonicalFile
        if (!file.path.startsWith(folder.path + File.separator) || !file.isFile) return null
        return file.readText()
    }

    @QuartzFunction
    fun pluginVersion(): String = plugin.pluginMeta.version

    @QuartzFunction
    fun memoryUsed(): Long = memory()
}
