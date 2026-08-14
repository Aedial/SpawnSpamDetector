package com.spawnspamdetector;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppedEvent;

import com.spawnspamdetector.proxy.CommonProxy;
import com.spawnspamdetector.tracking.ServerTrackingManager;


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

    @SidedProxy(
        clientSide = "com.spawnspamdetector.proxy.ClientProxy",
        serverSide = "com.spawnspamdetector.proxy.ServerProxy"
    )
    public static CommonProxy proxy;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }

    @EventHandler
    public void onServerStopped(FMLServerStoppedEvent event) {
        ServerTrackingManager.resetServerState();
    }
}
