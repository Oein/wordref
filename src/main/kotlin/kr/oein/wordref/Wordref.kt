package kr.oein.wordref

import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import org.bukkit.*
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class FileKV {
    private val dir:File;
    constructor(dir: File) {
        this.dir = dir
    }

    public fun get(key: String): String {
        // read file with name $key
        val file = File(this.dir, key)

        if(!file.exists()) return ""
        return file.readText(Charsets.UTF_8)
    }

    public fun set(key: String, value: String) {
        val file = File(this.dir, key)
        file.writeText(value, Charsets.UTF_8)
    }

    public fun remove(key: String) {
        val file = File(this.dir, key)
        if(file.exists())
            file.delete()
    }

    public fun exists(key: String): Boolean {
        val file = File(this.dir, key)
        return file.exists()
    }
}

class Wordref : JavaPlugin() {
    private lateinit var kv: FileKV
    fun ensureWorld(worldName: String) {
        server.logger.info("Ensure world $worldName")
        val wc = WorldCreator(worldName)
        wc.environment(World.Environment.NORMAL)
        wc.type(WorldType.NORMAL)
        wc.createWorld()
    }
    private fun runSequence() {
        val usingWorld = this.kv.get("using")
        val isA = usingWorld == "a"

        val newWorldName = if(isA) "world_b" else "world_a"
        val oldWorldName = if(isA) "world_a" else "world_b"

        server.logger.info("Teleport to new world: $newWorldName")
        var newWorld = Bukkit.getWorld(newWorldName)
        if(newWorld == null) {
            server.logger.info("Create World: $newWorldName")
            val wc = WorldCreator(newWorldName)
            wc.environment(World.Environment.NORMAL)
            wc.type(WorldType.NORMAL)
            newWorld = wc.createWorld()
        }

        val nrexWorld = Bukkit.getWorld(newWorldName)!!

        server.logger.info("Sending Players: ${server.onlinePlayers.size}")
        for(o in server.onlinePlayers) {
            val playerWorld = o.world
            if(playerWorld.name != oldWorldName) continue

            nrexWorld.loadChunk(o.x.toInt(), o.z.toInt())
            val hiBlock = nrexWorld.getHighestBlockAt(o.x.toInt(), o.z.toInt())

            o.teleport(Location(nrexWorld, o.x, hiBlock.y + 1.0, o.z))
            Bukkit.getLogger().info("Teleport ${o.name} to <${o.x}, ${hiBlock.y + 1.0}, ${o.z}>")
        }

        this.kv.set("using", if(isA) "b" else "a")

        // unload world
        server.logger.info("Unload old world: $oldWorldName")
        val origWorld = Bukkit.getWorld(oldWorldName)
        if(origWorld != null) {
            server.unloadWorld(origWorld, false)
            deleteWorld(origWorld.worldFolder)
        }

        // create world
        server.logger.info("Create new world to be used later: $oldWorldName")
        val wcc = WorldCreator(oldWorldName)
        wcc.environment(World.Environment.NORMAL)
        wcc.type(WorldType.NORMAL)
        wcc.createWorld()
    }
    private val sec = 60 * 10
    private var lastSecond = sec

    private fun prettierTime(sec: Int): String {
        val ss = sec % 60
        val min = ((sec - ss) / 60).toInt()
        return "${min.toInt()} : ${ss.toInt()}"
    }

    override fun onEnable() {
        // Plugin startup logic
        if(!this.dataFolder.exists()) this.dataFolder.mkdir()
        this.kv = FileKV(this.dataFolder)
        if(!kv.exists("using")) kv.set("using", "a")

        Bukkit.getPluginManager().registerEvents(EventListener(this.kv), this);

        ensureWorld("world_a")
        ensureWorld("world_b")

        val scheduler = server.scheduler;
        scheduler.scheduleSyncRepeatingTask(this, {
            lastSecond -= 1
            if(lastSecond == 0) {
                Bukkit.broadcastMessage("Changing World!")
                server.logger.info("Reloading world...")
                lastSecond = sec
                this.runSequence()
            }
            server.onlinePlayers.forEach { player ->
                player.sendActionBar(
                    Component.text("Left ${prettierTime(lastSecond)}")
                )
            }
            if (lastSecond == 60 || lastSecond <= 5 || lastSecond == 60 * 5) {
                Bukkit.broadcastMessage("Left ${prettierTime(lastSecond)} seconds to change world")
            }
        }, 0L, 20L);

        server.logger.info("Server folder at ${this.dataFolder.path}")
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }

    private fun deleteWorld(path: File): Boolean {
        if (path.exists()) {
            val files = path.listFiles()
            if (files != null) {
                for (i in files.indices) {
                    if (files[i].isDirectory) {
                        deleteWorld(files[i])
                    } else {
                        files[i].delete()
                    }
                }
            }
        }
        return (path.delete())
    }
}
