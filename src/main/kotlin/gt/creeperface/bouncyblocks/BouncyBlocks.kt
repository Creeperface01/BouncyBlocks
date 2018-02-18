package gt.creeperface.bouncyblocks

import cn.nukkit.Player
import cn.nukkit.event.EventHandler
import cn.nukkit.event.Listener
import cn.nukkit.event.entity.EntityDamageEvent
import cn.nukkit.event.player.PlayerMoveEvent
import cn.nukkit.math.NukkitMath
import cn.nukkit.math.Vector3
import cn.nukkit.plugin.PluginBase
import cn.nukkit.utils.ConfigSection
import java.util.stream.Collectors

/**
 * @author CreeperFace
 */
class BouncyBlocks : PluginBase(), Listener {

    private var bouncyBlocks: Set<BlockEntry> = emptySet()
    private var jumpPower = 0.toDouble()

    private val jumpers: MutableSet<Long> = mutableSetOf()

    override fun onEnable() {
        saveDefaultConfig()
        val cfg = config

        jumpPower = cfg.getDouble("jump_power", 2.toDouble())

        val sections = cfg.getList("blocks")

        bouncyBlocks = sections.stream().filter { it is ConfigSection }.map {
            if (it is ConfigSection) {
                val id = it.getInt("id")
                val meta = it.getInt("damage", -1)

                return@map BlockEntry(id, meta)
            }

            return@map BlockEntry(-1, 0)
        }.collect(Collectors.toSet())

        server.pluginManager.registerEvents(this, this)
    }

    @EventHandler
    fun onMove(e: PlayerMoveEvent) {
        val p = e.player

        if (!p.isOnGround || p.isSneaking) {
            return
        }

        val block = p.getLevel().getBlockIdAt(p.floorX, NukkitMath.floorDouble(p.y - 0.3), p.floorZ)
        val damage = p.getLevel().getBlockDataAt(p.floorX, NukkitMath.floorDouble(p.y - 0.3), p.floorZ)

        var found = false
        bouncyBlocks.forEach {
            if (it.id == block && (it.damage == -1 || it.damage == damage)) {
                found = true
                return@forEach
            }
        }

        if (found) {
            p.motion = Vector3(0.toDouble(), jumpPower)
            jumpers.add(p.id)
        }
    }

    @EventHandler
    fun onDamage(e: EntityDamageEvent) {
        val p = e.entity

        if (e.cause == EntityDamageEvent.DamageCause.FALL && p is Player) {
            if (jumpers.remove(p.id)) {
                e.setCancelled()
            }
        }
    }

    private inner class BlockEntry(val id: Int, val damage: Int)
}