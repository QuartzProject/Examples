package org.quartzlang.examples.paper.host

import org.bukkit.entity.Player
import org.quartzlang.embed.api.QuartzType

/** A place in a world; homes and warps are kept as these. */
data class Spot(val world: String, val x: Double, val y: Double, val z: Double, val yaw: Double, val pitch: Double)

/** Items to hand out, by Minecraft names: `iron_sword`, enchanted with `sharpness` at a level when given one. */
data class Item(val material: String, val amount: Int, val enchantment: String, val level: Int)

/** An armour slot. */
enum class Slot { HEAD, CHEST, LEGS, FEET }

/** A block a player broke or placed, by its lowercase Minecraft name (`stone`). */
@QuartzType
data class BlockEvent(val player: Player, val block: String)
