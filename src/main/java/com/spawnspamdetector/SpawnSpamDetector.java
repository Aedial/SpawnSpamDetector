package com.spawnspamdetector;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppedEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.spawnspamdetector.integration.journeymap.JourneyMapIntegration;
import com.spawnspamdetector.integration.journeymap.TopTrackedChunkWaypointHandler;
import com.spawnspamdetector.command.TopTrackedChunksCommand;
import com.spawnspamdetector.command.TopTrackedMobsCommand;
import com.spawnspamdetector.command.OpenWaypointCommand;
import com.spawnspamdetector.config.SpawnSpamDetectorConfig;
import com.spawnspamdetector.network.SpawnSpamDetectorNetwork;
import com.spawnspamdetector.tracking.ServerTrackingEventHandler;
import com.spawnspamdetector.tracking.ServerTrackingManager;
import com.spawnspamdetector.tracking.SpawnEventHandler;


@Mod(
    modid = Tags.MODID,
    name = Tags.MODNAME,
    version = Tags.VERSION,
    acceptedMinecraftVersions = "[1.12.2]",
    guiFactory = "com.spawnspamdetector.config.SpawnSpamDetectorGuiFactory"
)
public class SpawnSpamDetector {

    public static final Logger LOGGER = LogManager.getLogger(Tags.MODID);

    @Mod.Instance(Tags.MODID)
    public static SpawnSpamDetector instance;

    @SideOnly(Side.CLIENT)
    private void preInitClient(FMLPreInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new SpawnEventHandler());
        ClientCommandHandler.instance.registerCommand(new TopTrackedMobsCommand());
        ClientCommandHandler.instance.registerCommand(new TopTrackedChunksCommand());

        if (JourneyMapIntegration.isJourneyMapAvailable()) {
            MinecraftForge.EVENT_BUS.register(new TopTrackedChunkWaypointHandler());
            ClientCommandHandler.instance.registerCommand(new OpenWaypointCommand());
        }
    }

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        // Common side
        SpawnSpamDetectorNetwork.init();
        MinecraftForge.EVENT_BUS.register(new ServerTrackingEventHandler());

		File configDir = event.getModConfigurationDirectory();
		SpawnSpamDetectorConfig.init(new File(configDir, Tags.MODID + ".cfg"));

        // Client side
        if (event.getSide() == Side.CLIENT) preInitClient(event);
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {
    }

    @EventHandler
    public void onServerStopped(FMLServerStoppedEvent event) {
        ServerTrackingManager.resetServerState();
    }
}
