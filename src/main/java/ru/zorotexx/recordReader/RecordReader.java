package ru.zorotexx.recordReader;

import org.bukkit.plugin.java.JavaPlugin;
import ru.zorotexx.recordReader.commands.PlayrecordCommand;

public final class RecordReader extends JavaPlugin {

    @Override
    public void onEnable() {
        getCommand("playrecord").setExecutor(new PlayrecordCommand());

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
