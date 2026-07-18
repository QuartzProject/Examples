package org.quartzlang.examples.paper.host

import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.quartzlang.embed.api.QuartzFunction
import org.quartzlang.embed.api.QuartzHandle
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import org.bukkit.entity.Player as BukkitPlayer

/** An online player, as a Quartz handle. Locations cross as plain numbers, since structs cannot cross yet. */
@QuartzHandle
class Player private constructor(val bukkit: BukkitPlayer) {
    @QuartzFunction
    fun name(): String = bukkit.name

    @QuartzFunction
    fun uuid(): String = bukkit.uniqueId.toString()

    @QuartzFunction
    fun sender(): Sender = Sender.of(bukkit)

    @QuartzFunction
    fun send(message: String) = bukkit.sendMessage(Text.of(message))

    @QuartzFunction
    fun actionBar(message: String) = bukkit.sendActionBar(Text.of(message))

    @QuartzFunction
    fun hasPermission(node: String): Boolean = bukkit.hasPermission(node)

    @QuartzFunction
    fun hasPlayedBefore(): Boolean = bukkit.hasPlayedBefore()

    @QuartzFunction
    fun world(): String = bukkit.world.name

    @QuartzFunction
    fun x(): Double = bukkit.location.x

    @QuartzFunction
    fun y(): Double = bukkit.location.y

    @QuartzFunction
    fun z(): Double = bukkit.location.z

    @QuartzFunction
    fun yaw(): Float = bukkit.location.yaw

    @QuartzFunction
    fun pitch(): Float = bukkit.location.pitch

    /** False when the world does not exist (any more). */
    @QuartzFunction
    fun teleport(world: String, x: Double, y: Double, z: Double, yaw: Float, pitch: Float): Boolean {
        val target = Bukkit.getWorld(world) ?: return false
        return bukkit.teleport(Location(target, x, y, z, yaw, pitch))
    }

    @QuartzFunction
    fun toSpawn(): Boolean = bukkit.teleport(bukkit.world.spawnLocation)

    @QuartzFunction
    fun gameMode(): String = bukkit.gameMode.name.lowercase()

    @QuartzFunction
    fun health(): Double = bukkit.health

    @QuartzFunction
    fun maxHealth(): Double = bukkit.getAttribute(Attribute.MAX_HEALTH)?.value ?: 20.0

    @QuartzFunction
    fun food(): Int = bukkit.foodLevel

    @QuartzFunction
    fun level(): Int = bukkit.level

    @QuartzFunction
    fun ping(): Int = bukkit.ping

    @QuartzFunction
    fun heal() {
        bukkit.health = maxHealth()
        bukkit.foodLevel = 20
        bukkit.saturation = 20f
        bukkit.fireTicks = 0
    }

    /** Gives items by material name (`iron_sword`); false when the material or the enchantment is unknown. */
    @QuartzFunction
    fun give(material: String, amount: Int, enchantment: String, level: Int): Boolean {
        val item = item(material, amount, enchantment, level) ?: return false
        bukkit.inventory.addItem(item).values.forEach { bukkit.world.dropItem(bukkit.location, it) }
        return true
    }

    /** Puts an item in an armour slot: head, chest, legs or feet. */
    @QuartzFunction
    fun equip(slot: String, material: String, enchantment: String, level: Int): Boolean {
        val target = when (slot) {
            "head" -> EquipmentSlot.HEAD
            "chest" -> EquipmentSlot.CHEST
            "legs" -> EquipmentSlot.LEGS
            "feet" -> EquipmentSlot.FEET
            else -> return false
        }
        val item = item(material, 1, enchantment, level) ?: return false
        bukkit.inventory.setItem(target, item)
        return true
    }

    @QuartzFunction
    fun playSound(sound: String) {
        bukkit.playSound(Sound.sound(Key.key(sound), Sound.Source.MASTER, 1f, 1f))
    }

    private fun item(material: String, amount: Int, enchantment: String, level: Int): ItemStack? {
        val type = Material.matchMaterial(material)?.takeIf { it.isItem } ?: return null
        val stack = ItemStack(type, amount.coerceIn(1, type.maxStackSize))
        if (enchantment.isNotEmpty()) {
            val enchant = enchantments.get(NamespacedKey.minecraft(enchantment)) ?: return null
            stack.addUnsafeEnchantment(enchant, level)
        }
        return stack
    }

    companion object {
        private val enchantments = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT)
        private val online = ConcurrentHashMap<UUID, Player>()

        /** The wrapper of an online player; a player who logs in again gets a fresh one. */
        fun of(player: BukkitPlayer): Player = online.compute(player.uniqueId) { _, known ->
            if (known != null && known.bukkit === player) known else Player(player)
        }!!

        fun forget(player: BukkitPlayer): Player? = online.remove(player.uniqueId)
    }
}
