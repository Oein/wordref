package kr.oein.wordref

import org.bukkit.GameMode
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import kr.oein.wordref.Wordref
import org.bukkit.Bukkit
import org.bukkit.Location


class EventListener(val kv: FileKV) : Listener  {
    @EventHandler
    fun onJoin(e: PlayerJoinEvent) {
        val player = e.player
        Bukkit.getLogger().info("${player.name} joined the word! ${player.world.name}")
        val kkv = this.kv.get("using")
        val wdn = if(kkv == "a") "world_a" else "world_b"
        if (player.world.name != wdn && player.world.name != "world_nether" && player.world.name != "world_the_end") {
            // process teleport
            val wd = Bukkit.getWorld(wdn)!!
            val hiBlock = wd.getHighestBlockYAt(player.x.toInt(), player.z.toInt())
            player.teleport(Location(wd, player.x, hiBlock + 1.0, player.z))
            Bukkit.getLogger().info("Teleport ${player.name} to <${player.x}, ${hiBlock + 1.0}, ${player.z}>")
        }
    }
}