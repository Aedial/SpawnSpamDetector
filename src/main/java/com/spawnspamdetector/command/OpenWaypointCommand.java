package com.spawnspamdetector.command;

import javax.annotation.Nonnull;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;

import com.spawnspamdetector.integration.journeymap.JourneyMapIntegration;


/**
 * Client-only command used by the clickable coordinates in the top-chunk report.
 * Required because chat clicks can only trigger commands, not direct code.
 */
public class OpenWaypointCommand extends CommandBase {

    private static final int CHUNK_CENTER_OFFSET = 8;
    private static final int WAYPOINT_COLOR = 0x55FF55;

    @Override
    @Nonnull
    public String getName() {
        return "spawnspamwaypoint";
    }

    @Override
    @Nonnull
    public String getUsage(@Nonnull ICommandSender sender) {
        return "/spawnspamwaypoint <dimension> <chunkX> <chunkZ>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public void execute(@Nonnull MinecraftServer server, @Nonnull ICommandSender sender, @Nonnull String[] args)
            throws CommandException {
        if (args.length != 3) throw new WrongUsageException(getUsage(sender));

        int dimensionId = parseInt(args[0]);
        int chunkX = parseInt(args[1]);
        int chunkZ = parseInt(args[2]);
        BlockPos chunkCenter = new BlockPos(
            (chunkX << 4) + CHUNK_CENTER_OFFSET,
            64,
            (chunkZ << 4) + CHUNK_CENTER_OFFSET
        );

        JourneyMapIntegration.openWaypointEditor(
            "[" + chunkX + ", " + chunkZ + "]",
            chunkCenter,
            dimensionId,
            WAYPOINT_COLOR,
            true
        );
    }
}
