package com.spawnspamdetector.proxy;

import java.io.File;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import com.spawnspamdetector.Tags;
import com.spawnspamdetector.command.TopTrackedMobsCommand;
import com.spawnspamdetector.config.SpawnSpamDetectorConfig;
import com.spawnspamdetector.tracking.SpawnEventHandler;


public class ClientProxy extends CommonProxy {

	@Override
	public void preInit(FMLPreInitializationEvent event) {
		super.preInit(event);

		File configDir = event.getModConfigurationDirectory();
		SpawnSpamDetectorConfig.init(new File(configDir, Tags.MODID + ".cfg"));

        MinecraftForge.EVENT_BUS.register(new SpawnEventHandler());
        ClientCommandHandler.instance.registerCommand(new TopTrackedMobsCommand());
    }
}
