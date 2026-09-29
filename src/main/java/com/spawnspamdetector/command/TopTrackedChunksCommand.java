package com.spawnspamdetector.command;

import javax.annotation.Nonnull;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;

import com.spawnspamdetector.tracking.ClientTrackingSync;


/**
 * Client-only command that requests a current per-chunk count from the server.
 */
public class TopTrackedChunksCommand extends CommandBase {

    private static final int DEFAULT_LIMIT = 10;

    @Override
    @Nonnull
    public String getName() {
        return "spawnspamtopchunks";
    }

    @Override
    @Nonnull
    public String getUsage(@Nonnull ICommandSender sender) {
        return "/spawnspamtopchunks";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public void execute(@Nonnull MinecraftServer server, @Nonnull ICommandSender sender, @Nonnull String[] args) {
        ClientTrackingSync.requestTopTrackedChunks(DEFAULT_LIMIT);
    }
}
