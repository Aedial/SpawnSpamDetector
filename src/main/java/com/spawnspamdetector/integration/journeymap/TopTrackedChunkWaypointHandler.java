package com.spawnspamdetector.integration.journeymap;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import com.spawnspamdetector.tracking.ServerTrackingManager;


/**
 * Adds the JourneyMap waypoint action to top-chunk chat coordinates.
 */
public class TopTrackedChunkWaypointHandler {

    @SubscribeEvent
    public void onClientChatReceived(ClientChatReceivedEvent event) {
        if (!JourneyMapIntegration.isJourneyMapAvailable()) return;

        TextComponentTranslation location = getTopChunkLocation(event.getMessage());
        if (location == null) return;

        Object[] locationArgs = location.getFormatArgs();
        if (locationArgs.length < 2 || !(locationArgs[1] instanceof ITextComponent)) return;

        Integer dimensionId = getDimensionId(locationArgs[0]);
        int[] chunkCoordinates = getChunkCoordinates((ITextComponent) locationArgs[1]);
        if (dimensionId == null || chunkCoordinates == null) return;

        ITextComponent coordinates = (ITextComponent) locationArgs[1];
        coordinates.getStyle().setClickEvent(new ClickEvent(
            ClickEvent.Action.RUN_COMMAND,
            "/spawnspamwaypoint " + dimensionId + " " + chunkCoordinates[0] + " " + chunkCoordinates[1]
        ));
        coordinates.getStyle().setHoverEvent(new HoverEvent(
            HoverEvent.Action.SHOW_TEXT,
            new TextComponentTranslation("spawnspamdetector.command.topchunks.openWaypoint")
        ));
    }

    private static TextComponentTranslation getTopChunkLocation(ITextComponent message) {
        if (!(message instanceof TextComponentTranslation)) return null;

        TextComponentTranslation entry = (TextComponentTranslation) message;
        if (!ServerTrackingManager.TOP_CHUNKS_ENTRY_KEY.equals(entry.getKey())) return null;

        Object[] entryArgs = entry.getFormatArgs();
        if (entryArgs.length < 2 || !(entryArgs[1] instanceof TextComponentTranslation)) return null;

        TextComponentTranslation location = (TextComponentTranslation) entryArgs[1];
        return ServerTrackingManager.TOP_CHUNKS_LOCATION_KEY.equals(location.getKey()) ? location : null;
    }

    private static Integer getDimensionId(Object dimension) {
        if (!(dimension instanceof TextComponentTranslation)) return null;

        Object[] dimensionArgs = ((TextComponentTranslation) dimension).getFormatArgs();
        if (dimensionArgs.length == 0) return null;

        Object dimensionId = dimensionArgs[dimensionArgs.length - 1];
        try {
            return Integer.valueOf(String.valueOf(dimensionId));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static int[] getChunkCoordinates(ITextComponent coordinates) {
        String text = coordinates.getUnformattedComponentText();
        if (!text.startsWith("[") || !text.endsWith("]")) return null;

        int separator = text.indexOf(", ");
        if (separator < 2 || separator + 2 >= text.length() - 1) return null;

        try {
            return new int[] {
                Integer.parseInt(text.substring(1, separator)),
                Integer.parseInt(text.substring(separator + 2, text.length() - 1))
            };
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
