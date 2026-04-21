package ru.zorotexx.recordReader.commands;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import ru.zorotexx.recordReader.RecordReader;
import ru.zorotexx.recordReader.reader.ParticleRecordReader;

import java.io.File;
import java.util.ArrayList;

public class PlayrecordCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (strings.length < 1) {
            commandSender.sendMessage(ChatColor.RED + "Usage: /playrecord <название> <каждый тик?>");
            return false;
        }

        Player player = (Player) commandSender;
        Location location = player.getLocation();

        String name = strings[0];
        boolean everytick = Boolean.parseBoolean(strings[1]);

        ParticleRecordReader.ParticleSpawner spawner = ParticleRecordReader.read(new File(RecordReader.getPlugin(RecordReader.class).getDataFolder() + "/" + name + ".txt"))
                .createSpawner(location.getX(), location.getY(), location.getZ());

        if (everytick) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    spawner.run(new ArrayList<>(Bukkit.getOnlinePlayers()));
                }
            }.runTaskTimer(RecordReader.getPlugin(RecordReader.class), 0, 1);
        } else {
            spawner.run(new ArrayList<>(Bukkit.getOnlinePlayers()));
        }

        return false;
    }
}
