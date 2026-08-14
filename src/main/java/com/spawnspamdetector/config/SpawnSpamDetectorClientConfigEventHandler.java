package com.spawnspamdetector.config;

import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

import com.spawnspamdetector.Tags;
import com.spawnspamdetector.tracking.ClientTrackingSync;


@Mod.EventBusSubscriber(modid = Tags.MODID, value = Side.CLIENT)
public final class SpawnSpamDetectorClientConfigEventHandler {

    private SpawnSpamDetectorClientConfigEventHandler() {
    }

    @SubscribeEvent
    public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (!Tags.MODID.equals(event.getModID())) return;

        SpawnSpamDetectorConfig.syncConfig();
        ClientTrackingSync.pushSettingsToServer();
    }
}