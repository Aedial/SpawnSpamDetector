package com.spawnspamdetector.tracking;

import net.minecraft.client.Minecraft;

import com.spawnspamdetector.config.SpawnSpamDetectorConfig;
import com.spawnspamdetector.network.PacketClientTrackingSettings;
import com.spawnspamdetector.network.SpawnSpamDetectorNetwork;


public final class ClientTrackingSync {

    private ClientTrackingSync() {
    }

    public static void pushSettingsToServer() {
        SpawnTrackerManager.clearAll();

        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.getConnection() == null) return;

        SpawnSpamDetectorNetwork.INSTANCE.sendToServer(new PacketClientTrackingSettings(SpawnSpamDetectorConfig.isDetectionEnabled()));
    }
}