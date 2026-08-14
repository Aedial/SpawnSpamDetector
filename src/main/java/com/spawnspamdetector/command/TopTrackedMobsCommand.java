package com.spawnspamdetector.command;

import javax.annotation.Nonnull;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;

import com.spawnspamdetector.tracking.SpawnTrackerManager;


/**
 * Client-only command for inspecting the most recently synchronized tracking
 * snapshot without contacting the server.
 */
public class TopTrackedMobsCommand extends CommandBase {

    static private final int DEFAULT_TOP_COUNT = 10;

    @Override
    @Nonnull
    public String getName() {
        return "spawnspamtop";
    }

    @Override
    @Nonnull
    public String getUsage(@Nonnull ICommandSender sender) {
        return "/spawnspamtop";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public void execute(@Nonnull MinecraftServer server, @Nonnull ICommandSender sender, @Nonnull String[] args) {
        EntityPlayerSP player = (EntityPlayerSP) sender.getCommandSenderEntity();
        SpawnTrackerManager.sendTopGlobalMobCounts(player, DEFAULT_TOP_COUNT);
    }
}
