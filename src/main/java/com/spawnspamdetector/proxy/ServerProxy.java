package com.spawnspamdetector.proxy;

import java.io.File;

import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import com.spawnspamdetector.Tags;
import com.spawnspamdetector.config.SpawnSpamDetectorConfig;


public class ServerProxy extends CommonProxy {

	@Override
	public void preInit(FMLPreInitializationEvent event) {
		super.preInit(event);

		File configDir = event.getModConfigurationDirectory();
		SpawnSpamDetectorConfig.init(new File(configDir, Tags.MODID + ".cfg"));
	}
}