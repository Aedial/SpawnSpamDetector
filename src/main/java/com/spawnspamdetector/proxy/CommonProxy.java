package com.spawnspamdetector.proxy;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import com.spawnspamdetector.network.SpawnSpamDetectorNetwork;
import com.spawnspamdetector.tracking.ServerTrackingEventHandler;


public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        SpawnSpamDetectorNetwork.init();

        MinecraftForge.EVENT_BUS.register(new ServerTrackingEventHandler());
    }

    public void init(FMLInitializationEvent event) {
    }

    public void postInit(FMLPostInitializationEvent event) {
    }
}
