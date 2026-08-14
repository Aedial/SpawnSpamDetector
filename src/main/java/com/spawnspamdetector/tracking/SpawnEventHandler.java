package com.spawnspamdetector.tracking;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

import com.spawnspamdetector.SpawnSpamDetector;


public class SpawnEventHandler {

    @SubscribeEvent
    public void onClientConnect(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        SpawnSpamDetector.LOGGER.info("Client connected to server, sending settings");

        ClientTrackingSync.pushSettingsToServer();
    }

    @SubscribeEvent
    public void onClientPlayerJoinWorld(EntityJoinWorldEvent event) {
        if (!event.getWorld().isRemote) return;
        if (!(event.getEntity() instanceof EntityPlayerSP)) return;

        SpawnSpamDetector.LOGGER.info("Client player joined world, resending settings to server");

        // The connection event can run before the client player is fully attached.
        // Resending here ensures integrated-server worlds always subscribe.
        ClientTrackingSync.pushSettingsToServer();
    }

    @SubscribeEvent
    public void onClientDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        SpawnTrackerManager.clearAll();
    }
}