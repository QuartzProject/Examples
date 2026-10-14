@file:Suppress("EXTENSION_SHADOWED_BY_MEMBER")
@file:JvmName("PlayerApi")

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
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.quartzlang.embed.api.QuartzFunction

// Paper's own Player becomes the Quartz handle `Player`: these extensions are its methods there.

/** The name shown in chat. */
@QuartzFunction(property = true)
fun Player.name(): String = name

@QuartzFunction(property = true)
fun Player.uuid(): String = uniqueId.toString()

/** The name of the world the player is in. */
@QuartzFunction(property = true)
fun Player.world(): String = world.name

@QuartzFunction(property = true)
fun Player.gameMode(): String = gameMode.name.lowercase()

@QuartzFunction(property = true)
fun Player.health(): Double = health

@QuartzFunction(property = true)
fun Player.maxHealth(): Double = getAttribute(Attribute.MAX_HEALTH)?.value ?: 20.0

@QuartzFunction(property = true)
fun Player.food(): Int = foodLevel

@QuartzFunction(property = true)
fun Player.level(): Int = level

/** The round trip to the client in milliseconds. */
@QuartzFunction(property = true)
fun Player.ping(): Int = ping

/** Sends a MiniMessage text. */
@QuartzFunction
fun Player.send(message: String) = sendMessage(Text.of(message))

@QuartzFunction
fun Player.actionBar(message: String) = sendActionBar(Text.of(message))

@QuartzFunction
fun Player.hasPermission(node: String): Boolean = hasPermission(node)

@QuartzFunction
fun Player.hasPlayedBefore(): Boolean = hasPlayedBefore()

/** Where the player stands. */
@QuartzFunction
fun Player.spot(): Spot = location.let { Spot(world.name, it.x, it.y, it.z, it.yaw.toDouble(), it.pitch.toDouble()) }

/** False when the spot's world does not exist (any more). */
@QuartzFunction
fun Player.teleport(spot: Spot): Boolean {
    val target = Bukkit.getWorld(spot.world) ?: return false
    return teleport(Location(target, spot.x, spot.y, spot.z, spot.yaw.toFloat(), spot.pitch.toFloat()))
}

@QuartzFunction
fun Player.toSpawn(): Boolean = teleport(world.spawnLocation)

/** Fills health and hunger and puts out fire. */
@QuartzFunction
fun Player.heal() {
    health = maxHealth()
    foodLevel = 20
    saturation = 20f
    fireTicks = 0
}

/** False when the material or the enchantment is unknown; what does not fit drops at the player's feet. */
@QuartzFunction
fun Player.give(item: Item): Boolean {
    val stack = stackOf(item) ?: return false
    inventory.addItem(stack).values.forEach { world.dropItem(location, it) }
    return true
}

/** Puts an item in an armour slot, replacing what was there. */
@QuartzFunction
fun Player.equip(slot: Slot, item: Item): Boolean {
    val stack = stackOf(item.copy(amount = 1)) ?: return false
    inventory.setItem(EquipmentSlot.valueOf(slot.name), stack)
    return true
}

@QuartzFunction
fun Player.playSound(sound: String) {
    playSound(Sound.sound(Key.key(sound), Sound.Source.MASTER, 1f, 1f))
}

private val enchantments by lazy { RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT) }

private fun stackOf(item: Item): ItemStack? {
    val type = Material.matchMaterial(item.material)?.takeIf { it.isItem } ?: return null
    val stack = ItemStack(type, item.amount.coerceIn(1, type.maxStackSize))
    if (item.enchantment.isNotEmpty()) {
        val enchant = enchantments.get(NamespacedKey.minecraft(item.enchantment)) ?: return null
        stack.addUnsafeEnchantment(enchant, item.level)
    }
    return stack
}
