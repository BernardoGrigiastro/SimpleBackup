package org.spoorn.simplebackup;

import lombok.extern.log4j.Log4j2;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.WorldSavePath;
import org.spoorn.simplebackup.config.ModConfig;
import org.spoorn.simplebackup.mixin.MinecraftServerAccessor;
import org.spoorn.simplebackup.util.SimpleBackupUtil;

import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Log4j2
public class SimpleBackup implements ModInitializer {
    
    public static final String MODID = "simplebackup";
    public static final String BACKUPS_FOLDER = "backup";
    private static final ScheduledExecutorService EXECUTOR_SERVICE = Executors.newSingleThreadScheduledExecutor();
    
    @Override
    public void onInitialize() {
        log.info("Hello from SimpleBackup!");
        
        // Config
        ModConfig.init();

        Path root = FabricLoader.getInstance().getGameDir();

        // Create worlds backup folder
        Path backupsPath = root.resolve(BACKUPS_FOLDER);
        SimpleBackupUtil.createDirectoryFailSafe(backupsPath);
        log.info("Worlds backup folder: {}", backupsPath);

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            MinecraftServerAccessor accessor = (MinecraftServerAccessor) server;
            String worldFolderName = accessor.getSession().getDirectoryName();
            Path worldSavePath = accessor.getSession().getDirectory(WorldSavePath.ROOT).getParent();
            
            SimpleBackupTask simpleBackupTask = new SimpleBackupTask(root, worldFolderName, worldSavePath);
            int backupIntervals = ModConfig.get().backupIntervalInSeconds;
            log.info("Scheduling a backup every {} seconds...", backupIntervals);
            ScheduledFuture<?> future = EXECUTOR_SERVICE.scheduleAtFixedRate(simpleBackupTask, backupIntervals, backupIntervals, TimeUnit.SECONDS);
        });
    }
}
